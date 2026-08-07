package teamport.creatures.core;

import net.minecraft.client.Minecraft;
import net.minecraft.client.render.EntityRendererDispatcher;
import net.minecraft.client.render.texturepack.TexturePack;
import net.minecraft.client.render.texturepack.TexturePackList;
import net.minecraft.core.data.registry.Registries;
import org.useless.dragonfly.data.entity.mojang.EntityGeometryMojangData;
import teamport.creatures.MoreMobs;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/**
 * Bridges entity art out of a copy of the original Mo' Creatures that the <em>player</em> supplies.
 * <p>
 * This mod ships no art from the original. Mo' Creatures is DrZhark's work and its licence does not
 * permit redistribution, so the textures cannot live in this repository. Instead, if the player drops
 * their own copy of the original mod into the game directory, this reads the images out of it and
 * writes a generated texture pack that BTA then loads like any other pack. Nothing is downloaded and
 * nothing is redistributed — the file has to already be on the player's disk.
 * <p>
 * {@link MMGeometryBridge} rides along on the same archive walk and converts the original's
 * <em>models</em> the same way, because the original textures are painted against the original box
 * layout and only look right on it.
 * <p>
 * Mapping is driven by {@code /assets/creatures/asset-bridge.properties} rather than hardcoded, so
 * adding a mob is a one-line manifest edit. Original archives differ in internal layout between
 * versions, so entries are matched on the file's <em>base name</em> and the directory structure
 * inside the archive is ignored — including the zip-inside-a-zip that the original's own installer
 * download ships, whose class and image files all live one level down in {@code mods/}.
 * <p>
 * <b>Where the copy has to be: anywhere under the game directory.</b> Nothing about the search
 * depends on the file being called the right thing or sitting in the right folder, because after
 * fifteen years of being passed around the original turns up in every shape there is — as
 * {@code DrZharks_MoCreatures_Mod_v2.12.2_1.zip}, as the {@code MoCreatures.zip} nested inside that,
 * as a {@code .jar}, renamed to whatever, or unpacked into a folder. So:
 * <ul>
 *   <li>The whole game directory is walked, not just {@code mods/}, to {@value #MAX_SCAN_DEPTH}
 *       levels, skipping only places nothing could sensibly be ({@code saves/}, {@code logs/} and
 *       friends, plus the pack this bridge writes).</li>
 *   <li>A container is recognised by its zip header, not its extension, so {@code .zip},
 *       {@code .jar}, {@code .disabled} and no extension at all are the same thing.</li>
 *   <li>An <em>unpacked</em> copy works too: loose {@code .png} and {@code .class} files lying in a
 *       folder are picked up individually, so a folder and an archive are equally good.</li>
 *   <li>Nesting is followed either way, to {@value #MAX_NESTING} levels of archive-inside-archive.</li>
 * </ul>
 * The name is not ignored, but it only decides <em>order</em>: see {@link #findSources}. Several
 * sources can contribute, first-ranked wins per file, so a partial copy is topped up rather than
 * rejected.
 */
public final class MMAssetBridge {
	private MMAssetBridge() {}

	public static final String PACK_NAME = "MoCreaturesAssets";
	private static final String MANIFEST = "/assets/creatures/asset-bridge.properties";
	private static final String STAMP = "bridge-source.txt";

	/**
	 * Bumped whenever a manifest or the converter changes shape, so an existing pack is rebuilt even
	 * though the player's archive has not moved.
	 */
	private static final int BRIDGE_REVISION = 14;

	/** How deep to follow zips inside zips. The original's download nests exactly one level. */
	private static final int MAX_NESTING = 3;

	/** Set once the bridge has run, so the audit can report on it. */
	public static int bridgedCount = -1;
	public static int missingCount = -1;
	public static String sourceArchive = null;
	public static boolean packAutoEnabled = false;
	public static boolean usedCache = false;

