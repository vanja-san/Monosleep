<div align="center">

<h1>monosleep</h1>

<h3>One sleeping player is enough to skip the night</h3>

<p>
  No matter how many other players are online.<br />
  Vanilla waits for a whole lobby to be in bed before the morning comes — monosleep needs one.
</p>

<p>
  <b>Minecraft 26.3</b> &nbsp;·&nbsp; <b>Fabric</b> &nbsp;·&nbsp; <b>Server-side</b> &nbsp;·&nbsp; <b>50 XP</b>
</p>

</div>

---

## What it does

- **Server-side.** The mod contains no client code, so players do not have to install it. In
  single-player, install it on the client that opens the world — the integrated server runs inside
  that process.
- **Advancement `monosleep:slept_alone`** — worth 50 XP, awarded when the night is skipped while
  exactly one non-spectator player is in bed, which is the only situation this mod is about.
- **Action bar notice.** Whenever somebody successfully goes to bed, everyone in the level sees a
  short message about it. The message is translatable, so clients with the mod installed read it in
  their own language, and clients without it get readable fallback text instead of a raw key.

## Requirements

- Minecraft 26.3
- Fabric Loader 0.19.5+
- Fabric API
- Java 25 (Minecraft 26.3 ships Java 25 class files)

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/installer/) for Minecraft 26.3 on the server, or
   on the client for single-player.
2. Put the matching Fabric API jar into `mods/`.
3. Drop the `monosleep` jar from the
   [releases](https://github.com/vanja-san/monosleep/releases) into the same `mods/` folder.

## License

[CC0-1.0](LICENSE) — public domain, do what you want with it.

Building, data generation and the inner workings of the mixins are documented in
[DEVELOPMENT.md](DEVELOPMENT.md).
