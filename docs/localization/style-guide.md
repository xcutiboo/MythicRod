# Translation style guide

Keep MythicRod messages short, calm, and easy to skim. The plugin sends
a lot of chat lines during a single catch.

## Tone

- Plain, practical, direct.
- No marketing.
- No filler words.
- No em dashes.
- Friendly, not cute.

## Structure

| Type | Tone | Notes |
| --- | --- | --- |
| Player chat | conversational | short sentence, single line |
| Admin chat | precise | name the offending value when a command fails |
| GUI labels | concise | one to three words |
| GUI lore | sentence fragments | one per line |
| Errors | actionable | tell the player or admin what to do next |
| Catch messages | excited but small | reserve emphasis for rare and legendary |

## Symbols

- `✓` for success messages
- `✗` for errors
- `⚠` for warnings
- `⟳` for in-progress state
- Mirror the source for new keys

## MiniMessage

Tags travel with the value. Translate the words, not the tags.

```yaml
ok: '<green>✓ <gray>%count%<gray> drops loaded.'
```

Keep `<green>`, `<gray>`, `<green>` exactly where they are. Reorder the
sentence around the placeholders if the target language reads better that
way.

## Placeholders

`%count%`, `%player%`, `%tier%`, `%biome%`, `%locale%`, `%mode%`, and
similar tokens are filled at runtime. The locale parity test refuses to
merge a translation that adds, drops, or renames a token.

You can reorder tokens inside a sentence to match the target language.
You cannot rename them.

## Names that stay in English

| Term | Why |
| --- | --- |
| `MythicRod` | product name |
| `Paper`, `Folia`, `Bukkit` | platform names |
| `Nexo` | dependency name |
| `mythicrod.gui`, `mythicrod.admin.config`, etc. | permission identifiers |
| `/mythicrod`, `/mr`, `/mrod` | command literals |
| Material keys (`DIAMOND`, `NETHER_STAR`, ...) | code identifiers |
| Locale codes (`en_US`, `ja_JP`) | machine-readable |

[← Localization](../localization.md) ·
[Crowdin](crowdin.md)
