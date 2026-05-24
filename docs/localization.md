---
title: Localization
nav_order: 12
has_children: true
---

# Localization

![divider]({{ site.baseurl }}/assets/divider.svg)

## Children

- [Crowdin]({{ site.baseurl }}/localization/crowdin.html)
- [Translation style guide]({{ site.baseurl }}/localization/style-guide.html)
- [Sync runbook]({{ site.baseurl }}/localization/sync-runbook.html)

MythicRod ships English (`en_US`) and Japanese (`ja_JP`) out of the box.
Every other language is community-driven through Crowdin and arrives in
the repository as l10n PRs.

## Source of truth

| File                                                  | Role                          |
| ----------------------------------------------------- | ----------------------------- |
| `mythicrod-paper/src/main/resources/lang/en_US.yml`   | Source. All other locales mirror this key tree. |
| `mythicrod-paper/src/main/resources/lang/ja_JP.yml`   | Bundled Japanese.              |
| `mythicrod-paper/src/main/resources/lang/<locale>.yml`| Downloaded by `crowdin.yml`.   |

The `BundledLocaleParityTest` JUnit test fails the build if any bundled
locale drops or adds keys against `en_US.yml`, or if `%placeholder%` tokens
differ between locales. Crowdin-managed locales are not checked because
they may be partially translated.

## Repository config

`crowdin.yml` maps the source file to its translation path and lists all
locale aliases:

```yaml
files:
  - source: /mythicrod-paper/src/main/resources/lang/en_US.yml
    translation: /mythicrod-paper/src/main/resources/lang/%locale_with_underscore%.yml
    type: yaml
    update_option: update_as_unapproved
    languages_mapping:
      locale_with_underscore:
        ja: ja_JP
        de: de_DE
        ...
```

`update_as_unapproved` means Crowdin marks pulled source updates as
unapproved so translators have a chance to review them.

## GitHub Actions workflows

| Workflow                              | Trigger                          | What it does                                                                |
| ------------------------------------- | -------------------------------- | --------------------------------------------------------------------------- |
| `.github/workflows/crowdin.yml`       | push to `master`, weekly cron, manual | Uploads the latest `en_US.yml` to Crowdin and opens a PR on `l10n_master` when translations change. |
| `.github/workflows/crowdin-seed.yml`  | manual only                      | One-shot. Uploads bundled `ja_JP.yml` into Crowdin so it appears in the project. |

Both workflows are no-ops unless the Crowdin project is configured. They
read these GitHub config values:

| Name                       | Type             | Used for                                |
| -------------------------- | ---------------- | --------------------------------------- |
| `CROWDIN_PROJECT_ID`       | Repository variable | Crowdin numeric project id.            |
| `CROWDIN_PERSONAL_TOKEN`   | Repository secret   | Crowdin user API token.                |
| `GITHUB_TOKEN`             | Repository token    | Branch + PR creation. Provided by GitHub Actions. |

Set the variable + secret at
**Settings → Secrets and variables → Actions** before tagging a release.

## How to add a new bundled locale

1. Copy `en_US.yml` to the new file (for example `de_DE.yml`).
2. Translate value strings only. Keep keys, MiniMessage tags, and
   `%placeholder%` tokens identical to `en_US.yml`.
3. Add the new locale to `BundledLocaleParityTest.BUNDLED_TARGETS`.
4. Update `crowdin.yml`'s `languages_mapping` if the Crowdin code differs
   from the underscore form.
5. Run `./gradlew :mythicrod-paper:test`. The parity test catches drift.
6. Open a PR. Once merged on `master`, the Crowdin workflow uploads the
   updated source. To seed the existing translations into Crowdin in one
   shot, trigger the `Crowdin seed translations` workflow manually.

## Why a locale might not appear in Crowdin

Crowdin only sees what is uploaded. The scheduled `crowdin.yml` workflow
uploads the source file (`en_US.yml`). It does not upload existing
translations. If a freshly added bundled locale (for example `ja_JP.yml`)
is not visible in Crowdin yet:

1. Confirm `CROWDIN_PROJECT_ID` and `CROWDIN_PERSONAL_TOKEN` exist.
2. Run **`Crowdin seed translations`** from the Actions tab. This sets
   `upload_translations: true` for a single run and brings the bundled
   files into the Crowdin project.
3. After it finishes, the language appears in the Crowdin dashboard with
   its existing strings marked as imported but unapproved.

## Runtime behaviour

- `config.yml`'s `language.default` selects the server-wide locale.
- Players can override it through `/mythicrod gui` -> Language or
  `/mythicrod config language <locale>` (admin) and a per-player override
  is persisted via `PlayerDataService`.
- `LanguageManager.getAvailableLanguages()` enumerates every file present
  in `plugins/MythicRod/lang/` plus the bundled jar entries. Brigadier
  autocomplete only suggests those.
- Missing keys fall back to the source locale, then to the literal key
  name, so a half-translated file never silently shows blank text.

## Style guide for translators

- Keep MiniMessage tags exactly as in the source.
- Keep `%placeholder%` tokens. Reorder them inside the sentence to match
  the target language naturally.
- Do not translate product names (`MythicRod`, `Nexo`, `Paper`, `Folia`,
  `Bukkit`).
- Command names and permission nodes stay in English.
- Keep messages short and direct. The plugin sends a lot of chat lines
  during a catch.

---

[← Back to docs home](./) · [Crowdin project](https://crowdin.com/project/mythicrod) · [GitHub](https://github.com/xcutiboo/MythicRod)
