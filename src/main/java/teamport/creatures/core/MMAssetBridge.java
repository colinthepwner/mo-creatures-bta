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
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.zip.ZipEntry;
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
	private static final int BRIDGE_REVISION = 11;

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

		File archive = findArchive(gameDir);
		if (archive == null) {
			bridgedCount = 0;
			missingCount = manifest.size();
			MoreMobs.LOGGER.info("Asset bridge: no Mo' Creatures archive found — mobs will use built-in textures "
				+ "where they exist. Drop the original mod jar/zip in '{}' to restore the original look.",
				new File(gameDir, "mods").getPath());
			return;
		}
		sourceArchive = archive.getName();

		File packDir = new File(gameDir, "texturepacks/" + PACK_NAME);
		if (isUpToDate(packDir, archive)) {
			usedCache = true;
			MoreMobs.LOGGER.info("Asset bridge: '{}' is already built from '{}', skipping extraction",
				PACK_NAME, archive.getName());
			enablePack(packDir);
			return;
		}

		// One walk of the archive serves both halves, so a nested zip is only unpacked once.
		Set<String> wanted = new LinkedHashSet<>(manifest.keySet());
		wanted.addAll(MMGeometryBridge.wantedEntries());
		Map<String, byte[]> entries;
		try {
			entries = collect(archive, wanted);
		} catch (IOException e) {
			MoreMobs.LOGGER.warn("Asset bridge: could not read '{}': {}", archive.getName(), e.toString());
			return;
		}

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

		MoreMobs.LOGGER.info("Asset bridge: {} textures bridged from '{}' into texture pack '{}'",
			written, archive.getName(), PACK_NAME);
		if (!missing.isEmpty()) {
			MoreMobs.LOGGER.warn("Asset bridge: {} textures not found in the archive: {}",
				missing.size(), String.join(", ", missing));
		}

		runGeometryBridge(entries, packDir);
		stamp(packDir, archive);
		enablePack(packDir);
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
	// Archive walk
	// ----------------------------------------------------------------------------------------------

	/**
	 * Flattens the archive to the entries the manifests asked for, keyed by lower-case base name.
	 * Zips nested inside the archive are walked too: the original's own download is a zip whose
	 * payload is {@code mods/MoCreatures.zip}, and reading only the outer layer finds nothing at all.
	 */
	private static Map<String, byte[]> collect(File archive, Set<String> wanted) throws IOException {
		Map<String, byte[]> found = new HashMap<>();
		try (InputStream in = new BufferedInputStream(new FileInputStream(archive))) {
			collect(in, wanted, found, 0);
		}
		return found;
	}

	private static void collect(InputStream in, Set<String> wanted, Map<String, byte[]> found, int depth)
		throws IOException {
		ZipInputStream zip = new ZipInputStream(in);
		ZipEntry entry;
		while ((entry = zip.getNextEntry()) != null) {
			if (entry.isDirectory()) continue;
			String name = baseName(entry.getName());
			if (wanted.contains(name)) {
				found.putIfAbsent(name, MMGeometryBridge.readFully(zip));
			} else if (depth < MAX_NESTING && (name.endsWith(".zip") || name.endsWith(".jar"))) {
				collect(zip, wanted, found, depth + 1);
			}
		}
	}

	/**
	 * Looks for a player-supplied archive. Checked in order: an explicit {@code mocreatures-assets}
	 * file, then anything in {@code mods/} whose name looks like Mo' Creatures.
	 */
	private static File findArchive(File gameDir) {
		for (String candidate : new String[]{"mocreatures-assets.zip", "mocreatures-assets.jar"}) {
			File direct = new File(gameDir, candidate);
			if (direct.isFile()) return direct;
		}
		File mods = new File(gameDir, "mods");
		File[] files = mods.listFiles();
		if (files == null) return null;
		for (File file : files) {
			String name = file.getName().toLowerCase(Locale.ROOT);
			if (!file.isFile()) continue;
			if (!name.endsWith(".zip") && !name.endsWith(".jar")) continue;
			// Skip this mod itself.
			if (name.contains("creatures") && !name.contains("mocreature") && !name.contains("mo_creature")) continue;
			if (name.contains("mocreature") || name.contains("mo_creature") || name.contains("mo-creature")) {
				return file;
			}
		}
		return null;
	}

	// ----------------------------------------------------------------------------------------------
	// Cache
	// ----------------------------------------------------------------------------------------------

	/** What the pack was last built from, so a second launch does not pay for the extraction again. */
	private static String stampFor(File archive) {
		return BRIDGE_REVISION + " " + archive.getName() + " " + archive.length() + " " + archive.lastModified();
	}

	private static boolean isUpToDate(File packDir, File archive) {
		File stamp = new File(packDir, STAMP);
		if (!stamp.isFile()) return false;
		try {
			return stampFor(archive).equals(new String(Files.readAllBytes(stamp.toPath()), StandardCharsets.UTF_8).trim());
		} catch (IOException e) {
			return false;
		}
	}

	private static void stamp(File packDir, File archive) {
		try {
			write(new File(packDir, STAMP), stampFor(archive).getBytes(StandardCharsets.UTF_8));
		} catch (IOException e) {
			MoreMobs.LOGGER.warn("Asset bridge: could not record what the pack was built from: {}", e.toString());
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
