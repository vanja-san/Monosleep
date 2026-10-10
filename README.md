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
- **Configurable notice.** Whenever somebody successfully goes to bed, everyone in the level sees a
  short message about it. Where it appears is up to the server owner, see
  [Configuration](#configuration). The message is translatable, so clients with the mod installed
  read it in their own language, and clients without it get readable fallback text instead of a raw
  key.

## Configuration

On the first start the mod writes `config/monosleep.json`. It is read once at server start, so a
change takes effect after a restart.

```json5
{
  // Where the "somebody went to bed" notice is shown. It is read once at server start,
  // so a change here only takes effect after a restart.
  //
  // CHAT                a line in the regular chat
  // ACTION_BAR          above the hotbar, where vanilla puts its own sleep notice
  // TITLE               in the middle of the screen, at half the size of a vanilla
  //                     title so that the whole line fits
  // TAB_LIST            above the player list; it replaces the header until another mod
  //                     or plugin writes its own
  // OFF                 no notice at all
  "notificationTarget": "CHAT",

  // Seconds that have to pass between two notices. While they have not, the next notice
  // is dropped instead of being sent. It counts for every target, not only for chat,
  // because the notice is dropped before the target is even looked at.
  "notificationCooldownSeconds": 5,

  // The same, but counted for each player separately: a notice about somebody who just
  // went to bed is dropped for this many seconds, even when the cooldown above has
  // already passed.
  "notificationCooldownPerPlayerSeconds": 5,

  // How long a TITLE notice stays on screen: fade in, hold, fade out. A tick is a
  // twentieth of a second, so the default holds it for two seconds. These only matter
  // for the TITLE target, the other places are replaced or timed by the client.
  "titleFadeInTicks": 5,
  "titleStayTicks": 40,
  "titleFadeOutTicks": 10
}
```

The file is JSON that also accepts `//` comments. Values are case-insensitive, and an unknown one is
reported in the log and falls back to `CHAT`.

| Value | Where the notice lands |
| --- | --- |
| `CHAT` | a line in the regular chat, kept in the chat history |
| `ACTION_BAR` | above the hotbar, the spot vanilla uses for its own sleep notice |
| `TITLE` | in the middle of the screen, at half the size of a vanilla title |
| `TAB_LIST` | the header above the player list, which stays until something else writes one |
| `OFF` | no notice at all |

### Spam

A notice the client already shows cannot be taken back, so the chat keeps every line and the only
way to slow it down is to not send the next one in time:

- `notificationCooldownSeconds` — a window for every notice, so a crowd getting into bed at the
  same time produces one line instead of one each.
- `notificationCooldownPerPlayerSeconds` — the same window for each player on their own, so one
  person getting in and out of bed repeatedly stops adding lines once the crowd cooldown has
  passed.

Both default to 5 seconds, and `0` turns the matching one off. They are checked before the target
is looked at, so they apply to every target, not only to `CHAT`. The windows are counted per
server, not per dimension, because chat is shared between dimensions: a notice in the Nether counts
against the next notice in the Overworld. They are cleared when the server starts, which matters in
single-player, where the game keeps running after the world is closed.

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
