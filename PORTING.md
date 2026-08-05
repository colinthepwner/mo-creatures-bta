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

Builds clean; four mobs registered and rendering: **Bear** (+ polar variant), **Bird**, **Bunny**, **Fox**
(+ arctic variant). Polar bear and arctic fox are variant flags on the base mob rather than separate
entity classes — that consolidation came from the 7.3 rewrite and was kept.

## Not yet done

`wip/entity-7.2-port/` holds six mobs carried over from the 7.2 branch — **Horse, Unicorn, Pegasus,
Kitty, Boar, Duck**. They are converted to the `Mob*` naming convention and had the bulk API renames
applied, but they are **excluded from the source set because they do not compile yet** (~44 errors). They
came from the 1.7.7.x era and use pre-BTA-7 idioms that still need resolving:

- `getBoundingBoxFromPool(...)` / `AABB` → JOML `AABBd`
- `difficultySetting`, `areMobsHostile()` → `gamemode.hasHostileMobs()`
- `faceEntity(...)`, various AI hooks renamed on `Mob`
- `spawnParticle(...)` trailing-boolean arity (several sites)
- `MobHorsePegasus` imports `org.useless.dragonfly.model.entity.AnimationState`, which no longer exists —
  the flight animation needs rewriting against the current Dragonfly bone-transform API
- `MobKitty` depends on `LitterboxEntity`; the litterbox and bee-hive **blocks/tile entities have not been
  ported at all** yet, nor their items/recipes

Also still absent (present in the original b1.7.3 mod, never ported by Team Port): big cats, dolphin,
shark, fishy, ogre, werewolf, wraith, ghost, scorpion, snake, turtle, rat, crocodile, elephant/mammoth,
wyvern. Some of their **sounds already ship** in `assets/creatures/sound/` (dolphin, ogre, etc.), but no
models or textures exist for them.

Note: BTA 8.0.1 now ships a **vanilla `MobDeer`**, so porting Mo' Creatures' own deer would duplicate a
base-game mob. Left out deliberately — worth a decision before adding.

## Licensing

The port code is CC0 (inherited from Team Port). The original Mo' Creatures is DrZhark's work; this is a
reimplementation against BTA's API rather than a redistribution of the original mod.
