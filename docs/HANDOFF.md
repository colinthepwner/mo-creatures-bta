# Handoff — unreleased work on `claude/mo-creatures-settings-aro3f9`

Written for whoever picks this up on a machine that can actually build and run BTA. The work below is
finished and pushed, but it is **not built, not tested in-game, and not released**, because the
environment it was written in could do none of those three things. What is left is the part that needs
a real machine.

## State

| | |
|---|---|
| Branch | `claude/mo-creatures-settings-aro3f9` @ `ff1a2c2` |
| `master` | `a161bb3` — two commits behind, no divergence, fast-forward merges cleanly |
| Last release | [v1.8.1](https://github.com/colinthepwner/mo-creatures-bta/releases/tag/v1.8.1), tagged on `master`, jar attached |
| `mod_version` | still `1.8.1` in `gradle.properties` — **must be bumped before releasing** |

Two commits are on the branch and in no release:

- `d92366c` **Carry the original's settings across into creatures.cfg** — DrZhark's v2.12.2 settings,
  the six GUI panels' worth, as `config/creatures.cfg`. See the `MMConfig` class javadoc for what was
  carried, what was rescaled and the five settings that have nothing here to attach to.
- `ff1a2c2` **Find the player's copy of the original wherever they put it** — the asset bridge no
  longer requires a particular filename or folder. See `MMAssetBridge`'s class javadoc.

## What still needs doing

1. Build.
2. Test in-game — nothing below has ever been run inside BTA.
3. Bump `mod_version`, merge to `master`, tag, release.

### 1. Build

`./gradlew build` → `build/libs/creatures-<version>+8.0.1.jar`.

**If that fails to resolve dependencies**, `maven.thesignalumproject.net` is down again — it serves
both `halplibe` and `fabric-loader`, and it was returning Cloudflare 522 throughout the session this
work was written in. Two ways out, best first:

- **`./gradlew build --offline`.** This is very likely to just work on the release machine: v1.2.0
  through v1.8.1 were all built there, so `~/.gradle/caches` already holds loom, halplibe `6.1.3+8.0`
  and loader `0.18.4-bta.11`. Nothing new is needed to compile these two commits.
- **Wait for the maven.** It is a server-side outage, not a local network problem, so it will be down
  for everyone until it isn't.

There is a third route — compiling with `javac` directly and packaging the jar by hand — and it was
verified to work, but it should be a last resort. Details in the appendix, along with what it does not
reproduce.

### 2. Test in-game

Nothing here has been run. The two commits want quite different checks.

**Settings.** Launch once and confirm `config/creatures.cfg` is generated with all seven categories —
`IDs`, `SpawnLimits`, `SpawnFrequencies`, `Hunters`, `HostileMobs`, `WaterMobs`, `Replacements` — and
that values are the original's defaults. Then change something with a visible effect — `Hunters.attackHorses`
to `false`, or an `ogreRange` — restart, and confirm the behaviour follows. Every value is read once at
startup and cached, so a config change needs a restart, not a world reload.

**Asset bridge.** This is the one that most needs real eyes, because it now searches the whole game
directory rather than two fixed paths. Put a copy of the original somewhere under the game directory
and watch the log for:

```
Asset bridge: N textures bridged from '<source>' into texture pack 'MoCreaturesAssets'
Asset bridge: sources used, best first: <paths>      (only when more than one contributed)
Creatures audit: asset bridge read '<source>' — N textures bridged, M not found, pack auto-enabled: true
```

`<source>` is a filename when one archive supplied everything, or a folder and a file count
(`'mods/fullmocreatures' (97 files)`) when an unpacked copy did.

Worth trying at least the unpacked-folder case, since that is new and is how the copy on the release
machine is currently stored — a folder named `fullmocreatures`. **It must be inside the game
directory.** The search deliberately does not look outside it, so a copy sitting on the Desktop or
anywhere else on the drive will not be found no matter what it contains; `<game dir>\mods\fullmocreatures\`
works. If nothing is found the log says so plainly and the mod falls back to built-in art:

```
Asset bridge: no Mo' Creatures files found anywhere under '<game dir>' — mobs will use built-in textures ...
```

Second launch should print `already built from ... skipping extraction` instead of re-extracting.
Deleting `texturepacks/MoCreaturesAssets` forces a fresh search.

Note the stamp format changed, so the **first** launch after this rebuilds the texture pack once even
if the source has not moved. That is expected, not a bug.

### 3. Release

Follow the established pattern exactly — every previous release does this:

1. Bump `mod_version` in `gradle.properties`. `1.9.0` fits: two features, no breakage.
2. Fast-forward `master` to the branch.
3. Tag on `master`, `v1.9.0`.
4. Create the release, title in the existing house style (`1.9.0 - <what changed, lowercase>`), and
   **attach the built jar** as `creatures-1.9.0+8.0.1.jar`. Every prior release has its jar attached;
   a release without one breaks the pattern people are downloading from.

The two commit messages are written to be usable as release notes more or less as they stand.

## What was and was not verified

Verified, by running the real code against the real v2.12.2 archive:

- Asset discovery across nine game-directory layouts — documented install, a `.dat` renamed beyond
  recognition four levels down outside `mods/`, a fully unpacked folder, the inner zip loose under
  another name, nothing-but-decoys, a mixed `mods/` including this mod's own jar, a 60-mod install with
  and without the archive present, and a run with an existing generated pack plus a rival texture pack
  carrying a decoy `bear.png`. All find every one of the 97 entries the manifests ask for, except the
  two designed to find nothing. A symlink loop is contained by the depth cap.
- Cache behaviour — a fresh stamp validates, an unrelated mod appearing does not invalidate it,
  touching a file the pack was actually built from does, and restoring it revalidates.
- Cost on a 60-mod install with 2472 files: about 30–50 ms to scan and 110–160 ms to read, with the
  filesystem cache warm. **Worth watching during testing**: on a genuinely cold cache — which is what a
  game launch starts from — the same scan measured several seconds in that environment, whose storage
  is slow and probably not representative. It should be far quicker off an SSD, but the number to trust
  is one measured on the release machine, not this one. It is a first-launch cost either way: once the
  pack is stamped, later launches skip the walk and only stat the recorded files.
- Compilation, against the real BTA 8.0.1 jars and HalpLibe sources.

**Not** verified, and the reason in each case:

- Anything inside BTA. No runtime in that environment — the game was never launched, no world was
  loaded, no mob was looked at.
- The jar. Loom never ran; see above.
- `config/creatures.cfg` as actually written by HalpLibe's `TomlConfigHandler`. The values and their
  wiring were checked by reading, not by generating the file.
- The settings' effect on live mob behaviour.

## Appendix — building without loom

Only if both routes in step 1 are unavailable. This produces a semantically equivalent jar, not a
bit-identical one, and it has never been loaded by the game.

It was validated by rebuilding the **already-released** v1.8.1 from its tag and comparing against the
published jar:

- Identical class list, 90 of 90.
- Correct class-file version, major 61 (Java 17), given `--release 17`.
- 37 of 90 byte-identical. The other 53 differ **only** by a `MethodParameters` attribute, which is an
  artifact of compiling on javac 21 rather than the Java 17 Adoptium toolchain the build declares. No
  JDK 17 was available. On a machine with one, expect the match to be closer still.

Two flags matter and are easy to miss: `--release 17` (without it the classes are major 65 and will not
load on a Java 17 runtime) and `-g` (Gradle compiles with full debug info; without it every class
differs by a missing `LocalVariableTable`).

```
javac --release 17 -g -proc:none -encoding UTF-8 -d classes \
  -cp "<bta-client.jar>:<bta-server.jar>:<fabric-loader>:<sponge-mixin>:<asm*>:<guava>" \
  -sourcepath "src/main/java:<halplibe-src>" \
  $(find src/main/java -name "*.java")
```

Packaging notes, all of which the real build does for you:

- Only `teamport/**` classes belong in the jar. Compiling with HalpLibe on the `-sourcepath` also
  compiles HalpLibe itself into the output directory; that must not be shipped.
- `fabric.mod.json` and `*.mixins.json` need their `${...}` placeholders expanded — `version`,
  `fabricloader`, `halplibe`, `java`, `modmenu` — see `processResources` in `build.gradle.kts`.
- `LICENSE` is copied to the jar root renamed to `LICENSE_creatures`.
- No mixin refmap is needed. The released jars carry `Fabric-Mapping-Namespace: official` and contain
  no refmap, because BTA ships deobfuscated and compile-time names already match runtime ones.
- The HalpLibe on that `-sourcepath` was `6.1.4` against a declared dependency of `6.1.3+8.0`. Close,
  but it is a real difference and another reason this route is the last one.
