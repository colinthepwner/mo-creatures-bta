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

/**
 * The one spawn table, and the one place that puts it on a biome.
 *
 * <p>Split out of {@code BiomeMixin} because the constructor is not the last word on a biome's
 * spawn lists. The mixin fires at the tail of {@code Biome.<init>}, which is where a subclass's
 * {@code super(...)} call returns — so a subclass whose own constructor body then calls
 * {@code spawnableCreatureList().clear()} to drop the defaults it does not want takes this mod's
 * entries out with them. That is a perfectly reasonable thing for a biome to do, and it is common:
 * a biome that wants no passive mobs clears the list rather than fighting whatever put entries in
 * it. The result is a biome that spawns nothing of this mod's, with no error anywhere.
 *
 * <p>So the real placement happens in a sweep over {@link Registries#BIOMES} after the game has
 * started and every biome — from any mod — is built and registered. {@link #apply(Biome)} is
 * idempotent, and returns without doing anything while a biome is still under construction, because
 * a biome's registry key is not bound until it is registered and the key is what says whether this
 * is the Nether.
 *
 * <p>The weights are the original mod's own settings converted to BTA's pool; see
 * {@link teamport.creatures.MMConfig} for the two baselines and the per-category factors.
 */
public final class MMSpawns {

	private MMSpawns() {
	}

	/**
	 * Land animals. Overworld only, as in the original.
	 *
	 * <p>A row is {@code {class, frequency key}}, or {@code {class, frequency key, cold-biome key}}
	 * for a mob whose cold-biome form had its own frequency in the original. Only the bear does: it
	 * is one entity here with a polar variant that {@link MobBear#spawnInit()} switches on in exactly
	 * the glacier and tundra, so {@code bear_polar} stands in for the original's {@code FreqPBear}
	 * across those two biomes and {@code bear} covers the rest.
	 */
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

	/** Aquatic mobs use the dedicated water list; the creature list only spawns on land. */
	private static final Object[][] WATER_CREATURES = {
		{MobDolphin.class, "dolphin"},
		{MobShark.class, "shark"},
		{MobFishy.class, "fishy"}
	};

	/** Hostiles belong on the monster list so they obey the difficulty and light spawn rules. */
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

	/**
	 * What the Nether gets, and all it gets.
	 *
	 * <p>The original registered most of its roster through {@code ModLoader.AddSpawn(class, freq,
	 * type)} with no biome array, which walks {@code BiomeGenBase.biomeList} — the 64x64 climate
	 * lookup filled purely from temperature and rainfall. Hell is never written into that table, so
	 * that call never reached it. Exactly three mobs were registered against hell explicitly, by
	 * passing the biome array form with {@code BiomeGenBase.hell}: the fire ogre, the flame wraith
	 * and the hell rat.
	 *
	 * <p>This port had been putting all twenty-four entries into every biome including the Nether.
	 * That is what made ogres pile up there: {@code BiomeNether} clears its inherited spawn lists and
	 * keeps only a ghast and a zombie pigman, 20 weight between them, so the mod's 64 was three
	 * quarters of everything that could spawn — a fifth of it ogres the original never put in hell at
	 * all.
	 */
	private static final Object[][] NETHER_MONSTERS = {
		{MobOgreFire.class, "ogre_fire"},
		{MobWraithFlame.class, "wraith_flame"},
		{MobRatHell.class, "rat_hell"}
	};

	/**
	 * Puts every entry this mod owns on one biome, skipping any already present.
	 *
	 * <p>Safe to call more than once on the same biome: that is the whole point, since it runs once
	 * from the constructor and again from {@link #sweep()}.
	 */
	public static void apply(Biome biome) {
		// A biome's registry key is bound when it is registered, which is after its constructor has
		// run — so at construction there is no way to tell the Nether from anywhere else, and putting
		// the overworld roster in first and correcting later is exactly the bug this avoids. The
		// sweep, which runs once everything is registered, does the real work.
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

	/**
	 * The biomes {@link MobBear#spawnInit()} turns a bear polar in, and so the biomes whose bears are
	 * weighted by {@code bear_polar} rather than {@code bear}.
	 */
	private static boolean isCold(Biome biome) {
		return biome == Biomes.OVERWORLD_GLACIER || biome == Biomes.OVERWORLD_TUNDRA;
	}

	/**
	 * Applies {@code SpawnFrequencies.squid} to BTA's own squid.
	 *
	 * <p>Not this mod's mob, and included only because the original exposed it for one reason: it
	 * raises the water cap from BTA's 5 to 25 to fit dolphins, sharks and fish in, and squid would
	 * otherwise take that new headroom first. {@code Biome}'s constructor adds squid once at a flat
	 * weight of 10, so this is an outright override of that number rather than a scaling of it, and
	 * leaving the entry at its default of 10 changes nothing.
	 */
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
			// Zero means "do not spawn", which has to be a removal rather than a zeroed weight — see
			// addMissing for why a zero-weight entry is not the same thing.
			if (weight <= 0) {
				entries.remove();
			} else {
				entry.spawnFrequency = weight;
			}
		}
	}

	/**
	 * BTA names its biomes {@code minecraft:nether.crag}, {@code minecraft:overworld.forest} and so
	 * on. Only the {@code nether.} path counts — {@code overworld.hell} is a surface biome that
	 * merely looks the part, and biomes from other mods keep their own names and are treated as
	 * overworld, which is what they almost always are.
	 */
	private static boolean isNether(String registryKey) {
		int colon = registryKey.indexOf(':');
		String path = colon < 0 ? registryKey : registryKey.substring(colon + 1);
		return path.startsWith("nether.");
	}

	/**
	 * BTA 8.0.1 ships its own deer and adds it to every biome. Ours stands in for it rather than
	 * beside it, so the vanilla entry comes back out first.
	 */
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

	/**
	 * Adds every entry in the table that the biome does not already carry.
	 *
	 * <p>A frequency of zero or less means the mob is switched off and no entry is added at all. That
	 * is the original's own reading of a zero frequency — every one of its spawn checks opened with
	 * {@code if (freq > 0)} — and removing the entry rather than zeroing its weight is what
	 * {@link net.minecraft.core.world.SpawnerMobs} needs: it sums the weights and rolls
	 * {@code nextInt(total)}, which throws outright on an all-zero list, and it seeds its choice with
	 * {@code list.get(0)} before walking, so a zero-weight entry sitting first is still reachable.
	 */
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

	/**
	 * Second pass over every registered biome, once the game has started and nothing else is still
	 * building biomes. Restores whatever a biome's own constructor cleared after
	 * {@code Biome.<init>} had already run.
	 */
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
