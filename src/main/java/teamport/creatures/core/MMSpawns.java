package teamport.creatures.core;

import java.util.Iterator;
import java.util.List;

import net.minecraft.core.data.registry.Registries;
import net.minecraft.core.entity.Entity;
import net.minecraft.core.entity.SpawnListEntry;
import net.minecraft.core.entity.animal.MobDeer;
import net.minecraft.core.entity.animal.MobSquid;
import net.minecraft.core.enums.MobCategory;
import net.minecraft.core.world.biome.Biome;
import net.minecraft.core.world.biome.Biomes;

import teamport.creatures.MMConfig;
import teamport.creatures.MoreMobs;
import teamport.creatures.core.entity.mob.MobBear;
import teamport.creatures.core.entity.mob.MobBigCat;
import teamport.creatures.core.entity.mob.MobBird;
import teamport.creatures.core.entity.mob.MobBoar;
import teamport.creatures.core.entity.mob.MobBunny;
import teamport.creatures.core.entity.mob.MobDeerMoC;
import teamport.creatures.core.entity.mob.MobDolphin;
import teamport.creatures.core.entity.mob.MobDuck;
import teamport.creatures.core.entity.mob.MobFishy;
import teamport.creatures.core.entity.mob.MobFox;
import teamport.creatures.core.entity.mob.MobHorse;
import teamport.creatures.core.entity.mob.MobHorsePegasus;
import teamport.creatures.core.entity.mob.MobHorseUnicorn;
import teamport.creatures.core.entity.mob.MobKitty;
import teamport.creatures.core.entity.mob.MobMouse;
import teamport.creatures.core.entity.mob.MobOgre;
import teamport.creatures.core.entity.mob.MobOgreCave;
import teamport.creatures.core.entity.mob.MobOgreFire;
import teamport.creatures.core.entity.mob.MobRat;
import teamport.creatures.core.entity.mob.MobRatHell;
import teamport.creatures.core.entity.mob.MobShark;
import teamport.creatures.core.entity.mob.MobWerewolf;
import teamport.creatures.core.entity.mob.MobWerewolfWolf;
import teamport.creatures.core.entity.mob.MobWraith;
import teamport.creatures.core.entity.mob.MobWraithFlame;

public final class MMSpawns {

	private MMSpawns() {
	}

	private static final Object[][] CREATURES = {
		{MobBear.class, "bear", "bear_polar"},
		{MobBird.class, "bird"},
		{MobFox.class, "fox"},
		{MobBunny.class, "bunny"},
		{MobBoar.class, "boar"},
		{MobDuck.class, "duck"},
		{MobKitty.class, "kitty"},
		{MobHorse.class, "horse"},
		{MobHorseUnicorn.class, "unicorn"},
		{MobHorsePegasus.class, "pegasus"},
		{MobBigCat.class, "bigcat"},
		{MobMouse.class, "mouse"}
	};

	private static final Object[][] WATER_CREATURES = {
		{MobDolphin.class, "dolphin"},
		{MobShark.class, "shark"},
		{MobFishy.class, "fishy"}
	};

	private static final Object[][] MONSTERS = {
		{MobRat.class, "rat"},
		{MobOgre.class, "ogre"},
		{MobOgreFire.class, "ogre_fire"},
		{MobOgreCave.class, "ogre_cave"},
		{MobWerewolf.class, "werewolf"},
		{MobWerewolfWolf.class, "werewolf_wolf"},
		{MobWraith.class, "wraith"},
		{MobWraithFlame.class, "wraith_flame"}
	};

	private static final Object[][] NETHER_MONSTERS = {
		{MobOgreFire.class, "ogre_fire"},
		{MobWraithFlame.class, "wraith_flame"},
		{MobRatHell.class, "rat_hell"}
	};

