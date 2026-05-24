# Crowdin

## What MythicRod uses Crowdin for

Crowdin is the single source of truth for community translations.
The English source file (`en_US.yml`) ships with the plugin; everything
else flows through Crowdin to a `l10n_master` branch, where translators
get a real review path before changes land on `master`.

Crowdin project: <https://crowdin.com/project/mythicrod>

## Repo wiring

```
crowdin.yml
.github/workflows/crowdin.yml       # scheduled source sync + l10n PR
.github/workflows/crowdin-seed.yml  # one-shot bundled translation upload
```

| Repo value | Type | Purpose |
| --- | --- | --- |
| `CROWDIN_PROJECT_ID` | variable | Numeric Crowdin project id |
| `CROWDIN_PERSONAL_TOKEN` | secret | API token (any user with write access) |

Both workflows skip silently when those are missing.

## Languages and locale codes

| Bundled file | Crowdin language code |
| --- | --- |
| `en_US.yml` | source |
| `ja_JP.yml` | `ja` |

Mappings for every other target locale live in `crowdin.yml` under
`languages_mapping.locale_with_underscore`.

If a bundled translation does not appear in Crowdin after the seed
workflow runs, the most common cause is that the target language is not
yet enabled in the Crowdin project. Enable it under **Project settings →
Languages**, then re-run **Crowdin seed translations** from the GitHub
Actions tab.

## Sync model

```
master (en_US.yml)
   │
   ▼  crowdin.yml workflow (scheduled / push)
Crowdin source pool
   │
   ▼  translators submit + approve
Crowdin translations
   │
   ▼  crowdin.yml workflow downloads
l10n_master branch + auto PR back to master
```

The translation PR is `l10n: sync Crowdin translations`. Merge it the
same way as any other PR.

## Why a one-time seed is needed for bundled translations

The scheduled workflow only **uploads the source file** and downloads
finished translations. It does not upload existing translated files,
because re-uploading them on every push could overwrite reviewer work on
Crowdin.

That means a freshly added bundled locale (for example `ja_JP.yml`
shipped with the plugin) sits on disk, but Crowdin has no record of it.
The **Crowdin seed translations** workflow does the one-time upload
explicitly:

```
.github/workflows/crowdin-seed.yml
  upload_sources: true
  upload_translations: true
  auto_approve_imported: false   # seeded strings stay unapproved
  import_eq_suggestions: false   # exact matches do not silently override
```

Run it from the Actions tab after:
- adding a new bundled locale to the repo
- enabling a new target language in the Crowdin project
- recovering from a Crowdin project rebuild

After the seed completes, the language tile on the Crowdin dashboard
shows imported strings. From there on, the scheduled workflow handles
source pushes and translation pulls automatically.

## When a freshly enabled target language still shows nothing

Symptom: a new target was enabled in Crowdin settings, but the project
dashboard still shows zero strings for it.

Fix:
1. Confirm the target language is checked under **Project settings →
   Languages → Target Languages** and the *section-local* Save button
   under that list was clicked.
2. Verify the language code in `crowdin.yml` matches the Crowdin code.
   For example `pt-BR → pt_BR`.
3. Trigger **Crowdin seed translations** from Actions. The action log
   should print `Importing translations for file '.../<locale>.yml'`
   followed by `✔ File '...'`.
4. Reload the Crowdin dashboard. The tile updates within a few seconds.

## Why local edits to non-source locale files might disappear

A direct edit to `ja_JP.yml` on `master` survives until the next
download from Crowdin. The Crowdin workflow's `update_option:
update_as_unapproved` means it does not silently overwrite approved
translations, but the auto-PR can still revert manual edits if Crowdin
holds a different (approved) string for the same key. Always upload the
change through Crowdin if you want it to stick.

## What never to translate

- product names (`MythicRod`, `Nexo`, `Paper`, `Folia`, `Bukkit`)
- command literals (`mythicrod`, `rod`, `select`, `add`, `set`)
- permission nodes (`mythicrod.admin.config` and so on)
- MiniMessage tag names (`<gold>`, `<gray>`, `<bold>`)
- `%placeholder%` tokens

[← Localization](../localization.md) ·
[Style guide](style-guide.md) ·
[Sync runbook](sync-runbook.md)
