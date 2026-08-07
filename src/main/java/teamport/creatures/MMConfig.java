package teamport.creatures;

import net.minecraft.core.enums.Difficulty;
import turniplabs.halplibe.util.TomlConfigHandler;
import turniplabs.halplibe.util.toml.Toml;

import static teamport.creatures.MoreMobs.MOD_ID;

/**
 * {@code config/creatures.cfg} — the original mod's settings, carried over.
 *
 * <p>DrZhark's v2.12.2 kept its settings in {@code mocreatures.cfg} and edited them through a GUI
 * API screen under <i>Options / Mods Settings</i>, laid out as six panels: Spawn Limits, Animals,
 * Hunter Creatures, Hostile Mobs, Water Mobs and Other Settings. BTA 8.0 has no equivalent settings
 * screen for mods to hang a panel off, so the same knobs live in this file instead. The categories
 * below are those six panels, and every entry names the original key it came from so a
 * {@code mocreatures.cfg} can be transcribed by hand.
 *
 * <p>Defaults are the original's own defaults throughout, with two exceptions that are called out
 * where they occur: {@link #ogreStrength} and its two siblings are rescaled because this port has no
 * explosion for them to size, and the spawn <em>frequencies</em> keep the BTA-pool weights this mod
 * already shipped rather than the original's 0-10 scale.
 *
 * <p><b>Not carried over.</b> Five original settings have nothing in this port to attach to, so they
 * are absent rather than present and inert:
 * <ul>
 *   <li>{@code EasyHorseBreeding} — horses do not breed here at all.</li>
 *   <li>{@code PegasusSpawningP} — the original rolled pegasus as a variant of one horse entity, so
 *       it needed a percentage. This port gives the pegasus its own entity and its own weight, which
 *       is {@code SpawnFrequencies.pegasus} and is strictly more expressive.</li>
 *   <li>{@code DisplayPetNames}, {@code DisplayPetHealth}, {@code DisplayPetEmotions} — there is no
 *       pet naming, no medallion and no nameplate rendering in this port.</li>
 *   <li>{@code StaticKBeds} — the kitty bed was never ported.</li>
 *   <li>{@code StaticLitter} — the litter box is a block here and so is always static; the setting
 *       would be permanently at its default.</li>
 * </ul>
 */
public class MMConfig {
	private static final Toml TOML = new Toml("Mo' Creatures TOML Config");
	public static TomlConfigHandler cfg;

	// --- Cached values ----------------------------------------------------------------------------
	// Read once at startup rather than per call: several of these are consulted from findPlayerToAttack
	// and findSwimTarget, which run every tick for every mob of that kind, and TomlConfigHandler.get
	// walks a category map per lookup. Everything here is fixed for the run either way, since the file
	// is only read at startup.

	/** {@code MobsSpawnLimit} — hostile mobs allowed per 256 eligible chunks. */
	public static int maxHostiles;
	/** {@code AnimalsSpawnLimit} — land animals allowed per 256 eligible chunks. */
	public static int maxAnimals;
	/** {@code WaterMobSpawnLimit} — water mobs allowed per 256 eligible chunks. */
	public static int maxWaterMobs;

	/** {@code HuntersAttackHorses} — whether predators count horses as prey. */
	public static boolean huntersAttackHorses;
	/** {@code HuntersAttackWolves} — whether predators count wolves and dogs as prey. */
	public static boolean huntersAttackWolves;
	/** {@code HuntersDestroyDrops} — whether a predator eats what its kill dropped. */
	public static boolean huntersDestroyDrops;

	/** {@code ogreSpawnDifficulty} — lowest difficulty a green ogre spawns on. */
	public static Difficulty ogreSpawnDifficulty;
	/** {@code FireOgreSpawnDifficulty} — lowest difficulty a fire ogre spawns on. */
	public static Difficulty fireOgreSpawnDifficulty;
	/** {@code CaveOgreSpawnDifficulty} — lowest difficulty a cave ogre spawns on. */
	public static Difficulty caveOgreSpawnDifficulty;
	/** {@code wereSpawnDifficulty} — lowest difficulty a werewolf spawns on. */
	public static Difficulty werewolfSpawnDifficulty;
	/** {@code wraithSpawnDifficulty} — lowest difficulty a wraith spawns on. */
	public static Difficulty wraithSpawnDifficulty;
	/** {@code flameWraithSpawnDifficulty} — lowest difficulty a flame wraith spawns on. */
	public static Difficulty flameWraithSpawnDifficulty;
	/** {@code sharkSpawnDifficulty} — lowest difficulty a shark spawns on. */
	public static Difficulty sharkSpawnDifficulty;

