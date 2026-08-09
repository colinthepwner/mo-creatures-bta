package teamport.creatures.core;

import net.minecraft.core.entity.EntityDispatcher;
import net.minecraft.core.util.collection.NamespaceID;
import teamport.creatures.MoreMobs;
import teamport.creatures.core.entity.MMEntities;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.CRC32;

public final class MMAudit {
	private MMAudit() {}

	private static final String MODEL_DIR = "/assets/creatures/models/entity/";
	private static final String TEXTURE_DIR = "/assets/creatures/textures/entity/";
	private static final String LANG_FILE = "/lang/creatures/en_US/guidebook.lang";
	private static final String MODEL_BRIDGE_MANIFEST = "/assets/creatures/model-bridge.properties";

	public static void run() {
		List<String> ids = MMEntities.REGISTERED_IDS;
		List<String> problems = new ArrayList<>();

		Set<String> langKeys = readLangKeys();

		Set<String> bridgeSupplied = readBridgeSuppliedIds();

		int withModel = 0;
		int withTexture = 0;
		int withLang = 0;
		int viaBridge = 0;

		for (String id : ids) {

			NamespaceID nsid = NamespaceID.getPermanent(MoreMobs.MOD_ID, id);
			if (EntityDispatcher.getInstance().idToEntryMap.get(nsid) == null) {
				problems.add("entity '" + id + "' is not in the dispatcher after registration");
			}

			boolean bridged = bridgeSupplied.contains(id);
			if (bridged) viaBridge++;

			if (resourceExists(MODEL_DIR + id + ".json")) {
				withModel++;
			} else if (!bridged) {
				problems.add("entity '" + id + "' has no model at " + MODEL_DIR + id + ".json"
					+ " and is not listed in " + MODEL_BRIDGE_MANIFEST);
			}

			if (resourceExists(TEXTURE_DIR + id + "/")) {
				withTexture++;
			} else if (!bridged) {
				problems.add("entity '" + id + "' has no texture directory at " + TEXTURE_DIR + id + "/"
					+ " and is not listed in " + MODEL_BRIDGE_MANIFEST);
			}

			String langId = id.contains("_") ? id.substring(id.indexOf('_') + 1) : id;
			if (langKeys.contains("guidebook.section.mob." + id + ".name")
				|| langKeys.contains("guidebook.section.mob." + langId + ".name")) {
				withLang++;
			} else {
				problems.add("entity '" + id + "' has no guidebook name key in " + LANG_FILE);
			}
		}

		if (problems.isEmpty()) {
			MoreMobs.LOGGER.info("Creatures audit: {} entities registered, all accounted for "
					+ "({} with built-in models, {} with built-in textures, {} supplied by the asset bridge, "
					+ "{} with lang key)",
				ids.size(), withModel, withTexture, viaBridge, withLang);
		} else {
			MoreMobs.LOGGER.warn("Creatures audit: {} entities registered, {} with model, {} with texture, "
					+ "{} via asset bridge, {} with lang key",
				ids.size(), withModel, withTexture, viaBridge, withLang);
			for (String problem : problems) {
				MoreMobs.LOGGER.warn("Creatures audit problem: {}", problem);
			}
		}

		MoreMobs.LOGGER.info("Creatures ID fingerprint: {} entries, hash {}", ids.size(), fingerprint(ids));

		List<String> summonIds = new ArrayList<>();
		for (String id : ids) {
			NamespaceID nsid = NamespaceID.getPermanent(MoreMobs.MOD_ID, id);
			Object entry = EntityDispatcher.getInstance().idToEntryMap.get(nsid);
			summonIds.add(nsid + (entry == null ? " (MISSING)" : ""));
		}
		MoreMobs.LOGGER.info("Creatures summon ids: {}", String.join(", ", summonIds));
	}

	public static void reportBridges() {
		if (MMAssetBridge.sourceArchive == null) {
			MoreMobs.LOGGER.info("Creatures audit: no original Mo' Creatures archive supplied, "
				+ "so built-in art and models are in use");
			return;
		}
		if (MMAssetBridge.usedCache) {

			MoreMobs.LOGGER.info("Creatures audit: texture pack '{}' was already built from {} and was reused; "
				+ "delete it to force a rebuild. Pack auto-enabled: {}", MMAssetBridge.PACK_NAME,
				MMAssetBridge.sourceArchive, MMAssetBridge.packAutoEnabled);
			return;
		}
		MoreMobs.LOGGER.info("Creatures audit: asset bridge read {} — {} textures bridged, {} not found, "
			+ "pack auto-enabled: {}", MMAssetBridge.sourceArchive, MMAssetBridge.bridgedCount,
			MMAssetBridge.missingCount, MMAssetBridge.packAutoEnabled);

		if (MMGeometryBridge.convertedCount < 0) {
			MoreMobs.LOGGER.warn("Creatures audit problem: geometry bridge did not run");
			return;
		}
		MoreMobs.LOGGER.info("Creatures audit: geometry bridge converted {} models "
			+ "({} self-contained, {} completed with vanilla base geometry), {} left alone",
			MMGeometryBridge.convertedCount + MMGeometryBridge.composedCount,
			MMGeometryBridge.convertedCount, MMGeometryBridge.composedCount, MMGeometryBridge.skippedCount);
		for (String problem : MMGeometryBridge.problems) {
			MoreMobs.LOGGER.warn("Creatures audit problem: geometry bridge: {}", problem);
		}
	}

	private static String fingerprint(List<String> ids) {
		CRC32 crc = new CRC32();
		for (String id : ids) {
			crc.update(id.getBytes(StandardCharsets.UTF_8));
			crc.update(';');
		}
		return String.format("%08X", crc.getValue());
	}

	private static boolean resourceExists(String path) {
		return MMAudit.class.getResource(path) != null;
	}

	private static Set<String> readBridgeSuppliedIds() {
		Set<String> ids = new HashSet<>();
		try (InputStream in = MMAudit.class.getResourceAsStream(MODEL_BRIDGE_MANIFEST)) {
			if (in == null) return ids;
			java.util.Properties props = new java.util.Properties();
			props.load(in);
			for (String key : props.stringPropertyNames()) {
				int dot = key.indexOf('.');
				ids.add(dot > 0 ? key.substring(0, dot) : key);
			}
		} catch (IOException e) {
			MoreMobs.LOGGER.warn("Creatures audit problem: could not read {}: {}", MODEL_BRIDGE_MANIFEST, e.toString());
		}
		return ids;
	}

	private static Set<String> readLangKeys() {
		Set<String> keys = new HashSet<>();
		try (InputStream in = MMAudit.class.getResourceAsStream(LANG_FILE)) {
			if (in == null) {
				MoreMobs.LOGGER.warn("Creatures audit problem: lang file {} is missing entirely", LANG_FILE);
				return keys;
			}
			BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
			String line;
			while ((line = reader.readLine()) != null) {
				int eq = line.indexOf('=');
				if (eq > 0 && !line.startsWith("#")) {
					keys.add(line.substring(0, eq).trim());
				}
			}
		} catch (IOException e) {
			MoreMobs.LOGGER.warn("Creatures audit problem: could not read {}: {}", LANG_FILE, e.toString());
		}
		return keys;
	}
}
