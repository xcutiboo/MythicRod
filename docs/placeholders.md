---
title: Placeholders
---

# PlaceholderAPI tokens

MythicRod registers a PlaceholderAPI expansion when PAPI is on the
server. Use the tokens below in any plugin that resolves placeholders
(scoreboard, tab list, hologram, chat formatter).

PAPI is a **soft dependency**. If you do not have it installed, nothing
breaks - the expansion just stays unregistered.

## Token reference

| Token | Returns |
| --- | --- |
| `%mythicrod_total%` | Lifetime catches |
| `%mythicrod_common%` | Common-tier catches |
| `%mythicrod_uncommon%` | Uncommon-tier catches |
| `%mythicrod_rare%` | Rare-tier catches |
| `%mythicrod_legendary%` | Legendary-tier catches |
| `%mythicrod_rod_tier%` | Selected rod tier (`basic`, `advanced`, `legendary`, `mythic`) |
| `%mythicrod_version%` | Running MythicRod plugin version |

All numeric tokens return integers as strings. Catch tokens return an
empty string when the player has no persisted stats yet (first login,
or a server that has just been wiped). `%mythicrod_rod_tier%` returns
`basic` when the player has not opened the rod menu yet.

`%mythicrod_version%` is the only token that works without a player
context - safe to use in plugin status panels.

## Example: scoreboard

```yaml
# config.yml for a scoreboard plugin
lines:
  - '&6&l⚓ MythicRod'
  - ''
  - '&7Caught: &f%mythicrod_total%'
  - '&7Legendary: &6%mythicrod_legendary%'
  - '&7Rod: &b%mythicrod_rod_tier%'
```

## Example: chat formatter

```yaml
# config.yml for a chat plugin
format: '&7[&6%mythicrod_legendary%&7] %1$s: %2$s'
```

[← Back to docs home](./)
