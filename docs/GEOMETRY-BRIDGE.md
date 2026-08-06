# Geometry bridge — design

Extends the texture asset bridge to **models**. Goal: the player drops their own copy of the original
Mo' Creatures into `mods/` and everything works. No tool to run, no pack to enable, no manual step.
The original never loads as a mod — it is b1.7.3 code and BTA ignores it — it is only a data source.

Status: **implemented** in `MMGeometryBridge`, driven by `assets/creatures/model-bridge.properties`.
Measured against DrZhark's v2.12.2 for b1.7.3: all 27 model classes are read without a single
extraction failure, and **34 geometries are written**, from 21 of those classes plus two of
Minecraft's own. Nothing this port draws is held back for want of a source; the two mobs it does not
bridge are named at the end of this file and both are deliberate.

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

Reattaching is not free, and the converter pays for it rather than the manifest. The originals have
no hierarchy at all — every `ModelRenderer` is drawn about its own rotation point — while Dragonfly
composes a bone's transform on top of its parent's. Hang a bone off a parent that has a rest rotation
and it picks that rotation up twice and swings about the wrong pivot. So `MMGeometryBridge` divides
the parent's rest transform back out: the child's pivot moves into the parent's unrotated frame and
the parent's angles come off its own, which carries the cubes with it because a cube's origin is
derived from the pivot. The rest pose is then bit-identical to the original's and the bone still
follows its parent when posed. This is why the horse's ears and horn can hang off `head` at all, and
it fixed a rat whose front torso was being drawn at 90 degrees to the rest of it.

