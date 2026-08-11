# Mo' Creatures — BTA 8.0.1 (Babric) port status

Continues [bta-team-port/mo-creatures](https://github.com/bta-team-port/mo-creatures) (archived at BTA 7.2,
with an unfinished 7.3 rewrite branch). This tree targets **BTA 8.0.1**.

## Toolchain

| | 7.3 branch | here (8.0.1) |
|---|---|---|
| BTA | 7.3-pre2 | **8.0.1** (`release` channel) |
| HalpLibe | 5.0.0 | **6.1.3+8.0** |
| Loader | 0.15.6-bta.7 | **0.18.4-bta.11** |
| Java | 8 | **17** |
| Build | Groovy `build.gradle` | Kotlin DSL + `libs.versions.toml` |

Build: `./gradlew build` → `build/libs/creatures-<version>+8.0.1.jar`

## API migration performed (7.3 → 8.0.1)

These are the breaking changes that had to be worked through:

1. **Entrypoints → events.** HalpLibe 6.1.0 deprecated `GameStartEntrypoint` / `ClientStartEntrypoint` /
   `RecipeEntrypoint` for removal. Replaced with `CommonEvents.*` / `ClientEvents.*` listeners keyed by
   `Key.of(MOD_ID)`. `fabric.mod.json` collapses to just `main` + `client` entrypoints.
2. **Mod registration.** `MOD_ID` now comes from `HalpLibe.registerMod("creatures", true)`.
3. **Entity registration.** `EntityHelper.createEntity(cls, id, name, rendererSupplier)` is gone. Now
   `EntityDispatcher.getInstance().addMapping(cls, id, factory, langKey)` — the display name is a
   **language key**, not a literal, and the renderer is no longer passed here.
4. **Renderer registration.** Split out to `ClientEvents.ENTITY_RENDERER_RELOAD` →
   `dispatcher.assignRenderer(...)`, so renderers survive dispatcher reloads. See `MMEntityRenderers`.
5. **Models: `ModelBase` deleted entirely.** This is the big one. BTA 8.0 has no `ModelBase`/`Cube`
   system at all — entity models are Bedrock-format geometry JSON loaded through Dragonfly. Renderers now
   call `setModel(key, path, inflation)` and pose bones via `getTransform(name)` on a `StaticEntityModel`.
   The 7.3 branch's hand-written Java models were therefore a dead end and were removed; the Bedrock
   geometry already in `assets/creatures/models/entity/` is what 8.0 wants.
6. **Renamed/moved:** `EntityRenderDispatcher` → `EntityRendererDispatcher`; `Gamemode.creative` →
   `Gamemodes.CREATIVE`; `world.seasonManager` → `world.getSeasonManager()`;
   `getBlockPathWeight(x,y,z)` → `getBlockPathWeight(TilePosc)`; `sendDeathMessage` → `sendsDeathMessage`;
   `inventory.getCurrentItemIndex()` → `getCurrentSlot()`;
   `SoundRepository.SOUNDS.registerNamespace(ns)` → `SoundRepository.namespaceAdded(ns)`;
   `spawnParticle(...)` gained a trailing `boolean`.
7. **Bounding boxes are now JOML.** `Entity.bb` is `org.joml.primitives.AABBd`, which has no `grow()`.
   `MMUtils.grow(...)` restores the old semantics without mutating the entity's own box;
   `intersects` → `intersectsAABB`.
8. Mixin `compatibilityLevel` raised to `JAVA_17`.

## Working

The roster is complete: **all 30 mobs** of the original are registered, rendering and behaving. The
README lists them. Polar bear and arctic fox are variant flags on the base mob rather than separate
entity classes, as are the seven big cat species — that consolidation came from the 7.3 rewrite and
was kept.

The litter box is in, as a block with a tile entity. So are four of the original's items: big cat
claw, shark teeth, sugar lump and pet food.

BTA 8.0.1 ships a deer of its own. This port's deer replaces it in every biome's spawn list by
default; `Replacements.replaceVanillaDeer` turns that off.

## Not ported

- **Kitty bed** — the original's other piece of cat furniture, and the one entity of its roster that
  is not here.
- **Whip, medallion, rope, wool ball, hay stack, and the original's own saddle.** The whip and the
  medallion drove commands and pet naming, neither of which this port has. Horses take the vanilla
  saddle, and a pack horse carries a chest without needing an item of its own for it.

Later Mo' Creatures releases added snakes, turtles, crocodiles, elephants, wyverns and more. None of
them are in scope: this port targets **v2.12.2 for Beta 1.7.3**, and none of them exist in it.

## Licensing

The port code is CC0 (inherited from Team Port). The original Mo' Creatures is DrZhark's work; this is a
reimplementation against BTA's API rather than a redistribution of the original mod.