	public static void run() {
		File gameDir;
		try {
			gameDir = Minecraft.getMinecraft().getMinecraftDir();
		} catch (Throwable t) {
			// Dedicated server, or called before the client exists. Nothing to bridge.
			return;
		}
		if (gameDir == null) return;

		Map<String, List<String>> manifest = readManifest();
		if (manifest.isEmpty()) {
			MoreMobs.LOGGER.warn("Asset bridge: manifest {} is empty or missing, skipping", MANIFEST);
			return;
		}

		File packDir = new File(gameDir, "texturepacks/" + PACK_NAME);

		// Asked before anything is searched for. The stamp names the files the pack was built from, so
		// a warm launch costs a handful of stat calls and no directory walk at all.
		Stamp previous = Stamp.read(packDir);
		if (previous != null && previous.stillValid(gameDir)) {
			usedCache = true;
			sourceArchive = previous.label;
			MoreMobs.LOGGER.info("Asset bridge: '{}' is already built from {}, skipping extraction",
				PACK_NAME, previous.label);
			enablePack(packDir);
			return;
		}

		// One walk of each source serves both halves, so a nested zip is only unpacked once.
		Set<String> wanted = new LinkedHashSet<>(manifest.keySet());
		wanted.addAll(MMGeometryBridge.wantedEntries());

		List<Source> sources = findSources(gameDir, packDir, wanted);
		if (sources.isEmpty()) {
			reportNothingFound(gameDir, manifest.size());
			return;
		}

		List<Source> used = new ArrayList<>();
		Map<String, byte[]> entries = collect(sources, wanted, used);
		if (entries.isEmpty()) {
			reportNothingFound(gameDir, manifest.size());
			return;
		}
		sourceArchive = describe(gameDir, used);

		List<String> missing = new ArrayList<>();
		int written = 0;
		try {
			for (Map.Entry<String, List<String>> mapping : manifest.entrySet()) {
				byte[] bytes = entries.get(mapping.getKey());
				if (bytes == null) {
					missing.add(mapping.getKey());
					continue;
				}
				// One original can serve several paths, where this port splits a mob into states the
				// original drew with a single skin.
				for (String path : mapping.getValue()) {
					write(new File(packDir, path), bytes);
					written++;
				}
			}
			writePackMeta(packDir);
		} catch (IOException e) {
			MoreMobs.LOGGER.warn("Asset bridge: could not write into '{}': {}", packDir.getPath(), e.toString());
			return;
		}

		bridgedCount = written;
		missingCount = missing.size();

		MoreMobs.LOGGER.info("Asset bridge: {} textures bridged from {} into texture pack '{}'",
			written, sourceArchive, PACK_NAME);
		if (used.size() > 1) {
			MoreMobs.LOGGER.info("Asset bridge: sources used, best first: {}", paths(gameDir, used));
		}
		if (!missing.isEmpty()) {
			MoreMobs.LOGGER.warn("Asset bridge: {} textures not found in the archive: {}",
				missing.size(), String.join(", ", missing));
		}

		runGeometryBridge(entries, packDir);
		new Stamp(sourceArchive, used, gameDir).write(packDir);
		enablePack(packDir);
	}

	private static void reportNothingFound(File gameDir, int manifestSize) {
		bridgedCount = 0;
		missingCount = manifestSize;
		MoreMobs.LOGGER.info("Asset bridge: no Mo' Creatures files found anywhere under '{}' — mobs will use "
			+ "built-in textures where they exist. Drop the original mod in (zip, jar or unpacked folder, "
			+ "any name, any depth) to restore the original look.", gameDir.getPath());
	}

	private static void runGeometryBridge(Map<String, byte[]> entries, File packDir) {
		MMGeometryBridge.Result result;
		try {
			result = MMGeometryBridge.run(entries, packDir);
		} catch (Throwable t) {
			MoreMobs.LOGGER.warn("Geometry bridge: failed, textures are unaffected: {}", t.toString());
			return;
		}
		int total = result.converted.size() + result.composed.size();
		if (total > 0) {
			MoreMobs.LOGGER.info("Geometry bridge: {} models converted ({})", total,
				String.join(", ", concat(result.converted, result.composed)));
		}
		if (!result.composed.isEmpty()) {
			MoreMobs.LOGGER.info("Geometry bridge: {} of those extend one of Minecraft's own model classes and "
				+ "were completed with vanilla geometry: {}", result.composed.size(),
				String.join(", ", result.composed));
		}
		for (String problem : result.problems) {
			MoreMobs.LOGGER.warn("Geometry bridge: {}", problem);
		}
	}

