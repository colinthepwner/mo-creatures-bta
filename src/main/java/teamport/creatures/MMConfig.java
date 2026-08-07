package teamport.creatures;

import turniplabs.halplibe.util.TomlConfigHandler;
import turniplabs.halplibe.util.toml.Toml;

import static teamport.creatures.MoreMobs.MOD_ID;

public class MMConfig {
	private static final Toml TOML = new Toml("Mo' Creatures TOML Config");
	public static TomlConfigHandler cfg;

	static {
		TOML.addCategory("IDs")
			.addEntry("startingItemID", 17000)
			.addEntry("startingBlockID", 2800)
			.addEntry("startingEntityID", 100);

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
		TOML.addCategory("SpawnFrequencies")
			// Passive: original x10.45.
			.addEntry("bear", 21)            // FreqBear 2
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
			.addEntry("fishy", 10);          // FreqFishy 10

		TOML.addCategory("Replacements")
			.addEntry("replaceVanillaDeer",
				"Swap BTA's built-in deer out of every biome's spawn list for the Mo' Creatures deer. " +
					"Set to false to keep the vanilla deer and leave ours unspawned.",
				true);

		cfg = new TomlConfigHandler(MOD_ID, TOML);
	}
}