	/** {@code OgreStrength} — how much a green ogre can chew through. See {@link #blastCeiling}. */
	public static float ogreStrength;
	/** {@code FireOgreStrength} — the fire ogre's equivalent. */
	public static float fireOgreStrength;
	/** {@code CaveOgreStrength} — the cave ogre's equivalent. */
	public static float caveOgreStrength;
	/** {@code OgreRange} — how far off an ogre smells a player, in blocks. */
	public static int ogreRange;

	/** {@code DolphinsAttackSharks} — whether dolphins and sharks pick fights with each other. */
	public static boolean dolphinsAttackSharks;
	/** {@code SpawnPiranhas} — whether the tenth lil' fish variety is ever rolled. */
	public static boolean spawnPiranhas;

	static {
		TOML.addCategory("IDs")
			.addEntry("startingItemID", 17000)
			.addEntry("startingBlockID", 2800)
			.addEntry("startingEntityID", 100);

		// The original's "Spawn Limits" panel. These are global caps, not this mod's own: BTA counts
		// every mob in the category, so raising the animal cap lifts cows and sheep along with bunnies.
		// That is exactly what the original did, and it is why it needed to — this mod roughly doubles
		// the passive spawn pool, and under BTA's stock cap of 15 the newcomers would simply crowd the
		// vanilla animals out at unchanged total density rather than adding to it.
		//
		// BTA's own values are 70 / 15 / 5. Setting them back to those restores stock behaviour.
		TOML.addCategory(
				"Original panel: \"Spawn Limits\". Mobs allowed per 256 eligible chunks, counted across "
					+ "every mob in the category rather than just this mod's. BTA's stock values are "
					+ "70 / 15 / 5; the defaults here are the original mod's, which raised the passive "
					+ "and water caps to make room for the mobs it adds.",
				"SpawnLimits")
			.addEntry("maxHostiles", 70)       // MobsSpawnLimit 70
			.addEntry("maxAnimals", 30)        // AnimalsSpawnLimit 30
			.addEntry("maxWaterMobs", 25);     // WaterMobSpawnLimit 25

		// Every number here is the original mod's own setting, rescaled to BTA's spawn pool. The
		// original ran on a 0-10 scale that sat alongside beta 1.7.3's weights; BTA kept the monster
		// and water weights beta used but multiplied the passive ones by about ten, so one factor for
		// the lot would be wrong. Both baselines were read out of the jars rather than assumed:
		//
		//                    beta 1.7.3            BTA 8.0.1                        factor
		//   MONSTER          5 x 10        =  50   10,10,2,10,10,10,2      =  64    x1.28
		//   CREATURE         12,10,10,8    =  40   102,102,102,102,10      = 418    x10.45
		//   WATER_CREATURE   squid 10      =  10   squid 10               =  10    x1.00
		//
		// (Butterflies 65 and fireflies 10 are AMBIENT, not CREATURE, so they stay out of the
		// passive total.) Original setting -> converted weight is given per line below.
		//
		// Setting any of these to 0 removes that mob from every spawn list, which is how the original's
		// own "activate/deactivate individual mobs" worked: its spawn checks all began `if (freq > 0)`.
		TOML.addCategory(
				"Relative spawn weights, on BTA's scale rather than the original's 0-10 one. Set any "
					+ "entry to 0 to stop that mob spawning naturally at all.",
				"SpawnFrequencies")
			// Passive: original x10.45.
			.addEntry("bear", 21)            // FreqBear 2
			// FreqPBear 2 -- the same weight, because the original set both to 2. The polar bear is a
			// variant of MobBear here rather than its own entity, and MobBear.spawnInit turns a bear
			// polar in exactly the glacier and tundra biomes, so this is the bear's weight in those
			// two biomes and "bear" above is its weight everywhere else.
			.addEntry("bear_polar", 21)
			.addEntry("bird", 52)            // FreqBird 5
			.addEntry("bunny", 63)           // FreqBunny 6
			.addEntry("boar", 31)            // FreqBoar 3
			.addEntry("deer", 63)            // FreqDeer 6
			.addEntry("duck", 63)            // FreqDuck 6
			.addEntry("fox", 42)             // FreqFox 4
			.addEntry("horse", 52)           // FreqHorse 5
			// The original had no separate unicorn or pegasus spawn: EntityHorse picked the variant
			// itself. This port made them their own entities, so they keep a token weight rather than
			// vanishing from the world entirely.
			.addEntry("unicorn", 1)
			.addEntry("pegasus", 1)
			.addEntry("kitty", 42)           // FreqKitty 4
			.addEntry("bigcat", 31)          // FreqLion 3
			.addEntry("mouse", 63)           // FreqMice 6
			// Monsters: original x1.28.
			.addEntry("rat", 10)             // FreqRat 8
			.addEntry("rat_hell", 10)        // FreqHellRat 8 -- Nether only, as in the original
			.addEntry("ogre", 8)             // FreqOgre 6
			.addEntry("ogre_fire", 3)        // FreqFOgre 2
			.addEntry("ogre_cave", 4)        // FreqCOgre 3
			.addEntry("werewolf", 8)         // FreqWereWolf 6
			.addEntry("werewolf_wolf", 10)   // FreqWildWolf 8
			.addEntry("wraith", 6)           // FreqWraith 5
			.addEntry("wraith_flame", 3)     // FreqFWraith 2
			// Water: original x1.00.
			.addEntry("dolphin", 2)          // FreqDolphin 2
			.addEntry("shark", 2)            // FreqShark 2
			.addEntry("fishy", 10)           // FreqFishy 10
			// FreqSquid 1 (of a 0-3 range). BTA's squid is not this mod's mob, so this is the vanilla
			// weight left where BTA set it; the original exposed it purely so squid could be thinned
			// out to leave room for the water mobs it adds. Lower it to do that.
			.addEntry("squid", 10);

		TOML.addCategory(
				"Original panel: \"Hunter Creatures\". Which animals the mod's predators — big cats, "
					+ "bears, boars, wild wolves, sharks and piranhas — are willing to hunt.",
				"Hunters")
			.addEntry("attackHorses",
				"Whether predators treat horses, unicorns and pegasi as prey. HuntersAttackHorses.",
				false)
			.addEntry("attackWolves",
				"Whether predators treat wolves and tamed dogs as prey. HuntersAttackWolves.",
				false)
			.addEntry("destroyDrops",
				"Whether a predator eats the items its kill dropped. Only ever touches drops fresh "
					+ "enough to have come from the kill itself, never a player's. HuntersDestroyDrops.",
				true);

		// Difficulties are "this or harder", which is how the original read them: its check was
		// `world.difficultySetting >= setting + 1` against a 0=Easy/1=Normal/2=Hard list.
		TOML.addCategory(
				"Original panel: \"Hostile Mobs\". Difficulties are the lowest setting the mob spawns "
					+ "on, and mean that difficulty or harder: peaceful, easy, normal or hard.",
				"HostileMobs")
			.addEntry("ogreSpawnDifficulty", "ogreSpawnDifficulty.", "normal")
			.addEntry("fireOgreSpawnDifficulty", "FireOgreSpawnDifficulty.", "hard")
			.addEntry("caveOgreSpawnDifficulty", "CaveOgreSpawnDifficulty.", "normal")
			.addEntry("werewolfSpawnDifficulty", "wereSpawnDifficulty.", "normal")
			.addEntry("wraithSpawnDifficulty", "wraithSpawnDifficulty.", "normal")
			.addEntry("flameWraithSpawnDifficulty", "flameWraithSpawnDifficulty.", "hard")
			// The original's OgreStrength was the force of a literal explosion: EntityOgre handed
			// destroyForce to Destroyer.DestroyBlast, which cast rays and tested each block's explosion
			// resistance against a decaying intensity, exactly as TNT does. This port does not explode —
			// it removes a bounded slab of blocks between the ogre and its target — so there is no
			// force to set. What the number does here is scale the blast-resistance ceiling below which
			// a block can be broken, proportionally, so the original's default of 2.5 reproduces this
			// port's existing ceiling of 60 exactly. See MobOgre.blastCeiling.
			.addEntry("ogreStrength",
				"How much a green ogre can chew through. Original range 0.1 to 5.0; 2.5 leaves stone "
					+ "breakable and obsidian and bedrock safe. OgreStrength.",
				2.5F)
			.addEntry("fireOgreStrength", "FireOgreStrength.", 2.0F)
			.addEntry("caveOgreStrength", "CaveOgreStrength.", 3.0F)
			.addEntry("ogreRange",
				"How far off an ogre smells a player, in blocks. Original range 1 to 24. OgreRange.",
				12);

		TOML.addCategory(
				"Original panel: \"Water Mobs\".",
				"WaterMobs")
			.addEntry("sharkSpawnDifficulty",
				"Lowest difficulty a shark spawns on, that difficulty or harder. sharkSpawnDifficulty.",
				"easy")
			.addEntry("dolphinsAttackSharks",
				"Whether dolphins and sharks pick fights with each other. DolphinsAttackSharks.",
				true)
			.addEntry("spawnPiranhas",
				"Whether the piranha is ever rolled as a lil' fish variety. Fish that have already "
					+ "hatched keep whatever they are. SpawnPiranhas.",
				true);

		TOML.addCategory("Replacements")
			.addEntry("replaceVanillaDeer",
				"Swap BTA's built-in deer out of every biome's spawn list for the Mo' Creatures deer. " +
					"Set to false to keep the vanilla deer and leave ours unspawned.",
				true);

		cfg = new TomlConfigHandler(MOD_ID, TOML);
		cache();
	}