A model's constructor is not the whole shape, either. Minecraft's own models routinely build a box in
one place and move it somewhere else in `setRotationAngles`, every frame, before drawing it —
`ModelQuadruped` lays its body along the spine that way, and `ModelBiped` re-hangs its legs from
`rotationPointY = 12`. So the pose methods are read too, with every animation argument at zero: that
is what "rest pose" means, and it makes the constant beside an animation term readable rather than
unknown. A rotation the constructor already declared wins, because angles accumulate across a frame;
a rotation point does not, because it is assigned outright immediately before the box is drawn, so
there the pose method's value is the one the original renders with. This is what the original's
`ModelOgre2` needs — it builds its feet about `y = 0` and moves them to `y = 12` when it poses, so a
converter that reads only constructors hangs an ogre's feet at knee height — and it is most of what
the transformed werewolf needs, whose `ModelWerewolf` re-places its head, torso, arms, shins and tail
on every frame.

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
  `ModelBigCat2` + `ModelBigCat1`, `ModelHorse2` + `ModelHorse1`, `ModelOgre2` + `ModelOgre1`,
  `ModelWolf2` + `ModelWolf1`. None of them turned out to be one shape drawn twice, which is what the
  manifest's `overlay` key is for. They are either two halves of one animal, drawn as two layers with
  a texture each (the horse, the ogres and the pack wolf), or a state pass this port has no state for
  (the bear's angry coat) — and the lion's mane, which is a layer this port does draw.
  Telling the two apart matters: `ModelOgre2` alone is a headless ogre, because its head slot holds a
  1x1x1 dot and the real head, torso and shins are all in `ModelOgre1` on `ogreb.png`; and `ModelWolf2`
  alone is a wolf with a floating head, because its head sits two units clear of the front of its body
  and what closes that gap is `ModelWolf1`'s neck plate on `wolfb.png`. Both of those passes read
  `pass == 0 && !flag`, so the flag *suppresses* the pass rather than enabling it, and nothing but a
  saved NBT tag ever sets it — which is why both draw always and unconditionally here.
- **Two mobs have no model in the archive at all.** The original rendered its boar on Minecraft's
  `ModelPig` and its duck on `ModelChicken`. That is not a dead end — the converter already
  reconstructs vanilla base layers from BTA's own geometry, so it reconstructs the whole model for
  these two (`<id>.vanilla`), which is exactly the layout `boar.png` and `duck.png` are painted for.
  `pig.geo.json` and `chicken.geo.json` are the authority rather than Beta 1.7.3's classes, because
  BTA moved the chicken's wing pivots and mirrors the pig's right-hand legs, and what has to match is
  what BTA actually draws.
- **UV space is not the image size.** The art is double resolution, so a 128x64 PNG is usually still a
  64x32 layout. It is derived from the offsets themselves — the largest `u + 2*(depth+width)` and
  `v + depth + height` over every cube — and only `ModelKitty` needed an override (`128, 64`).
- **Not every box is meant to be seen.** `ModelWraith` leaves a 1x1x1 stub in the biped head slot at
  `v=40`, off the end of its own 64x32 texture, and keeps the real head in the headwear slot. The
  manifest drops the stub and promotes the real box, which also brings the model back inside 64x32.

## What converts, and what does not

**34 geometries and 77 textures**, measured against a real v2.12.2 archive with nothing missing and
nothing skipped: `bear`, `deer`, `bird`, `kitty`, `bigcat`, `bigcat_maned`, `rat`, `rat_hell`,
`mouse`, `dolphin`, `shark`, `shark_egg`, `fishy`, `fishy_egg`, `ogre`, `ogre_fire`, `ogre_cave`,
`wraith`, `wraith_flame`, `werewolf`, `werewolf_beast`, `werewolf_wolf`, `boar`, `duck`, `litterbox`,
the second half of each ogre (`ogre_over`, `ogre_fire_over`, `ogre_cave_over`), and the six halves of
the three horses. Most of those are things this repo ships **no** geometry for at all, so before the
bridge runs they do not render.

Four of these needed more than a manifest line, and each one is a pattern rather than a special case:

| Was held back | What it took |
|---|---|
| `boar`, `duck` | No class in the archive: the original rendered them on Minecraft's `ModelPig` and `ModelChicken`. The converter already rebuilds vanilla base layers from BTA's own geometry, so `<id>.vanilla` rebuilds the whole model the same way. |
| `horse`, `horse_unicorn`, `horse_pegasus` | The original draws a horse as two models with a texture each — `horse<colour>a.png` for body/tail/legs, `horse<colour>b.png` for head/neck/ears and the horn or wings. Both halves convert and the horse renderers draw them as two layers, the way `MobRendererBigCat` draws the lion's mane. One head model serves all three: the horn and wings are always built and simply blank on a plain horse's sheet, which is how the original did it too. |
| `werewolf_wolf` | The beast and the pack wolf shared one geometry here, so neither could take the skin painted for it. They are two models again — `ModelWerewolf` and `ModelWolf2` — as they were in the original, and the wolf takes `ModelWolf1` as a second layer on `wolfb.png` for the neck it was missing. |
| `litterbox` | The original had one image and swapped a `Litter` box for a `LitterUsed` one. Both convert, and `LitterboxRenderer` hides whichever does not apply, the way the doe's antlers are hidden. |

Two mobs are **not** bridged, and that is a decision rather than a gap:

| Built in on purpose | Why |
|---|---|
| `fox` | v2.12.2 has `fox.png` and nothing else — there is no arctic fox in that version at all, and this port draws angry, arctic and arctic-angry as well. This repo's own fox set is complete and self-consistent; replacing one quarter of it with the original's art on the original's boxes would be a downgrade, not a partial win. |
| `bunny` | The archive has four skins and this port's `variants.json` declares five. Same reasoning: a complete built-in set beats four fifths of a bridged one. |

Two audit lines survive all of this and cannot be cleared from here: `entity 'duck' has no model` and
`entity 'boar' has no texture directory`. `MMAudit` checks what the mod itself ships, and both of
those are supplied by the generated pack at runtime — which is the whole point, since the art is not
ours to ship. What the warnings pointed at (a duck with no geometry, a boar with no skin) is fixed;
the check is looking in the wrong place to see it.

## Never

Do not commit extracted geometry, extracted textures, or the source archive itself. The output belongs
in the player's generated pack only. `.gitignore` should cover the generated pack if it ever lands
inside the project directory.
