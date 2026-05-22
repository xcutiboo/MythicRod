---
title: Modrinth
parent: Release guide
nav_order: 2
---

# Modrinth publishing

## What this does

Uploads the MythicRod Paper jar to
<https://modrinth.com/plugin/mythicrod> on every `v*` tag.

## What it uses

- `.github/workflows/publish-modrinth.yml`
- `Kir-Antipov/mc-publish@v3.3`
- Repository secret: `MODRINTH_TOKEN`
- Modrinth project slug: `mythicrod` (id `Uc7DXvOG`)

## Required secret

1. Go to <https://modrinth.com/settings/pats>.
2. Create a personal access token. Scopes needed:
   - `read_projects`
   - `write_projects`
   - `create_versions`
   - `read_versions`
   - `write_versions`
3. Save it as `MODRINTH_TOKEN`.

## What gets uploaded

| Field | Value |
| --- | --- |
| Version number | tag with the leading `v` stripped |
| Version type | `beta` for tags containing `-rc`, `-beta`, `-alpha`, or `-snapshot`; `release` otherwise |
| Files | `MythicRod-Paper-<version>.jar` (the shaded jar) |
| Name | `MythicRod <version>` |
| Loaders | `paper`, `folia` |
| Game versions | `26.1.2` |
| Changelog | `CHANGELOG.md` |
| Dependencies | Nexo as optional |
| Fail mode | `warn` (workflow does not abort on a single field complaint) |

## Smoke check before the first release

The workflow short-circuits with `MODRINTH_TOKEN is not configured;
skipping Modrinth publish.` when the secret is missing.

## When something fails

- `401 Invalid Authentication Credentials`: the token is malformed
  (whitespace, quotes, or wrong scope). Regenerate the PAT and re-store
  it via `gh secret set MODRINTH_TOKEN --body '<token>'`.
- 409 conflict on version: that version is already on Modrinth. Either
  delete it from the Modrinth UI or pick a new tag.
- Missing jar: `shadowJar` did not run. Re-trigger the workflow from a
  clean cache.

[← Release guide]({{ site.baseurl }}/release/)
