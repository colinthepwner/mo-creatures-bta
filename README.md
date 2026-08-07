# Mo' Creatures for BTA 8.0.1 (Babric)

A continuation of the **Mo' Creatures** port for **Better than Adventure!** — updated from BTA 7.2/7.3 to
**BTA 8.0.1**, built on Babric.

> **Looking for the version that works on BTA 8.0.1?** This is it. The upstream Team Port repo is archived
> at BTA 7.2 and will not run on 8.0.

| | |
|---|---|
| **Minecraft / BTA** | Better than Adventure! `8.0.1` (`release` channel) |
| **Mod loader** | Babric / fabric-loader `0.18.4-bta.11` |
| **Requires** | HalpLibe `6.1.3+8.0` — bundled, no separate download needed |
| **Java** | 17 |
| **Status** | Playable — 27 mobs, full original roster |

## Lineage

This version exists because the original port stopped before BTA 8.0:

1. **[DrZhark](https://www.curseforge.com/minecraft/mc-mods/mo-creatures)** — original Mo' Creatures for
   Minecraft Beta 1.7.3 and later.
2. **[bta-team-port/mo-creatures](https://github.com/bta-team-port/mo-creatures)** — Team Port's BTA port.
   Archived; `7.2` branch is the last complete state, `7.3` is an unfinished rewrite.
3. **This repo** — picks up Team Port's unfinished `7.3` rewrite and carries it to BTA 8.0.1.

BTA 8.0 removed the entire `ModelBase` model system and replaced HalpLibe's entrypoints with an event
system, so this is a genuine API migration rather than a version bump. The full breaking-change list is in
[PORTING.md](PORTING.md) — useful if you're porting another 7.x BTA mod to 8.0, since most mods hit the
same walls.

## Install

1. Install [Better than Adventure!](https://betterthanadventure.net/) `8.0.1` with Babric.
2. Drop `creatures-<version>+8.0.1.jar` into `mods/`.
3. **Drop your own copy of the original Mo' Creatures somewhere under your game directory.** That is
   the whole step — see below.

[HalpLibe](https://github.com/Turnip-Labs/bta-halplibe/releases) `6.1.3+8.0` rides along inside the
jar, so there is nothing to install for it. If you already have HalpLibe in `mods/`, the loader keeps
whichever version is newer — a copy you installed yourself is never downgraded by this one.

Mods of that era shipped as zips, and the original's own download nests the payload one level down in
`mods/MoCreatures.zip` — the bridge descends into that by itself, so hand the whole zip over as-is.
Tested against **v2.12.2 for Beta 1.7.3**, mirrored at
[mcarchive.net](https://b2.mcarchive.net/file/mcarchive/77cac72cd428ea428eaa0a649ce8f901f7319ded7c5a88d04347e3fdf6343ec8/DrZharks_MoCreatures_Mod_v2.12.2_1.zip)
(a third-party preservation archive, not affiliated with this project or with DrZhark).

## Build

```
./gradlew build
```

Output lands in `build/libs/`. JDK 17 is required; Gradle will fetch the toolchain automatically.

## What's in it

The full original roster — **27 mobs**:

- **Animals** — Bear (+ polar), Bird, Bunny, Fox (+ arctic), Boar, Duck, Deer, Mouse
- **Horses** — Horse, Unicorn, Pegasus (tameable, rideable)
- **Cats** — Kitty (+ Litterbox block), Big Cat in 7 species: lioness, lion, panther, cheetah, tiger, snow leopard, white tiger
- **Aquatic** — Dolphin (tameable, rideable, breedable), Shark, Fishy, plus shark and fishy eggs
- **Hostile** — Ogre, Fire Ogre, Cave Ogre, Werewolf (transforms at night), Wolf, Wraith, Flame Wraith, Rat, Hell Rat

**The Deer replaces BTA's built-in deer.** BTA's own deer is not a vanilla Minecraft mob, so this swaps
it for the Mo' Creatures one. Set `Replacements.replaceVanillaDeer = false` in `config/creatures.cfg`
to keep BTA's instead.

Ogres break blocks to reach you, as they did originally — bounded here: it respects the
`doMobGriefing` game rule, breaks at most 6 blocks per swing on a cooldown, never digs beneath itself,
and spares bedrock, chests, furnaces, liquids and anything tough enough to survive TNT. How much it
can chew through is `HostileMobs.ogreStrength`, below.

## Settings

Everything lives in **`config/creatures.cfg`**, written on first launch and topped up on every launch
after it, so a file from an earlier version gains whatever has been added since without losing your
edits. The original edited the same knobs through a GUI API screen under *Options / Mods Settings*;
BTA 8.0 has no equivalent panel for a mod to hang itself off, so this is a file. The six categories
are the original's six panels, and every entry names the `mocreatures.cfg` key it came from, so an old
config can be transcribed by hand.

**`[SpawnLimits]`** — mobs per 256 eligible chunks. These are *global* caps: BTA counts every mob in
the category, so the animal cap covers cows and sheep too. That is what the original did and why it
had to — this mod roughly doubles the passive spawn pool, and under BTA's stock cap the new animals
would displace the vanilla ones rather than add to them. Put back BTA's own `70 / 15 / 5` for stock
behaviour.

| key | default | original |
|---|---|---|
| `maxHostiles` | 70 | `MobsSpawnLimit` |
| `maxAnimals` | 30 | `AnimalsSpawnLimit` |
| `maxWaterMobs` | 25 | `WaterMobSpawnLimit` |

**`[SpawnFrequencies]`** — relative spawn weight per mob, on BTA's scale rather than the original's
0–10 one (the conversion, and why passive and hostile mobs need different factors, is commented in
`MMConfig.java`). **Set any entry to 0 to stop that mob spawning naturally**, which is how the
original's own activate/deactivate worked. `bear_polar` is the bear's weight in glacier and tundra —
the original's `FreqPBear`, which has no separate entity here — and `squid` is BTA's own squid,
exposed because the water cap above went from 5 to 25 and squid would otherwise take the headroom.

**`[Hunters]`** — what the mod's predators are willing to hunt.

| key | default | original |
|---|---|---|
| `attackHorses` | `false` | `HuntersAttackHorses` |
| `attackWolves` | `false` | `HuntersAttackWolves` |
| `destroyDrops` | `true` | `HuntersDestroyDrops` |

**`[HostileMobs]`** — difficulties are the *lowest* setting the mob spawns on and mean that or
harder. Strengths scale how much an ogre can break through; the original's number sized a literal
explosion, which this port does not have, so it scales the blast-resistance ceiling instead, pinned so
the default 2.5 reproduces the bound the port already used.

| key | default | original |
|---|---|---|
| `ogreSpawnDifficulty` | `normal` | `ogreSpawnDifficulty` |
| `fireOgreSpawnDifficulty` | `hard` | `FireOgreSpawnDifficulty` |
| `caveOgreSpawnDifficulty` | `normal` | `CaveOgreSpawnDifficulty` |
| `werewolfSpawnDifficulty` | `normal` | `wereSpawnDifficulty` |
| `wraithSpawnDifficulty` | `normal` | `wraithSpawnDifficulty` |
| `flameWraithSpawnDifficulty` | `hard` | `flameWraithSpawnDifficulty` |
| `ogreStrength` | `2.5` | `OgreStrength` |
| `fireOgreStrength` | `2.0` | `FireOgreStrength` |
| `caveOgreStrength` | `3.0` | `CaveOgreStrength` |
| `ogreRange` | `12` | `OgreRange` |

**`[WaterMobs]`**

| key | default | original |
|---|---|---|
| `sharkSpawnDifficulty` | `easy` | `sharkSpawnDifficulty` |
| `dolphinsAttackSharks` | `true` | `DolphinsAttackSharks` |
| `spawnPiranhas` | `true` | `SpawnPiranhas` |

**`[Replacements]`** — `replaceVanillaDeer`, above. **`[IDs]`** — item, block and entity id bases.

### Three defaults change behaviour

Adopting the original's own defaults moves three things off where this port had them. Each is one line
in the config to put back.

- **Animal and water spawn caps rise** from BTA's 15 and 5 to 30 and 25, as described above.
- **Ogres notice you at 12 blocks, not 24.** 24 is the original's slider *maximum*; its default is 12.
  The readme line this port took 24 from ("they can smell players 24 blocks apart") predates the
  setting existing at all.
- **Fire ogres spawn on Hard, not Normal.** Same story: "Fire ogres now spawn on Normal difficulty" is
  a 2.x changelog line, but v2.12.2 ships the setting at Hard.

### Original settings not carried over

Five have nothing in this port to attach to, so they are absent rather than present and inert:
`EasyHorseBreeding` (horses do not breed here), `DisplayPetNames` / `DisplayPetHealth` /
`DisplayPetEmotions` (no pet naming or nameplates), and `StaticKBeds` (no kitty bed). `StaticLitter` is
likewise gone because the litter box is a block here and so is always static. `PegasusSpawningP` is
superseded by `SpawnFrequencies.pegasus`: the original needed a percentage because pegasi were a
variant roll on one horse entity, and this port gives them their own entity and their own weight.

## Textures and models — the asset bridge

This mod **ships no art or models from the original Mo' Creatures**. That is DrZhark's work and its
licence does not allow redistribution, so none of it can live in this repository.

Instead: **drop your own copy of the original anywhere under your game directory.** That is the only
step, and it really is *anywhere* — the search does not care what the file is called or where it sits:

- **Any folder**, not just `mods/`, to six levels deep.
- **Any name.** After fifteen years of being passed around the original turns up as
  `DrZharks_MoCreatures_Mod_v2.12.2_1.zip`, as the `MoCreatures.zip` nested inside that, as a `.jar`,
  or renamed to something meaningless. A container is recognised by its zip header, so the extension
  is irrelevant — `.zip`, `.jar`, `.disabled`, `.bin`, none at all, all the same.
- **Unpacked works too.** If you extracted it, the loose `.png` and `.class` files are picked up
  individually, so a folder is as good as an archive.
- **Nested archives are followed**, three levels deep, which is what handles the original's own
  download without you having to open it.

The name still counts for one thing: it decides what gets looked at first, so an obviously named
archive wins over an unrelated mod jar that happens to contain a `bear.png`. More than one source can
contribute, first match per file, so a partial copy gets topped up rather than rejected.

It will not load as a mod — it is Beta 1.7.3 code and BTA ignores it — it is read purely as a data
source. On startup the mod extracts the textures, converts the Java entity models to Bedrock geometry,
writes a `MoCreaturesAssets` texture pack and enables it automatically. What it was built from is
recorded in the pack, so later launches skip the search entirely; delete
`texturepacks/MoCreaturesAssets` to force a fresh look.

Measured against DrZhark's v2.12.2 for b1.7.3: **74 textures and 31 models** are converted, with
nothing left unconverted. Nothing is downloaded and nothing is redistributed — the file has to already
be on your disk.

Why models and not just skins: the original art is painted against the original box layout, and *no*
model in this repo shares a UV layout with the original it stands in for. Bridging a skin without its
geometry puts the art on boxes it was never laid out for, so the two travel together or not at all.

**Ten of the 27 mobs ship geometry in this repository; the other 17 do not.** Without the archive
those 17 have no model to draw at all, so they do not render — this is the one real install-time
expectation, and it is worth knowing before you decide the mod is broken. The ten that do ship
geometry render normally, falling back to the missing-texture checker for any skin the bridge would
have supplied. The startup audit prints exactly what resolved, mob by mob.

## Summoning

```
/summon creatures:bear
```

The 27 ids are: `bear` `bird` `fox` `bunny` `boar` `duck` `horse` `horse_unicorn` `horse_pegasus`
`kitty` `deer` `bigcat` `rat` `rat_hell` `mouse` `dolphin` `shark` `fishy` `shark_egg` `fishy_egg`
`ogre` `ogre_fire` `ogre_cave` `werewolf` `werewolf_wolf` `wraith` `wraith_flame`.

**Variants are not separate ids.** Polar bear and arctic fox are states of `creatures:bear` and
`creatures:fox`, and all seven big cats — lioness, lion, panther, cheetah, tiger, snow leopard, white
tiger — are `creatures:bigcat` with a skin variant, as are the six dolphins, six birds, ten fish and
three rats. So `/summon creatures:polarbear` is correctly rejected; summon the base mob instead. The
full list is also printed to the log at startup under `Creatures summon ids:`.

## Known gaps

Every model the archive can supply now converts — 31 of them, nothing skipped. What remains:

- **Fox and bunny use this repo's own art by design.** v2.12.2 ships one fox skin and no arctic fox at
  all, and its bunny has 4 skins where this port declares 5. The built-in sets are complete and
  self-consistent, so bridging a partial one would be a downgrade rather than an improvement.
- **Three horse types are unmapped** — pack, nightmare and black pegasus. The archive has the sheets,
  but this port has no mob or state for them yet. Saddled horse textures are likewise unmapped.
- **Six mobs fall back to vanilla sounds** — shark, fishy and both eggs (no fish audio exists in the
  repo *or* the original), bird hurt/death, and the werewolf's human idle. Everything else uses the
  mod's own audio: 64 sound events, all 27 mobs have hurt and death, 24 have an idle call.

## Licence

Port code is **CC0-1.0**, inherited from Team Port. Mo' Creatures itself is DrZhark's work; this is a
reimplementation against BTA's API, not a redistribution of the original mod.

<!-- keywords: mo creatures, mocreatures, BTA, Better than Adventure, babric, fabric, minecraft beta 1.7.3,
b1.7.3, halplibe, bta mod, bta 8.0.1, DrZhark, Team Port, mob mod, animals -->
