# Mo' Creatures for BTA 8.0.1 (Babric)

A continuation of the **Mo' Creatures** port for **Better than Adventure!** — updated from BTA 7.2/7.3 to
**BTA 8.0.1**, built on Babric.

> **Looking for the version that works on BTA 8.0.1?** This is it. The upstream Team Port repo is archived
> at BTA 7.2 and will not run on 8.0.

| | |
|---|---|
| **Minecraft / BTA** | Better than Adventure! `8.0.1` (`release` channel) |
| **Mod loader** | Babric / fabric-loader `0.18.4-bta.11` |
| **Requires** | HalpLibe `6.1.3+8.0` |
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
2. Drop [HalpLibe](https://github.com/Turnip-Labs/bta-halplibe/releases) `6.1.3+8.0` into `mods/`.
3. Drop `creatures-<version>+8.0.1.jar` into `mods/`.

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
and spares bedrock, chests, furnaces, liquids and anything stone-hardness or above.

## Textures and models — the asset bridge

This mod **ships no art or models from the original Mo' Creatures**. That is DrZhark's work and its
licence does not allow redistribution, so none of it can live in this repository.

Instead: **drop your own copy of the original into `mods/`.** That is the only step. It will not load
as a mod — it is Beta 1.7.3 code and BTA ignores it — it is read purely as a data source. On startup
the mod extracts the textures, converts the Java entity models to Bedrock geometry, writes a
`MoCreaturesAssets` texture pack and enables it automatically.

Measured against DrZhark's v2.12.2 for b1.7.3: **61 textures and 20 models** are converted, 17 of them
for mobs this repo ships no geometry for at all. Nothing is downloaded and nothing is redistributed —
the file has to already be on your disk.

Why models and not just skins: the original art is painted against the original box layout, and *no*
model in this repo shares a UV layout with the original it stands in for. Bridging a skin without its
geometry puts the art on boxes it was never laid out for, so the two travel together or not at all.

Without the archive, mobs that have built-in art render normally and the rest fall back to the
missing-texture checker. The startup audit prints exactly what resolved.

## Known gaps

Seven models are held back, each a mismatch between this port and v2.12.2 rather than a converter
limitation — see [docs/GEOMETRY-BRIDGE.md](docs/GEOMETRY-BRIDGE.md):

- **Fox** — v2.12.2 ships one fox skin and **no arctic fox at all**
- **Bunny** — archive has 4 skins, this port declares 5
- **Horse / Unicorn / Pegasus** — the original splits a horse across two models and two textures
  (body, then head/neck/horn/wings); this port draws one combined model
- **Boar / Duck** — the original rendered these on Minecraft's own pig and chicken models, so there is
  no class in the archive to convert
- **Wolf** — one geometry here is shared by the werewolf's beast form and the pack wolf
- **Litterbox** — this port wants clean and filthy states; the original swapped a box on one image

Several mobs are also silent: sound events are registered but the audio sits flat in the assets folder
rather than under `sounds/mob/<name>/`.

## Licence

Port code is **CC0-1.0**, inherited from Team Port. Mo' Creatures itself is DrZhark's work; this is a
reimplementation against BTA's API, not a redistribution of the original mod.

<!-- keywords: mo creatures, mocreatures, BTA, Better than Adventure, babric, fabric, minecraft beta 1.7.3,
b1.7.3, halplibe, bta mod, bta 8.0.1, DrZhark, Team Port, mob mod, animals -->
