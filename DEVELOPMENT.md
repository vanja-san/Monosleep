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
- Either one writes `run/config/monosleep.json` on the first start. Edit `notificationTarget` there
  and restart to switch where the notice appears.
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

- `startSleepInBed(...)` sends the "somebody went to bed" notice to every player in the level on a
  successful sleep. `MonosleepNotifications` picks the place the notice lands, see
  [Configuration](#configuration). The default, the action bar, is drawn with the regular font at
  the player's own GUI scale, unlike a screen title, which the client renders at 4x the font size —
  too large for a message that fires every time somebody goes to bed.

`MonosleepNotifications` (`monosleep`)

- One `switch` over `NotificationTarget`, one send call per branch: `sendSystemMessage` for chat,
  `sendOverlayMessage` for the action bar, and the three title and tab list packets
  (`ClientboundSetTitlesAnimationPacket`, `ClientboundSetTitleTextPacket`,
  `ClientboundSetSubtitleTextPacket`, `ClientboundTabListPacket`) for everything else. There is no
  client code, so all of it goes through packets or the existing `ServerPlayer` helpers.
- The sentence is sent into the subtitle slot rather than the title slot. The client draws a title
  at `scale(4.0F)` on one centred line, with no wrapping and no shrink to fit, so on the narrowest
  common screen it has room for roughly twenty characters and the sentence does not fit; the
  subtitle is drawn at `scale(2.0F)`, twice the room, and lands whole. The title packet is still
  sent with an empty component because `Hud.extractTitle` skips the whole block unless a title is
  set; an empty title draws nothing, since HUD text has no background to fill in behind it.
- A notice the client already shows cannot be taken back. `ClientboundDeleteChatPacket` is keyed on
  `MessageSignature.Packed`, which only signed player messages carry, and
  `ClientPacketListener.handleDeleteChat` disconnects the client with `INVALID_PACKET` when the
  signature does not unpack, so sending one for a system message would kick players off. There is
  no clear-chat packet in the protocol. The two cooldown settings are therefore the only way to
  slow the notices down: they are checked before the target is looked at, so they read the same
  for every target even though chat is the only one that piles up.

`MonosleepNoticeCooldown` (`monosleep`)

- The two windows, and nothing else. It takes the player, the current time and both windows, and
  answers whether a notice is due; the caller records one only when it actually sends it, so a
  dropped notice cannot push the window forward.
- Its timestamps come from `Util.getMillis()`, which is `System.nanoTime()` scaled down. That is
  monotonic rather than a wall clock and may start negative, so there is no "very old" value to
  seed the fields with — arithmetic against one overflows and then reads as "just now", which
  silences every notice for the rest of the session. A flag records whether anything was ever sent.
- It is the one piece of stateful logic in the mod, so it is the one piece with tests. It touches
  no Minecraft class, which is what lets it be unit tested.

## Configuration

`config/monosleep.json` is written from a template on first start and read once at server start.
Changing it needs a restart; there is no reload watcher.

- **Format** is JSON that also accepts `//` comments. The file is parsed with
  `JsonParser.parseReader`, which reads in Gson's lenient mode, so an admin can annotate their own
  setup without adding a JSON5 dependency. Gson ships with Minecraft, so no build change is needed.
- **Values** are case-insensitive enum names for `notificationTarget`, whole seconds for the two
  cooldowns and whole ticks for the three title timings. A wrong type, a fractional or out of range
  number, a negative value, a file that is not a JSON object and a file that does not parse are
  each logged and fall back to the default for that one key. A key the file does not carry keeps its
  default, so a file written before a setting existed still loads.
- **The defaults** are `CHAT`, `5`, `5`, `5`, `40`, `10`, written in exactly one place each, the
  field initializers. `load()` reads the fields back when it writes the template, so the file a
  server gets always shows the values the mod actually uses.
- **The cooldowns** are counted per server rather than per level, since chat is shared between
  levels. `MonosleepNoticeCooldown` holds that state and is cleared through
  `ServerLifecycleEvents.SERVER_STARTED`, which matters on an integrated server: the game keeps
  running after the world is closed, so without it the previous world would still hold the window.

## Project layout

```
build.gradle.kts                    Loom, datagen run, dependencies, toolchain, jar, publishing
settings.gradle.kts                  plugin and dependency repositories
gradle/libs.versions.toml            version catalog: Minecraft, Loader, Fabric API, Loom, JUnit
gradle.properties                    project metadata and Gradle build flags
src/main/java/monosleep/             mod entrypoint, advancement awarding, notifications, cooldown
src/main/java/monosleep/mixin/      the two mixins
src/main/java/monosleep/config/     config/monosleep.json: loading and the target enum
src/main/java/monosleep/data/       datagen providers and translations
src/main/generated/resources/        committed datagen output
src/main/resources/                  fabric.mod.json, mixin config, icon
src/test/java/monosleep/             unit tests for the logic that does not touch Minecraft
.github/workflows/build.yml          CI: wrapper validation, Java 25, build
```

## Tests

```
./gradlew test
```

Only the logic that does not touch Minecraft is unit tested, which is currently
`MonosleepNoticeCooldown` alone. Everything that needs a live game — the mixins, the packets, the
datagen providers — is verified by running `runClient` / `runServer`.

A test here earns its place by pinning a boundary rather than a line of code: the first-notice case
in `MonosleepNoticeCooldownTest` exists because the original "nothing sent yet" timestamp overflowed
and silenced every notice for the rest of the session. Inverting that condition makes the suite go
red, which is the check worth running after touching the cooldown.

`build` runs `test` through `check`, so CI picks the tests up with no extra step.

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
