package teamport.creatures.core;

import net.minecraft.client.Minecraft;
import teamport.creatures.MoreMobs;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Bridges entity textures out of a copy of the original Mo' Creatures that the <em>player</em>
 * supplies.
 * <p>
 * This mod ships no art from the original. Mo' Creatures is DrZhark's work and its licence does not
 * permit redistribution, so the textures cannot live in this repository. Instead, if the player drops
 * their own copy of the original mod into the game directory, this reads the images out of it and
 * writes a generated texture pack that BTA then loads like any other pack. Nothing is downloaded and
 * nothing is redistributed — the file has to already be on the player's disk.
 * <p>
 * Mapping is driven by {@code /assets/creatures/asset-bridge.properties} rather than hardcoded, so
 * adding a mob is a one-line manifest edit. Original archives differ in internal layout between
 * versions, so entries are matched on the file's <em>base name</em> and the directory structure
 * inside the archive is ignored.
 */
public final class MMAssetBridge {
	private MMAssetBridge() {}

	public static final String PACK_NAME = "MoCreaturesAssets";
	private static final String MANIFEST = "/assets/creatures/asset-bridge.properties";

	/** Set once the bridge has run, so the audit can report on it. */
	public static int bridgedCount = -1;
	public static int missingCount = -1;
	public static String sourceArchive = null;

	public static void run() {
		File gameDir;
		try {
			gameDir = Minecraft.getMinecraft().getMinecraftDir();
		} catch (Throwable t) {
			// Dedicated server, or called before the client exists. Nothing to bridge.
			return;
		}
		if (gameDir == null) return;

		Map<String, String> manifest = readManifest();
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
				new File(gameDir, "mocreatures-assets").getPath());
			return;
		}
		sourceArchive = archive.getName();

		File packDir = new File(gameDir, "texturepacks/" + PACK_NAME);
		List<String> missing = new ArrayList<>();
		int written = 0;

		try (ZipFile zip = new ZipFile(archive)) {
			// Index the archive by base name so layout differences between versions do not matter.
			Map<String, ZipEntry> byBaseName = new HashMap<>();
			for (java.util.Enumeration<? extends ZipEntry> e = zip.entries(); e.hasMoreElements(); ) {
				ZipEntry entry = e.nextElement();
				if (entry.isDirectory()) continue;
				String name = entry.getName();
				if (!name.toLowerCase(Locale.ROOT).endsWith(".png")) continue;
				byBaseName.putIfAbsent(baseName(name), entry);
			}

			for (Map.Entry<String, String> mapping : manifest.entrySet()) {
				ZipEntry entry = byBaseName.get(mapping.getKey().toLowerCase(Locale.ROOT));
				if (entry == null) {
					missing.add(mapping.getKey());
					continue;
				}
				File target = new File(packDir, mapping.getValue());
				if (!target.getParentFile().isDirectory() && !target.getParentFile().mkdirs()) {
					missing.add(mapping.getKey());
					continue;
				}
				try (InputStream in = zip.getInputStream(entry); OutputStream out = new FileOutputStream(target)) {
					byte[] buffer = new byte[8192];
					int read;
					while ((read = in.read(buffer)) > 0) out.write(buffer, 0, read);
				}
				written++;
			}

			writePackMeta(packDir);
		} catch (IOException e) {
			MoreMobs.LOGGER.warn("Asset bridge: could not read '{}': {}", archive.getName(), e.toString());
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
		if (written > 0) {
			MoreMobs.LOGGER.info("Asset bridge: enable the '{}' texture pack in Options to use them", PACK_NAME);
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

	private static void writePackMeta(File packDir) throws IOException {
		File info = new File(packDir, "pack.txt");
		if (info.isFile()) return;
		try (OutputStream out = new FileOutputStream(info)) {
			out.write(("Generated by Mo' Creatures for BTA from a locally supplied copy of the original mod.\n"
				+ "Not redistributable. Delete this folder to remove it.\n").getBytes("UTF-8"));
		}
	}

	private static String baseName(String path) {
		int slash = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
		return path.substring(slash + 1).toLowerCase(Locale.ROOT);
	}

	private static Map<String, String> readManifest() {
		Map<String, String> map = new HashMap<>();
		try (InputStream in = MMAssetBridge.class.getResourceAsStream(MANIFEST)) {
			if (in == null) return map;
			Properties props = new Properties();
			props.load(in);
			for (String key : props.stringPropertyNames()) {
				map.put(key.toLowerCase(Locale.ROOT), props.getProperty(key));
			}
		} catch (IOException e) {
			MoreMobs.LOGGER.warn("Asset bridge: could not read manifest: {}", e.toString());
		}
		return map;
	}
}
