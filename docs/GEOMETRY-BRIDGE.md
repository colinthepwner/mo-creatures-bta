# Geometry bridge — design

Extends the texture asset bridge to **models**. Goal: the player drops their own copy of the original
Mo' Creatures into `mods/` and everything works. No tool to run, no pack to enable, no manual step.
The original never loads as a mod — it is b1.7.3 code and BTA ignores it — it is only a data source.

Status: **designed, not implemented.** `MMAssetBridge` already does the texture half.

## Why this instead of shipping models

Bedrock geometry converted from the original *is* DrZhark's model expressed as numbers, so it can no
more be committed here than his textures can. Shipping the **converter** rather than its output keeps
the repo to code while each user generates assets from a copy they already own.

This is also forced by UVs: the original textures are painted against the original box layout, so
hand-authored geometry would map them as garbage. Geometry and textures have to come from the same
source to look right. See the Known gaps section of the README.

## Feasibility

The two formats hold the same data. A Java `ModelBase` is a tree of `ModelRenderer` boxes, each with a
rotation point, a box offset, a size and a texture offset; Bedrock geometry is bones with `pivot` and
cubes with `origin`, `size` and `uv`.

`org.ow2.asm:asm` **9.9** (`asm-tree`, `asm-commons`, `asm-util`) is already on the runtime classpath
via fabric-loader and Mixin — verified with `gradlew dependencies --configuration runtimeClasspath`.
No new dependency is needed.

## Extraction

Model classes in the original are compiled from the mod's own source, so field names survive (only
Minecraft's own classes were obfuscated in that era). For each candidate class:

1. Walk `<init>` with `asm-tree`.
2. Track constant arguments to `setTextureOffset(int,int)`, `addBox(float,float,float,int,int,int)`
   and `setRotationPoint(float,float,float)`.
3. Attribute each to the `PUTFIELD` target it is assigned to — the field name becomes the bone name.

Because these are constant arguments in a linear constructor, this is mechanical. Nothing is executed;
the classes are never loaded, only read. That matters — they extend b1.7.3 types that do not exist in
BTA, so they could not be instantiated even if we wanted to.

## Transform

Java model space hangs downward from a 24-unit origin; Bedrock is Y-up. The conversion negates X and Y
and applies the 24-unit vertical offset, with the box offset folded into the pivot to produce
`origin`. `setTextureOffset` maps directly onto `uv`.

**Do not hardcode these constants from memory.** Derive them by round-tripping one known model — the
repo already contains `bear.json` and `deer.json` alongside mobs whose original Java models are in the
archive, so the transform can be validated against a known-good pair before trusting it on the rest.

## Bone naming

The renderers here expect specific names (`head`, `body`, `legFrontLeft`, …) and the originals use
their own (`leg1`, `bearHead`, …). Needs a per-mob alias table alongside
`assets/creatures/asset-bridge.properties`, in the same spirit: data, not code, so adding a mob stays
a one-line edit.

## Output

Write into the same generated `MoCreaturesAssets` pack the textures already go to, at the paths
`setModel(...)` requests. Two open items:

- **Auto-enable the pack.** Currently the player has to enable it in Options. `TexturePackList` has
  `setTexturePack` / `updateAvailableTexturePacks` / `refresh`; wiring that up removes the last manual
  step and is required by the "just drop it in" goal.
- **Cache.** Extraction should run once and be skipped when the generated pack is newer than the
  source archive, so startup is not paying for it every launch.

## Before implementing

This needs verifying against a **real archive** rather than assumed bytecode shape. Specifically: that
field names are intact, that models are one-class-per-mob, and that the constructors are flat rather
than looping over arrays to build boxes (some model code does, and that path needs handling or
skipping). Build the extractor against an actual copy; do not guess.

## Never

Do not commit extracted geometry, extracted textures, or the source archive itself. The output belongs
in the player's generated pack only. `.gitignore` should cover the generated pack if it ever lands
inside the project directory.
