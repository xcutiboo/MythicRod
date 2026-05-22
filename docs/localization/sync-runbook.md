---
title: Sync runbook
parent: Localization
nav_order: 3
---

# Crowdin sync runbook

Use this when adding a new bundled locale, recovering after a Crowdin
outage, or moving the project to a different organisation.

## Daily operation

There is no daily operation. The `crowdin.yml` workflow runs on every
push that touches `en_US.yml`, on a Monday cron, and on manual dispatch.
Translation PRs land automatically on `l10n_master` and target `master`.

## Adding a new bundled locale

1. Copy `en_US.yml` into the new file (for example `de_DE.yml`).
2. Translate the values only. Keep keys, MiniMessage tags, and
   `%placeholder%` tokens identical to the source.
3. Add the locale code to
   `mythicrod-paper/src/main/java/io/xcutiboo/mythicrod/paper/internal/config/LanguageFileLoader.java`,
   the `BUNDLED_LANGS` array.
4. Add the locale to
   `mythicrod-paper/src/test/java/io/xcutiboo/mythicrod/paper/internal/config/BundledLocaleParityTest.java`,
   the `BUNDLED_TARGETS` list.
5. Make sure the mapping is in `crowdin.yml` under
   `languages_mapping.locale_with_underscore`. Add it if missing.
6. Run `./gradlew :mythicrod-paper:test`. The parity test catches drift.
7. Enable the target language in the Crowdin project under
   **Project settings → Languages**.
8. Push the branch, open the PR, merge it.
9. From the **Actions** tab, run **Crowdin seed translations**. It
   uploads the bundled translation file once so Crowdin sees it.
10. Verify the new language appears in the Crowdin dashboard with the
    seeded strings as imported but unapproved.

## When a translation does not appear in Crowdin

Symptom: `ja_JP.yml` is bundled but Crowdin only shows source strings.

Reason: the scheduled `crowdin.yml` workflow only uploads the source.
Translations need a one-time seed run.

Fix:
1. Verify `CROWDIN_PROJECT_ID` and `CROWDIN_PERSONAL_TOKEN` are set
   under **Settings → Secrets and variables → Actions**.
2. From the **Actions** tab, run **Crowdin seed translations**.
3. Confirm the workflow logs `✔ File ... ja_JP.yml`.
4. Reload the Crowdin project page. The language tile should now show
   imported strings.

## When the Crowdin auto-PR breaks

Symptom: the `l10n: sync Crowdin translations` PR fails to open, or the
download step says `no translation file to commit`.

Likely causes:
- No translator has submitted anything new since the last successful
  download. This is fine.
- Crowdin lost track of the source file because the source file path
  changed. Open `crowdin.yml`, confirm the `source:` path matches the
  current repo, then re-run the workflow.
- `CROWDIN_PERSONAL_TOKEN` was rotated. Update the secret.

## Rotating the Crowdin token

1. <https://crowdin.com/settings#api-key> → revoke the old token.
2. Create a new one with write access on the project.
3. `gh secret set CROWDIN_PERSONAL_TOKEN --body '<token>'`
4. Re-run **Crowdin seed translations** to confirm it works.

[← Localization]({{ site.baseurl }}/localization/) ·
[Crowdin]({{ site.baseurl }}/localization/crowdin.html) ·
[Style guide]({{ site.baseurl }}/localization/style-guide.html)
