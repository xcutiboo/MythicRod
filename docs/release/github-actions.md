# GitHub Actions

Every workflow lives in `.github/workflows/`. Each one is SHA-pinned to a
specific upstream commit so a tag rewrite upstream cannot change CI
behaviour.

## Workflows

| File | Trigger | What it does |
| --- | --- | --- |
| `build.yml` | push to `master`, PR, tag `v*` | `./gradlew clean build` + JUnit + tests artifact + tagged release jar with SHA-256 |
| `codeql.yml` | push to `master`, PR, Monday cron | CodeQL scan for `java-kotlin` |
| `sonar.yml` | push to `master`, PR | SonarCloud scan against `xcutiboo_MythicRod` |
| `pages.yml` | push to `master` when `docs/**` changes | Builds Jekyll site and deploys to GitHub Pages |
| `dependency-review.yml` | PR | Blocks PRs introducing high-severity vulnerable deps |
| `crowdin.yml` | push to `master` when source file or workflow changes, Monday cron, manual | Uploads `en_US.yml` source, downloads translations, opens l10n PR on `l10n_master` |
| `crowdin-seed.yml` | manual | One-shot upload of bundled translations |
| `publish-hangar.yml` | tag `v*`, manual | Hangar publish |
| `publish-modrinth.yml` | tag `v*`, manual | Modrinth publish |

## Required secrets

| Name | Type | Used by |
| --- | --- | --- |
| `HANGAR_API_TOKEN` | secret | `publish-hangar.yml` |
| `MODRINTH_TOKEN` | secret | `publish-modrinth.yml` |
| `CROWDIN_PERSONAL_TOKEN` | secret | `crowdin.yml`, `crowdin-seed.yml` |
| `SONAR_TOKEN` | secret | `sonar.yml` |
| `CROWDIN_PROJECT_ID` | variable | both Crowdin workflows |
| `SONAR_PROJECT_KEY` | variable | `sonar.yml` |

All publish workflows short-circuit cleanly when their secrets are
missing. Normal CI does not depend on them.

## Permissions

Every workflow declares the minimum permissions it needs. Build and
release write to release assets only when triggered by a tag. Pages
writes only the Pages artifact. CodeQL writes only security events.

## When to re-run

- Failed CodeQL or SonarCloud upload: re-trigger the workflow from the
  Actions UI. They are idempotent.
- Failed Pages build: usually a broken doc link or a Jekyll plugin
  conflict. The `pages.yml` workflow log points at the file.
- Failed publish workflow: do not retag. Trigger the workflow manually
  from the Actions UI after fixing the underlying error.

[← Release guide](../release.md)
