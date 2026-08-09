package teamport.creatures;

import net.minecraft.core.enums.Difficulty;
import turniplabs.halplibe.util.TomlConfigHandler;
import turniplabs.halplibe.util.toml.Toml;

import static teamport.creatures.MoreMobs.MOD_ID;

public class MMConfig {
	private static final Toml TOML = new Toml("Mo' Creatures TOML Config");
	public static TomlConfigHandler cfg;

	public static int maxHostiles;

	public static int maxAnimals;

	public static int maxWaterMobs;

	public static boolean huntersAttackHorses;

	public static boolean huntersAttackWolves;

	public static boolean huntersDestroyDrops;

	public static Difficulty ogreSpawnDifficulty;

	public static Difficulty fireOgreSpawnDifficulty;

	public static Difficulty caveOgreSpawnDifficulty;

	public static Difficulty werewolfSpawnDifficulty;

	public static Difficulty wraithSpawnDifficulty;

	public static Difficulty flameWraithSpawnDifficulty;

	public static Difficulty sharkSpawnDifficulty;

	public static float ogreStrength;

	public static float fireOgreStrength;

	public static float caveOgreStrength;

	public static int ogreRange;

	public static boolean easyHorseBreeding;

	public static boolean dolphinsAttackSharks;

	public static boolean spawnPiranhas;

	static {
		TOML.addCategory("IDs")
			.addEntry("startingItemID", 17000)
			.addEntry("startingBlockID", 2800)
			.addEntry("startingEntityID", 100);

		TOML.addCategory(
				"Original panel: \"Spawn Limits\". Mobs allowed per 256 eligible chunks, counted across "
					+ "every mob in the category rather than just this mod's. BTA's stock values are "
					+ "70 / 15 / 5; the defaults here are the original mod's, which raised the passive "
					+ "and water caps to make room for the mobs it adds.",
				"SpawnLimits")
			.addEntry("maxHostiles", 70)
			.addEntry("maxAnimals", 30)
			.addEntry("maxWaterMobs", 25);

		TOML.addCategory(
				"Relative spawn weights, on BTA's scale rather than the original's 0-10 one. Set any "
					+ "entry to 0 to stop that mob spawning naturally at all.",
				"SpawnFrequencies")

			.addEntry("bear", 21)

			.addEntry("bear_polar", 21)
			.addEntry("bird", 52)
			.addEntry("bunny", 63)
			.addEntry("boar", 31)
			.addEntry("deer", 63)
			.addEntry("duck", 63)
			.addEntry("fox", 42)
			.addEntry("horse", 52)

			.addEntry("unicorn", 1)
			.addEntry("pegasus", 1)
			.addEntry("kitty", 42)
			.addEntry("bigcat", 31)
			.addEntry("mouse", 63)

			.addEntry("rat", 10)
			.addEntry("rat_hell", 10)
			.addEntry("ogre", 8)
			.addEntry("ogre_fire", 3)
			.addEntry("ogre_cave", 4)
			.addEntry("werewolf", 8)
			.addEntry("werewolf_wolf", 10)
			.addEntry("wraith", 6)
			.addEntry("wraith_flame", 3)

			.addEntry("dolphin", 2)
			.addEntry("shark", 2)
			.addEntry("fishy", 10)

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

			.addEntry("ogreStrength",
				"How much a green ogre can chew through. Original range 0.1 to 5.0; 2.5 leaves stone "
					+ "breakable and obsidian and bedrock safe. OgreStrength.",
				2.5D)
			.addEntry("fireOgreStrength", "FireOgreStrength.", 2.0D)
			.addEntry("caveOgreStrength", "CaveOgreStrength.", 3.0D)
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

		TOML.addCategory(
				"Original panel: \"Other Settings\".",
				"Breeding")
			.addEntry("easyHorseBreeding",
				"Whether horse breeding is made easy: a rare foal comes up every time its parents' "
					+ "combination allows one rather than one time in three, and neither parent is "
					+ "left sterile afterwards. EasyHorseBreeding.",
				false);

		TOML.addCategory("Replacements")
			.addEntry("replaceVanillaDeer",
				"Swap BTA's built-in deer out of every biome's spawn list for the Mo' Creatures deer. " +
					"Set to false to keep the vanilla deer and leave ours unspawned.",
				true);

		cfg = new TomlConfigHandler(MOD_ID, TOML);
		fillInSettingsAddedSinceTheFileWasWritten();
		cache();
	}

	private static void fillInSettingsAddedSinceTheFileWasWritten() {
		cfg.getRawParsed().addMissing(TOML);
		cfg.writeConfig();
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

		ogreStrength = (float) cfg.getDouble("HostileMobs.ogreStrength");
		fireOgreStrength = (float) cfg.getDouble("HostileMobs.fireOgreStrength");
		caveOgreStrength = (float) cfg.getDouble("HostileMobs.caveOgreStrength");
		ogreRange = cfg.getInt("HostileMobs.ogreRange");

		easyHorseBreeding = cfg.getBoolean("Breeding.easyHorseBreeding");

		dolphinsAttackSharks = cfg.getBoolean("WaterMobs.dolphinsAttackSharks");
		spawnPiranhas = cfg.getBoolean("WaterMobs.spawnPiranhas");
	}

	public static int frequency(String entity) {
		return cfg.getInt("SpawnFrequencies." + entity);
	}

	public static float blastCeiling(float strength) {
		return Math.max(0.0F, strength) * (60.0F / 2.5F);
	}

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

	public static boolean spawnsAt(Difficulty world, Difficulty minimum) {
		return world.canHostileMobsSpawn() && world.id() >= minimum.id();
	}
}
