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
| **Status** | Playable — 4 mobs. Actively incomplete, see [PORTING.md](PORTING.md) |

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

**Working:** Bear (grizzly + polar), Bird, Bunny, Fox (red + arctic).

**Not yet ported:** Horse / Unicorn / Pegasus, Kitty, Boar, Duck are converted but not yet compiling —
parked in `wip/entity-7.2-port/`. The litterbox and bee hive blocks are unported. The classic mobs Team
Port never reached (big cats, dolphin, shark, ogre, werewolf, wraith, scorpion, snake, turtle, rat,
crocodile, elephant, wyvern) are absent, though some of their original sounds already ship in the assets.

Contributions welcome — [PORTING.md](PORTING.md) lists the exact remaining errors per file.

## Licence

Port code is **CC0-1.0**, inherited from Team Port. Mo' Creatures itself is DrZhark's work; this is a
reimplementation against BTA's API, not a redistribution of the original mod.

<!-- keywords: mo creatures, mocreatures, BTA, Better than Adventure, babric, fabric, minecraft beta 1.7.3,
b1.7.3, halplibe, bta mod, bta 8.0.1, DrZhark, Team Port, mob mod, animals -->
