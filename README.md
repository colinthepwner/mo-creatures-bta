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
| **Status** | Playable — 11 mobs. Some art still missing, see below |

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

**11 mobs:** Bear (grizzly + polar), Bird, Bunny, Fox (red + arctic), Boar, Duck, Horse, Unicorn,
Pegasus, Kitty, Deer — plus the Litterbox block.

Polar bear and arctic fox are variant flags on the base mob rather than separate entities.

**The Deer replaces BTA's built-in deer.** BTA's own deer is not a vanilla Minecraft mob, so this
swaps it for the Mo' Creatures one: the vanilla spawn entry is removed at biome construction and
ours takes its place. Set `Replacements.replaceVanillaDeer = false` in `config/creatures.cfg` to keep
BTA's instead.

## Textures — the asset bridge

This mod **ships no art from the original Mo' Creatures**. That mod is DrZhark's and its licence does
not allow redistribution, so the textures cannot live in this repository.

Instead, if you own a copy of the original, drop the jar or zip into your game directory as
`mocreatures-assets.zip` (or leave it in `mods/`). On startup the mod reads the images out of *your*
copy and writes a `MoCreaturesAssets` texture pack, which you then enable in Options. Nothing is
downloaded and nothing is redistributed — the file has to already be on your disk. Mapping lives in
`assets/creatures/asset-bridge.properties`, so adding a mob is a one-line edit.

Without it, mobs that have textures in this repo look correct and the rest fall back to the
missing-texture checker. The startup audit prints exactly which are missing.

## Known gaps

Boar, Duck, Unicorn and Pegasus have **no textures** in this repo, and Duck has no model (it reuses
the bird geometry). The asset bridge covers them if you supply the original mod. Several mobs are
also silent — sound events are registered but the ogg files were never part of the port.

Mobs the original had that are still absent: big cats, dolphin, shark, ogre, werewolf, wraith, ghost,
scorpion, snake, turtle, rat, crocodile, elephant/mammoth, wyvern. Their logic can be reimplemented,
but none of them has geometry or textures here.

## Licence

Port code is **CC0-1.0**, inherited from Team Port. Mo' Creatures itself is DrZhark's work; this is a
reimplementation against BTA's API, not a redistribution of the original mod.

<!-- keywords: mo creatures, mocreatures, BTA, Better than Adventure, babric, fabric, minecraft beta 1.7.3,
b1.7.3, halplibe, bta mod, bta 8.0.1, DrZhark, Team Port, mob mod, animals -->