	private static List<String> concat(List<String> first, List<String> second) {
		List<String> all = new ArrayList<>(first);
		all.addAll(second);
		return all;
	}

	// ----------------------------------------------------------------------------------------------
	// Finding the original
	// ----------------------------------------------------------------------------------------------

	/**
	 * One place the original's files might be: either a container to look inside, or a single loose
	 * file that is already one of the things being looked for.
	 */
	private static final class Source implements Comparable<Source> {
		final File file;
		/** True if the first bytes are a zip header, whatever the name says. */
		final boolean container;
		/** Base name, lower case. For a loose file this is the manifest key it satisfies. */
		final String name;
		final int rank;
		final int depth;

		Source(File file, boolean container, String name, int rank, int depth) {
			this.file = file;
			this.container = container;
			this.name = name;
			this.rank = rank;
			this.depth = depth;
		}

		/** Best first: by how much the path looks like the original, then shallowest, then by name. */
		@Override
		public int compareTo(Source other) {
			if (rank != other.rank) return Integer.compare(rank, other.rank);
			if (depth != other.depth) return Integer.compare(depth, other.depth);
			return file.getPath().compareToIgnoreCase(other.file.getPath());
		}
	}

	/** Named outright: {@code mocreatures-assets.<anything>}, the one filename this mod documents. */
	private static final int RANK_EXPLICIT = 0;
	/** Some part of the path says Mo' Creatures. */
	private static final int RANK_NAMED = 1;
	/** Somewhere under {@code mods/}, which is where the install notes say to put it. */
	private static final int RANK_MODS = 2;
	/** Found by looking, with nothing in the name to recommend it. */
	private static final int RANK_ANYWHERE = 3;

	/**
	 * How many directory levels below the game directory to look.
	 * {@code mods/pack/unpacked/MoCreatures.zip} is three, and doubling that leaves room to spare.
	 */
	private static final int MAX_SCAN_DEPTH = 6;
	/** Ceiling on files examined during the walk, so a pathological tree cannot stall startup. */
	private static final int MAX_SCAN_FILES = 20000;
	/**
	 * Ceiling on containers actually opened. Ranking means the likely ones are opened first, and this
	 * is set well above any real mods folder: it is a runaway guard, not a search budget. Setting it
	 * anywhere near the number of installed mods would let sixty unrelated jars use up the allowance
	 * before an unhelpfully named copy of the original further down the list ever got opened.
	 */
	private static final int MAX_CONTAINERS_READ = 400;

	/**
	 * Directory names never worth descending into: large, and none of them is anywhere a copy of the
	 * original would sensibly be. Everything else is fair game, {@code mods/} or not.
	 */
	private static final Set<String> SKIPPED_DIRS = new HashSet<>(Arrays.asList(
		"saves", "logs", "crash-reports", "screenshots", "stats", "assets", "libraries", "versions",
		"bin", "natives", "server-resource-packs", "backups", "shaderpacks", ".git", ".fabric", ".mixin.out"));

	/**
	 * Everywhere the original's files might be, best candidate first.
	 *
	 * <p>Deliberately not a filename match. The original has been passed around for fifteen years as
	 * {@code DrZharks_MoCreatures_Mod_v2.12.2_1.zip}, as {@code MoCreatures.zip} nested inside that, as
	 * a {@code .jar}, renamed to anything at all, and unpacked into a folder — so what identifies it
	 * here is <em>what it contains</em>, not what it is called. A zip is recognised by its header
	 * rather than its extension, a folder full of loose {@code .png} and {@code .class} files works as
	 * well as an archive, and nesting is followed either way.
	 *
	 * <p>The name still counts for something: it decides the <em>order</em> candidates are read in, so
	 * an obviously-named archive is opened before an unrelated mod jar that happens to contain a
	 * {@code bear.png}. First source to supply a given file wins.
	 */
	private static List<Source> findSources(File gameDir, File packDir, Set<String> wanted) {
		List<Source> sources = new ArrayList<>();
		String packPath = safePath(packDir);
		int[] budget = {MAX_SCAN_FILES};
		scan(gameDir, gameDir, packPath, wanted, sources, 0, budget);
		Collections.sort(sources);
		return sources;
	}