	private MMConfig() {
	}

	private static void cache() {
		maxHostiles = cfg.getInt("SpawnLimits.maxHostiles");
		maxAnimals = cfg.getInt("SpawnLimits.maxAnimals");
		maxWaterMobs = cfg.getInt("SpawnLimits.maxWaterMobs");

		huntersAttackHorses = cfg.getBoolean("Hunters.attackHorses");
		huntersAttackWolves = cfg.getBoolean("Hunters.attackWolves");
		huntersDestroyDrops = cfg.getBoolean("Hunters.destroyDrops");

		ogreSpawnDifficulty = difficulty("HostileMobs.ogreSpawnDifficulty", Difficulty.NORMAL);
		fireOgreSpawnDifficulty = difficulty("HostileMobs.fireOgreSpawnDifficulty", Difficulty.HARD);
		caveOgreSpawnDifficulty = difficulty("HostileMobs.caveOgreSpawnDifficulty", Difficulty.NORMAL);
		werewolfSpawnDifficulty = difficulty("HostileMobs.werewolfSpawnDifficulty", Difficulty.NORMAL);
		wraithSpawnDifficulty = difficulty("HostileMobs.wraithSpawnDifficulty", Difficulty.NORMAL);
		flameWraithSpawnDifficulty = difficulty("HostileMobs.flameWraithSpawnDifficulty", Difficulty.HARD);
		sharkSpawnDifficulty = difficulty("WaterMobs.sharkSpawnDifficulty", Difficulty.EASY);

		ogreStrength = cfg.getFloat("HostileMobs.ogreStrength");
		fireOgreStrength = cfg.getFloat("HostileMobs.fireOgreStrength");
		caveOgreStrength = cfg.getFloat("HostileMobs.caveOgreStrength");
		ogreRange = cfg.getInt("HostileMobs.ogreRange");

		dolphinsAttackSharks = cfg.getBoolean("WaterMobs.dolphinsAttackSharks");
		spawnPiranhas = cfg.getBoolean("WaterMobs.spawnPiranhas");
	}

