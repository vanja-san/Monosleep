# Development

Everything needed to build, run and understand monosleep locally. For what the mod does and how to
install it, see [README.md](README.md).

## Requirements

- **JDK 25.** Minecraft 26.3 ships Java 25 class files, so 25 is both the compile target and the
  minimum JDK to run the game. The build declares a Java 25
  [toolchain](https://docs.gradle.org/current/userguide/toolchains.html), so Gradle compiles with a
  JDK 25 no matter which JDK comes first on your `PATH`.
- Nothing else. Always go through the Gradle wrapper (`./gradlew`, not a locally installed `gradle`):
  it pins the Gradle version and verifies the distribution against the SHA-256 checksum in
  `gradle/wrapper/gradle-wrapper.properties`.

## Build

```
./gradlew build
```

The release jar and its sources jar are written to `build/libs/`. `build` also runs `check`.

## Release artifacts

Artifacts are named after what they contain, so several can sit in one folder without guessing which
one belongs where:

```
build/libs/monosleep-v1.0-mc26.3-fabric.jar           the mod to publish
build/libs/monosleep-v1.0-mc26.3-fabric-sources.jar  sources, for bug reports
```

The name is assembled in `build.gradle.kts` as `<mod id>-v<version>-mc<minecraft version>-fabric`,
so it follows `version` from `gradle.properties` and the Minecraft version from
`gradle/libs.versions.toml` automatically. The same `version` value is what ends up in
`fabric.mod.json`, which means there is exactly one place to bump a release.

## Data generation

The advancement and the `en_us` / `ru_ru` language files are produced by Fabric Datagen:

```
./gradlew runDatagen
```

Generated resources are written to `src/main/generated/resources`, which is registered as an extra
resource directory in `build.gradle.kts` and committed to the repository. A plain `./gradlew build`
therefore ships them without running datagen, and the mod stays loadable even if the generator is
never run.

After generating, review the diff in `src/main/generated/resources` and commit it. Datagen keeps an
incremental cache in `src/main/generated/resources/.cache`, which is both git-ignored and excluded
from the jar.

Sources:

| File | Role |
| --- | --- |
| `data/MonosleepDataGenerator.java` | Datagen entrypoint, registered as `fabric-datagen` in `fabric.mod.json` |
| `data/MonosleepAdvancementProvider.java` | The `slept_alone` advancement |
| `data/MonosleepLangProvider.java` | One language file per `MonosleepLang` value |
| `data/MonosleepLang.java` | Every translatable string, English and Russian side by side |
| `data/MonosleepLangKeys.java` | Translation keys shared by the runtime code and the generators |

`MonosleepLang.RU_RU` doubles as the fallback text baked into the components the server sends, so a
client without the mod still reads normal text instead of a raw translation key.

## Running it

- `./gradlew runServer` starts a development dedicated server.
- `./gradlew runClient` starts a development client with the mod loaded.
- To try the advancement without a second player online, use
  `/advancement grant @s monosleep:slept_alone`.

For IDE import and run-configuration instructions, see the
[Fabric documentation](https://docs.fabricmc.net/develop/getting-started/creating-a-project#setting-up)
for your IDE. IDE-specific files (`.idea/`, `.vscode/`, `.classpath`, `*.iml`) are git-ignored, so
each developer configures their own.

## How it works

Two mixins, both server-side.

`ServerLevelMixin` (`net.minecraft.server.level.ServerLevel`)

- `tick(BooleanSupplier)` redirects the two `SleepStatus` checks, so the requirement drops from
  `max(1, ceil(activePlayers * playersSleepingPercentage / 100))` to a single non-spectator player
  who is sleeping — and to a single player who has been asleep long enough, which is the second gate
  vanilla applies before the world advances.
- `wakeUpAllPlayers()` awards the advancement. Vanilla calls this right after the sleep status was
  satisfied and before anybody is woken up, so `ServerPlayer.isSleeping()` still reports who went to
  bed — the only moment where "exactly one player is in bed" can be observed.
- `announceSleepStatus()` is cancelled. The vanilla line ("3 players sleeping out of 5") counts
  towards the `playersSleepingPercentage` rule that this mod bypasses, so it no longer describes
  what is actually happening. `ServerPlayerMixin` takes over that spot.

`ServerPlayerMixin` (`net.minecraft.server.level.ServerPlayer`)

- `startSleepInBed(...)` sends a translatable action bar message to every player in the level on a
  successful sleep. The action bar is drawn with the regular font at the player's own GUI scale,
  unlike a screen title, which is rendered at 4x the font size — too large for a message that fires
  every time somebody goes to bed.

## Project layout

```
build.gradle.kts                    Loom, datagen run, dependencies, toolchain, jar, publishing
settings.gradle.kts                  plugin and dependency repositories
gradle/libs.versions.toml            version catalog: Minecraft, Loader, Fabric API, Loom
gradle.properties                    project metadata and Gradle build flags
src/main/java/monosleep/             mod entrypoint, advancement awarding
src/main/java/monosleep/mixin/      the two mixins
src/main/java/monosleep/data/       datagen providers and translations
src/main/generated/resources/        committed datagen output
src/main/resources/                  fabric.mod.json, mixin config, icon
.github/workflows/build.yml          CI: wrapper validation, Java 25, build
```

## Build configuration

- **Versions** live in `gradle/libs.versions.toml`: Minecraft, Fabric Loader, Fabric API and Fabric
  Loom. `gradle.properties` holds only `version` and `group`.
- **Compile JDK** is pinned to 25 in `build.gradle.kts` and passed to `javac` as `--release`, so the
  produced bytecode does not depend on the local `JAVA_HOME`.
- **Repositories** are declared in `settings.gradle.kts` under `dependencyResolutionManagement`.
  Loom adds the ones it needs to resolve Minecraft itself.
- **Task actions must not capture the build script.** A value used inside a nested action such as
  `filesMatching { }` has to be a local variable, otherwise the configuration cache fails with
  "cannot serialize Gradle script object references".
- **No dependency locking or verification metadata.** The Loom plugin is a `1.18-SNAPSHOT` and Loom
  resolves Minecraft itself, so pinned checksums would break on every snapshot bump. CI validates
  the Gradle wrapper instead, with `gradle/actions/wrapper-validation`.

## Versioning and releases

The mod version lives in `gradle.properties` as `version`, and is expanded into `fabric.mod.json` at
build time — it is written in exactly one place, and the release jar name is derived from it.

To cut a release:

1. Set the new `version` in `gradle.properties` — it drives both the artifact name and
   `fabric.mod.json`.
2. Run `./gradlew runDatagen` and `./gradlew build`.
3. Commit the version bump.
4. Tag the commit (`git tag -a v<version> -m "v<version>"`) and upload
   `build/libs/monosleep-v<version>-mc<minecraft version>-fabric.jar` to the releases page.

CI (`.github/workflows/build.yml`) validates the Gradle wrapper and runs the same `build` on every
push and pull request, so a green run is the release gate.

## Credits

Built on the [Fabric example mod](https://github.com/FabricMC/fabric-example-mod), which is
CC0-licensed. The mixins, datagen providers and Gradle setup in this repository are original work
on top of it.