	private static void scan(File dir, File gameDir, String packPath, Set<String> wanted,
		List<Source> sources, int depth, int[] budget) {
		if (depth > MAX_SCAN_DEPTH || budget[0] <= 0) return;

		File[] children = dir.listFiles();
		if (children == null) return;

		for (File child : children) {
			if (budget[0] <= 0) return;
			budget[0]--;

			if (child.isDirectory()) {
				String name = child.getName().toLowerCase(Locale.ROOT);
				// Never walk into the pack this bridge writes: it is full of the very files being
				// looked for, under their new names, and rereading it would be a slow no-op at best.
				if (SKIPPED_DIRS.contains(name) || safePath(child).equals(packPath)) continue;
				scan(child, gameDir, packPath, wanted, sources, depth + 1, budget);
				continue;
			}
			if (!child.isFile()) continue;

			String name = baseName(child.getName());
			if (wanted.contains(name)) {
				sources.add(new Source(child, false, name, rank(gameDir, child), depth));
			} else if (!isOwnJar(name) && isZip(child)) {
				sources.add(new Source(child, true, name, rank(gameDir, child), depth));
			}
		}
	}

	/**
	 * How much the path itself suggests this is the original, which decides read order only.
	 *
	 * <p>Judged on the whole path below the game directory, not just the file name, so an unpacked
	 * {@code mods/MoCreatures/mob/bear.png} ranks as well as the zip it came out of.
	 */
	private static int rank(File gameDir, File file) {
		String relative = relativePath(gameDir, file).toLowerCase(Locale.ROOT);
		if (baseName(file.getName()).startsWith("mocreatures-assets")) return RANK_EXPLICIT;
		if (relative.contains("mocreature") || relative.contains("mo_creature")
			|| relative.contains("mo-creature") || relative.contains("mo creature")
			|| relative.contains("drzhark")) {
			return RANK_NAMED;
		}
		if (relative.startsWith("mods/")) return RANK_MODS;
		return RANK_ANYWHERE;
	}

	/**
	 * This mod's own jar, which carries some of the same file names under {@code assets/creatures} and
	 * would otherwise be read back into the pack it just wrote.
	 */
	private static boolean isOwnJar(String name) {
		return name.contains("creatures") && !name.contains("mocreature")
			&& !name.contains("mo_creature") && !name.contains("mo-creature");
	}

	/**
	 * Whether a file opens with a zip header, which is the only reliable way to tell. Extensions do
	 * not survive the trip: the same payload turns up as {@code .zip}, {@code .jar}, {@code .mod},
	 * {@code .disabled} and with no extension at all.
	 */
	private static boolean isZip(File file) {
		if (file.length() < 4) return false;
		try (InputStream in = new FileInputStream(file)) {
			byte[] header = new byte[4];
			int read = 0;
			while (read < 4) {
				int step = in.read(header, read, 4 - read);
				if (step < 0) return false;
				read += step;
			}
			return isZipHeader(header, 4);
		} catch (IOException e) {
			return false;
		}
	}

	/** {@code PK} followed by a local-file, central-directory or spanning marker. */
	private static boolean isZipHeader(byte[] header, int length) {
		if (length < 4 || header[0] != 'P' || header[1] != 'K') return false;
		int third = header[2] & 0xFF;
		int fourth = header[3] & 0xFF;
		return (third == 0x03 && fourth == 0x04)
			|| (third == 0x05 && fourth == 0x06)
			|| (third == 0x07 && fourth == 0x08);
	}

