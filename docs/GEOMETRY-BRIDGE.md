# Geometry bridge — design

Extends the texture asset bridge to **models**. Goal: the player drops their own copy of the original
Mo' Creatures into `mods/` and everything works. No tool to run, no pack to enable, no manual step.
The original never loads as a mod — it is b1.7.3 code and BTA ignores it — it is only a data source.

Status: **implemented** in `MMGeometryBridge`, driven by `assets/creatures/model-bridge.properties`.
Measured against DrZhark's v2.12.2 for b1.7.3: all 27 model classes are read without a single
extraction failure, and **20 of them are converted and shipped**. The seven that are not are listed at
the end of this file, and none of them is blocked by the converter.

## Why this instead of shipping models

Bedrock geometry converted from the original *is* DrZhark's model expressed as numbers, so it can no
more be committed here than his textures can. Shipping the **converter** rather than its output keeps
the repo to code while each user generates assets from a copy they already own.

This is also forced by UVs: the original textures are painted against the original box layout, so
hand-authored geometry would map them as garbage. Geometry and textures have to come from the same
source to look right. See the Known gaps section of the README.

That last point turned out to be stronger than a caution. Every model this repo ships was compared
against every model class in the archive, cube by cube, on texture offset and box size. **The overlap
is zero** — not one of them, bear and deer included, shares a UV layout with the original it stands
in for. They are independent reimplementations that happen to draw the same animal. So bridging a
skin without also converting the geometry is not a partial win; it puts the original's art on boxes
that were never laid out for it. This is why `model-bridge.properties` lists a mob's textures
alongside its geometry and ships the pair or neither, and why `asset-bridge.properties` now carries
only skins whose geometry travels with them.

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
their own (`leg1`, `Head`, `bipedTail`, …). The per-mob alias table lives in
`assets/creatures/model-bridge.properties`, in the same spirit as the texture manifest: data, not
code, so adding a mob stays an edit rather than a commit to a class.

Sides are taken from the rotation point, not from the original's field names, which are not
consistent: `ModelRat` calls the leg at `x = +3` `FrontL` while `ModelBird` calls the one at `x = -2`
`leftleg`. The base layer this bridge reconstructs puts left at `-x` and front at `-z`, so that is the
convention the aliases follow.

A mob whose original model extends one of Minecraft's own gets that base layer rebuilt from the
Bedrock geometry BTA already ships (`quadruped`, `biped`) and its own boxes composed on top; a
subclass that writes into a base field slot is replacing that bone rather than adding one. Bones the
original builds but never draws are dropped with `= -`, and head furniture is reattached with
`name@parent` so posing the head carries it along.

## Output

Geometry is written into the same generated `MoCreaturesAssets` pack the textures go to, at the paths
`setModel(...)` requests, so a converted model replaces the built-in one with no code change. Both
former open items are done: the pack is selected automatically through `TexturePackList`, and a
`bridge-source.txt` stamp skips the whole extraction when the pack already matches the archive it was
built from. Bump `BRIDGE_REVISION` in `MMAssetBridge` whenever a manifest changes shape, or existing
packs will keep their stale output.

## What the archive actually turned out to be

Checked against a real copy rather than assumed, because several assumptions were wrong:

- **The payload is nested.** The original's download is a zip whose entire content is one
  `mods/MoCreatures.zip`. Reading only the outer layer finds no classes and no images at all, so the
  archive walk descends into zips it finds (`MAX_NESTING`).
- **Field names survive**, as hoped — only Minecraft's own classes were obfuscated in that era. All 27
  constructors are flat and readable; none loops over an array to build its boxes.
- **Models are not one-per-mob.** Several mobs are two classes: `ModelBear2` + `ModelBear1`,
  `ModelBigCat2` + `ModelBigCat1`, `ModelHorse2` + `ModelHorse1`, `ModelOgre2` + `ModelOgre1`. Some of
  those pairs are one shape drawn twice with *different* textures (the bear's angry pass, the lion's
  mane) and must stay separate; the manifest's `overlay` key is for the other kind.
- **Two mobs have no model in the archive at all.** The original rendered its boar on Minecraft's
  `ModelPig` and its duck on `ModelChicken`.
- **UV space is not the image size.** The art is double resolution, so a 128x64 PNG is usually still a
  64x32 layout. It is derived from the offsets themselves — the largest `u + 2*(depth+width)` and
  `v + depth + height` over every cube — and only `ModelKitty` needed an override (`128, 64`).
- **Not every box is meant to be seen.** `ModelWraith` leaves a 1x1x1 stub in the biped head slot at
  `v=40`, off the end of its own 64x32 texture, and keeps the real head in the headwear slot. The
  manifest drops the stub and promotes the real box, which also brings the model back inside 64x32.

## What converts, and what does not

20 of the 27 model classes are converted: `bear`, `deer`, `bird`, `kitty`, `bigcat`, `bigcat_maned`,
`rat`, `rat_hell`, `mouse`, `dolphin`, `shark`, `shark_egg`, `fishy`, `fishy_egg`, `ogre`,
`ogre_fire`, `ogre_cave`, `wraith`, `wraith_flame` and `werewolf`. Seventeen of those are mobs this
repo ships **no** geometry for at all, so before the bridge runs they do not render.

Nothing on the held-back list below is a converter limitation — each one is a mismatch between what
v2.12.2 contains and what this port asks for, and `model-bridge.properties` carries the same list with
the detail:

| Held back | Why |
|---|---|
| `fox` | v2.12.2 has `fox.png` only. This port also draws angry, arctic and arctic-angry; there is no arctic fox in this version at all. |
| `bunny` | Archive has 4 skins, this port's `variants.json` declares 5. |
| `horse`, `horse_unicorn`, `horse_pegasus` | The original splits a horse across two models and two textures — `horse<colour>a.png` for body/tail/legs and `horse<colour>b.png` for head/neck/horn/wings. This port draws one model with one combined skin, which neither half fits. |
| `boar`, `duck` | The original used Minecraft's own `ModelPig` and `ModelChicken`; there is no class in the archive to convert. |
| `werewolf_wolf` | This port draws the transformed werewolf and the pack wolf with one geometry; the original had two, with a texture painted for each. |
| `litterbox` | This port wants a clean and a filthy skin; the original had one image and swapped between a `Litter` and a `LitterUsed` box on it. |

Each becomes a one-line edit the moment its blocker moves — a fifth bunny skin, or a horse renderer
that draws two layers.

## Never

Do not commit extracted geometry, extracted textures, or the source archive itself. The output belongs
in the player's generated pack only. `.gitignore` should cover the generated pack if it ever lands
inside the project directory.