	public static void apply(Biome biome) {

		String key = biome.getRegistryKey();
		if (key == null) {
			return;
		}

		if (isNether(key)) {
			addMissing(biome.getSpawnableList(MobCategory.MONSTER), NETHER_MONSTERS, biome);
			return;
		}

		addMissing(biome.getSpawnableList(MobCategory.CREATURE), CREATURES, biome);
		addMissing(biome.getSpawnableList(MobCategory.WATER_CREATURE), WATER_CREATURES, biome);
		addMissing(biome.getSpawnableList(MobCategory.MONSTER), MONSTERS, biome);
		applyDeerReplacement(biome.getSpawnableList(MobCategory.CREATURE));
		applySquidFrequency(biome.getSpawnableList(MobCategory.WATER_CREATURE));
	}

	private static boolean isCold(Biome biome) {
		return biome == Biomes.OVERWORLD_GLACIER || biome == Biomes.OVERWORLD_TUNDRA;
	}

	private static void applySquidFrequency(List<SpawnListEntry> water) {
		if (water == null) {
			return;
		}
		int weight = MMConfig.frequency("squid");
		Iterator<SpawnListEntry> entries = water.iterator();
		while (entries.hasNext()) {
			SpawnListEntry entry = entries.next();
			if (entry.entityClass != MobSquid.class) {
				continue;
			}

			if (weight <= 0) {
				entries.remove();
			} else {
				entry.spawnFrequency = weight;
			}
		}
	}

	private static boolean isNether(String registryKey) {
		int colon = registryKey.indexOf(':');
		String path = colon < 0 ? registryKey : registryKey.substring(colon + 1);
		return path.startsWith("nether.");
	}

	private static void applyDeerReplacement(List<SpawnListEntry> creatures) {
		if (creatures == null || !MMConfig.cfg.getBoolean("Replacements.replaceVanillaDeer")) {
			return;
		}
		Iterator<SpawnListEntry> entries = creatures.iterator();
		while (entries.hasNext()) {
			if (entries.next().entityClass == MobDeer.class) {
				entries.remove();
			}
		}
		int weight = frequency("deer");
		if (weight > 0 && !contains(creatures, MobDeerMoC.class)) {
			creatures.add(new SpawnListEntry(MobDeerMoC.class, weight));
		}
	}

	private static void addMissing(List<SpawnListEntry> list, Object[][] table, Biome biome) {
		if (list == null) {
			return;
		}
		boolean cold = isCold(biome);
		for (Object[] row : table) {
			@SuppressWarnings("unchecked")
			Class<? extends Entity> type = (Class<? extends Entity>) row[0];
			if (contains(list, type)) {
				continue;
			}
			int weight = frequency((String) (cold && row.length > 2 ? row[2] : row[1]));
			if (weight > 0) {
				list.add(new SpawnListEntry(type, weight));
			}
		}
	}

	private static boolean contains(List<SpawnListEntry> list, Class<? extends Entity> type) {
		for (SpawnListEntry entry : list) {
			if (entry.entityClass == type) {
				return true;
			}
		}
		return false;
	}

	private static int frequency(String entity) {
		return MMConfig.frequency(entity);
	}

	public static void sweep() {
		int restored = 0;
		int touched = 0;
		for (Biome biome : Registries.BIOMES) {
			if (biome == null) {
				continue;
			}
			int before = count(biome);
			apply(biome);
			int added = count(biome) - before;
			if (added > 0) {
				restored += added;
				touched++;
			}
		}
		if (restored > 0) {
			MoreMobs.LOGGER.info(
				"Spawn sweep: restored {} entries across {} biomes that cleared their lists after construction.",
				restored, touched);
		}
	}

	private static int count(Biome biome) {
		return size(biome.getSpawnableList(MobCategory.CREATURE))
			+ size(biome.getSpawnableList(MobCategory.WATER_CREATURE))
			+ size(biome.getSpawnableList(MobCategory.MONSTER));
	}

	private static int size(List<SpawnListEntry> list) {
		return list == null ? 0 : list.size();
	}
}