	// ----------------------------------------------------------------------------------------------
	// Reading the sources
	// ----------------------------------------------------------------------------------------------

	/**
	 * Flattens every source down to the entries the manifests asked for, keyed by lower-case base
	 * name, best-ranked source first. Directory structure is ignored throughout — it differs between
	 * versions of the original and is not worth depending on.
	 *
	 * @param used filled in with the sources that actually supplied something, in the order read
	 */
	private static Map<String, byte[]> collect(List<Source> sources, Set<String> wanted, List<Source> used) {
		Map<String, byte[]> found = new HashMap<>();
		int containersRead = 0;

		for (Source source : sources) {
			if (found.size() >= wanted.size()) break;

			int before = found.size();
			if (source.container) {
				if (containersRead >= MAX_CONTAINERS_READ) continue;
				containersRead++;
				try {
					collectFromZipFile(source.file, wanted, found);
				} catch (IOException e) {
					// A jar that cannot be read is almost always somebody else's and not interesting.
					MoreMobs.LOGGER.debug("Asset bridge: skipped '{}': {}", source.file.getPath(), e.toString());
					continue;
				}
			} else if (!found.containsKey(source.name)) {
				try {
					found.put(source.name, Files.readAllBytes(source.file.toPath()));
				} catch (IOException e) {
					MoreMobs.LOGGER.debug("Asset bridge: skipped '{}': {}", source.file.getPath(), e.toString());
					continue;
				}
			}

			if (found.size() > before) used.add(source);
		}
		return found;
	}

	/**
	 * Reads one archive. Uses {@link ZipFile} rather than a stream so the entry list comes off the
	 * central directory: names can be checked without decompressing anything, which is what makes it
	 * affordable to look inside unrelated mod jars at all.
	 */
	private static void collectFromZipFile(File file, Set<String> wanted, Map<String, byte[]> found)
		throws IOException {
		try (ZipFile zip = new ZipFile(file)) {
			List<ZipEntry> nested = new ArrayList<>();

			for (Enumeration<? extends ZipEntry> e = zip.entries(); e.hasMoreElements(); ) {
				ZipEntry entry = e.nextElement();
				if (entry.isDirectory()) continue;
				String name = baseName(entry.getName());
				if (wanted.contains(name)) {
					if (!found.containsKey(name)) {
						try (InputStream in = zip.getInputStream(entry)) {
							found.put(name, MMGeometryBridge.readFully(in));
						}
					}
				} else if (isNestedArchiveName(name) && !isOwnJar(name)) {
					nested.add(entry);
				}
			}

			// The original's own download is a zip whose payload is mods/MoCreatures.zip, so reading
			// only the outer layer finds nothing at all. Descending is left until the flat pass is done
			// so that a hit at this level is preferred, and skipped entirely once nothing is missing.
			for (ZipEntry entry : nested) {
				if (found.size() >= wanted.size()) break;
				try (InputStream in = zip.getInputStream(entry)) {
					collectFromStream(in, wanted, found, 1);
				} catch (IOException ignored) {
					// A corrupt member inside an otherwise readable archive: keep the rest.
				}
			}
		}
	}

	/** Reads an archive that is itself inside an archive, which has to be streamed rather than indexed. */
	private static void collectFromStream(InputStream raw, Set<String> wanted, Map<String, byte[]> found, int depth)
		throws IOException {
		if (depth > MAX_NESTING) return;

		ZipInputStream zip = new ZipInputStream(new BufferedInputStream(raw));
		ZipEntry entry;
		while ((entry = zip.getNextEntry()) != null) {
			if (entry.isDirectory()) continue;
			if (found.size() >= wanted.size()) return;

			String name = baseName(entry.getName());
			if (wanted.contains(name)) {
				if (!found.containsKey(name)) found.put(name, MMGeometryBridge.readFully(zip));
			} else if (isNestedArchiveName(name) && !isOwnJar(name)) {
				collectFromStream(zip, wanted, found, depth + 1);
			}
		}
	}