	/**
	 * Relative weight of one mob on the spawn list, as {@code SpawnFrequencies} has it.
	 *
	 * @return 0 or less if the mob should not be placed on any spawn list at all
	 */
	public static int frequency(String entity) {
		return cfg.getInt("SpawnFrequencies." + entity);
	}

	/**
	 * The blast-resistance ceiling an ogre of the given strength can break through.
	 *
	 * <p>Proportional, and pinned so that the original's default green-ogre strength of 2.5 gives the
	 * 60 this port already used — see the {@code ogreStrength} note above for why a proportion is all
	 * that can be salvaged from a setting that used to size an explosion.
	 */
	public static float blastCeiling(float strength) {
		return Math.max(0.0F, strength) * (60.0F / 2.5F);
	}

	/**
	 * Reads a difficulty by name, tolerating case and surrounding whitespace.
	 *
	 * @param fallback used, with a warning, for anything unrecognised — a typo in the config should
	 *                 not stop the game from starting
	 */
	private static Difficulty difficulty(String key, Difficulty fallback) {
		String raw = cfg.getString(key);
		if (raw != null) {
			for (Difficulty candidate : Difficulty.values()) {
				if (candidate.name().equalsIgnoreCase(raw.trim())) {
					return candidate;
				}
			}
		}
		MoreMobs.LOGGER.warn("{} is \"{}\", which is not a difficulty. Using {}. Expected one of: {}.",
			key, raw, fallback.name().toLowerCase(), "peaceful, easy, normal, hard");
		return fallback;
	}

	/**
	 * Whether the world is at or above the difficulty a mob needs to spawn.
	 *
	 * <p>"At or above" is the original's own reading: its spawn checks were
	 * {@code world.difficultySetting >= setting + 1} against a zero-based Easy/Normal/Hard list, so a
	 * mob set to Normal spawns on Normal and Hard. Peaceful is always refused, difficulty setting or
	 * not, because nothing hostile spawns there at all.
	 */
	public static boolean spawnsAt(Difficulty world, Difficulty minimum) {
		return world.canHostileMobsSpawn() && world.id() >= minimum.id();
	}
}