	/**
	 * Whether an entry <em>inside</em> an archive is worth opening as an archive itself.
	 *
	 * <p>Named-based, unlike {@link #isZip} on disk, and deliberately so. A file the player handles
	 * gets renamed — that is the whole reason the on-disk test reads the header instead of the
	 * extension — but an entry sealed inside an archive keeps the name whoever built the archive gave
	 * it, and the case that matters, {@code mods/MoCreatures.zip} inside the original's own download,
	 * is properly named. Sniffing headers here instead would mean decompressing the first bytes of
	 * every entry of every jar in {@code mods/}, which measured at three seconds of startup for a
	 * sixty-mod install and found nothing that this does not.
	 */
	private static boolean isNestedArchiveName(String name) {
		return name.endsWith(".zip") || name.endsWith(".jar") || name.endsWith(".mod")
			|| name.endsWith(".pk3") || name.endsWith(".litemod") || name.endsWith(".disabled");
	}

	// ----------------------------------------------------------------------------------------------
	// Cache
	// ----------------------------------------------------------------------------------------------

	/**
	 * What the pack was last built from, so a second launch does not pay for the extraction again.
	 *
	 * <p>Records every file that actually contributed, by path, length and modification time. Written
	 * that way rather than as a fingerprint of everything that was <em>searched</em> so that updating
	 * an unrelated mod does not invalidate the pack — with the search now covering the whole game
	 * directory, that would mean rebuilding on almost every launch.
	 *
	 * <p>The flip side is that adding a <em>better</em> copy of the original somewhere else goes
	 * unnoticed while the recorded one is still in place. Deleting the generated pack forces the
	 * search to run again, which is what the log says to do.
	 */
	private static final class Stamp {
		final String label;
		final List<String> lines = new ArrayList<>();

		Stamp(String label, List<Source> used, File gameDir) {
			this.label = label;
			for (Source source : used) lines.add(line(gameDir, source.file));
		}

		private Stamp(String label, List<String> lines) {
			this.label = label;
			this.lines.addAll(lines);
		}

		static String line(File gameDir, File file) {
			return relativePath(gameDir, file) + "|" + file.length() + "|" + file.lastModified();
		}

		static Stamp read(File packDir) {
			File file = new File(packDir, STAMP);
			if (!file.isFile()) return null;
			try {
				List<String> all = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
				if (all.size() < 3 || !all.get(0).trim().equals(String.valueOf(BRIDGE_REVISION))) return null;
				return new Stamp(all.get(1).trim(), all.subList(2, all.size()));
			} catch (IOException | RuntimeException e) {
				return null;
			}
		}

		/** True while every file the pack was built from is still there, unchanged. */
		boolean stillValid(File gameDir) {
			if (lines.isEmpty()) return false;
			for (String recorded : lines) {
				String trimmed = recorded.trim();
				if (trimmed.isEmpty()) continue;
				int split = trimmed.indexOf('|');
				if (split < 0) return false;
				File file = new File(gameDir, trimmed.substring(0, split));
				if (!file.isFile() || !line(gameDir, file).equals(trimmed)) return false;
			}
			return true;
		}

		void write(File packDir) {
			// Nothing to check against later means no valid stamp, so do not write a misleading one.
			if (lines.isEmpty()) return;
			StringBuilder out = new StringBuilder();
			out.append(BRIDGE_REVISION).append('\n').append(label).append('\n');
			for (String recorded : lines) out.append(recorded).append('\n');
			try {
				MMAssetBridge.write(new File(packDir, STAMP), out.toString().getBytes(StandardCharsets.UTF_8));
			} catch (IOException e) {
				MoreMobs.LOGGER.warn("Asset bridge: could not record what the pack was built from: {}", e.toString());
			}
		}
	}

	/**
	 * Short human-readable name for what was read, for the log and the startup audit.
	 *
	 * <p>An unpacked copy contributes one source per file — ninety-odd of them — so naming the first
	 * and counting the rest would report a folder full of the original as "'ModelBear1.class' and 96
	 * more". Where the sources share a folder, that folder is the honest answer.
	 */
	private static String describe(File gameDir, List<Source> used) {
		if (used.isEmpty()) return "nothing";
		if (used.size() == 1) return "'" + used.get(0).file.getName() + "'";

		File shared = commonAncestor(used);
		if (shared != null && !safePath(shared).equals(safePath(gameDir))) {
			return "'" + relativePath(gameDir, shared) + "' (" + used.size() + " files)";
		}
		return "'" + used.get(0).file.getName() + "' and " + (used.size() - 1) + " more";
	}

	/** Deepest directory every source sits under, or null if they share nothing at all. */
	private static File commonAncestor(List<Source> used) {
		File shared = null;
		for (Source source : used) {
			File parent = source.file.getParentFile();
			if (parent == null) return null;
			shared = shared == null ? parent : commonAncestor(shared, parent);
			if (shared == null) return null;
		}
		return shared;
	}

	/** Walks {@code a} up until it is {@code b} or contains it. */
	private static File commonAncestor(File a, File b) {
		String candidate = safePath(a);
		String target = safePath(b);
		while (!target.equals(candidate) && !target.startsWith(candidate + File.separator)) {
			File up = new File(candidate).getParentFile();
			if (up == null) return null;
			candidate = safePath(up);
		}
		return new File(candidate);
	}

	/** How many source paths to spell out before summarising; an unpacked copy has dozens. */
	private static final int MAX_PATHS_LOGGED = 8;

	private static String paths(File gameDir, List<Source> used) {
		List<String> shown = new ArrayList<>();
		for (Source source : used) {
			if (shown.size() == MAX_PATHS_LOGGED) {
				shown.add("and " + (used.size() - MAX_PATHS_LOGGED) + " more");
				break;
			}
			shown.add(relativePath(gameDir, source.file));
		}
		return String.join(", ", shown);
	}

	/** Path below the game directory, with forward slashes, so a stamp survives a moved install. */
	private static String relativePath(File root, File file) {
		String rootPath = safePath(root);
		String filePath = safePath(file);
		String relative = filePath.startsWith(rootPath + File.separator)
			? filePath.substring(rootPath.length() + 1)
			: filePath;
		return relative.replace(File.separatorChar, '/');
	}

	/** {@code getCanonicalPath} where it works, absolute where it does not — it throws on odd names. */
	private static String safePath(File file) {
		try {
			return file.getCanonicalPath();
		} catch (IOException e) {
			return file.getAbsolutePath();
		}
	}

	// ----------------------------------------------------------------------------------------------
	// Turning the pack on
	// ----------------------------------------------------------------------------------------------

	/**
	 * Selects the generated pack so the player has nothing to do in Options. BTA treats a directory
	 * holding a {@code pack.txt} as a texture pack, so it only has to be rescanned and selected.
	 * <p>
	 * Guarded rather than assumed: if any of this throws, the pack is still on disk and can be
	 * enabled by hand, which is what the log then says.
	 */
	private static void enablePack(File packDir) {
		if (!packDir.isDirectory()) return;
		try {
			TexturePackList packs = Minecraft.getMinecraft().texturePackList;
			packs.updateAvailableTexturePacks();
			for (TexturePack pack : packs.availableTexturePacks()) {
				if (!PACK_NAME.equals(pack.fileName)) continue;
				if (packs.selectedPacks.contains(pack)) {
					packAutoEnabled = true;
					reloadGeometry(packs);
					return;
				}
				packs.setTexturePack(pack);
				packs.refreshIfReady();
				packAutoEnabled = true;
				reloadGeometry(packs);
				MoreMobs.LOGGER.info("Asset bridge: texture pack '{}' enabled automatically", PACK_NAME);
				return;
			}
			MoreMobs.LOGGER.info("Asset bridge: texture pack '{}' was written but BTA did not list it; "
				+ "enable it in Options to use it", PACK_NAME);
		} catch (Throwable t) {
			MoreMobs.LOGGER.warn("Asset bridge: could not enable texture pack '{}' automatically ({}); "
				+ "enable it in Options to use it", PACK_NAME, t.toString());
		}
	}

	/**
	 * Re-reads entity geometry after the generated pack is enabled.
	 * <p>
	 * Without this the bridge writes 31 correct models and none of them appear. Textures survive the
	 * ordering because {@code TextureManager} resolves them lazily at bind time, but geometry does not:
	 * BTA fills its model cache from the selected packs once, in {@code Minecraft.startGame()}, which
	 * has already run by the time this bridge gets to write anything. Worse, an
	 * {@link net.minecraft.client.render.entity.EntityRenderer} resolves its model in its
	 * <em>constructor</em>, so refreshing the cache alone would still leave every renderer holding the
	 * model it captured at startup.
	 * <p>
	 * So both halves have to be redone, in order: reload the cache from the pack list, then rebuild the
	 * renderers so they pick the new models up. The dispatcher's reload re-emits renderer registration,
	 * which is what puts this mod's own renderers back.
	 */
	private static void reloadGeometry(TexturePackList packs) {
		try {
			// Dragonfly only looks for /assets/<namespace>/models/entity/models.json for namespaces in
			// this registry, so an unregistered namespace means the manifest is never even opened and
			// every model silently falls back. Registering is idempotent enough to do defensively.
			boolean known = Registries.NAMESPACES.getItem(MoreMobs.MOD_ID) != null;
			MoreMobs.LOGGER.info("Asset bridge: namespace '{}' registered with Dragonfly: {}",
				MoreMobs.MOD_ID, known);
			if (!known) {
				Registries.NAMESPACES.register(MoreMobs.MOD_ID, MoreMobs.MOD_ID);
				MoreMobs.LOGGER.info("Asset bridge: registered namespace '{}' so its models can load",
					MoreMobs.MOD_ID);
			}

			EntityGeometryMojangData.Cache.reload(packs);
			EntityRendererDispatcher.instance.reload();
			MoreMobs.LOGGER.info("Asset bridge: entity geometry reloaded from '{}'", PACK_NAME);
		} catch (Throwable t) {
			MoreMobs.LOGGER.warn("Asset bridge: bridged models were written but could not be reloaded ({}); "
				+ "restart the game to see them", t.toString());
		}
	}

	// ----------------------------------------------------------------------------------------------
	// Plumbing
	// ----------------------------------------------------------------------------------------------

	private static void write(File target, byte[] bytes) throws IOException {
		File parent = target.getParentFile();
		if (!parent.isDirectory() && !parent.mkdirs()) throw new IOException("could not create " + parent);
		try (OutputStream out = new FileOutputStream(target)) {
			out.write(bytes);
		}
	}

	private static void writePackMeta(File packDir) throws IOException {
		File info = new File(packDir, "pack.txt");
		if (info.isFile()) return;
		write(info, ("Generated by Mo' Creatures for BTA from a locally supplied copy of the original mod.\n"
			+ "Not redistributable. Delete this folder to remove it.\n").getBytes(StandardCharsets.UTF_8));
	}

	private static String baseName(String path) {
		int slash = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
		return path.substring(slash + 1).toLowerCase(Locale.ROOT);
	}

	/**
	 * @return archive base name -> every path in the pack it supplies. The value side is a
	 *         comma-separated list so one original can serve several paths; see the manifest header.
	 */
	private static Map<String, List<String>> readManifest() {
		Map<String, List<String>> map = new HashMap<>();
		try (InputStream in = MMAssetBridge.class.getResourceAsStream(MANIFEST)) {
			if (in == null) return map;
			Properties props = new Properties();
			props.load(in);
			for (String key : props.stringPropertyNames()) {
				List<String> paths = new ArrayList<>();
				for (String path : props.getProperty(key).split(",")) {
					String trimmed = path.trim();
					if (!trimmed.isEmpty()) paths.add(trimmed);
				}
				if (!paths.isEmpty()) map.put(key.toLowerCase(Locale.ROOT), paths);
			}
		} catch (IOException e) {
			MoreMobs.LOGGER.warn("Asset bridge: could not read manifest: {}", e.toString());
		}
		return map;
	}
}
