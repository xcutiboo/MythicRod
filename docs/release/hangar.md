---
title: Hangar
parent: Release guide
nav_order: 1
---

# Hangar publishing

## What this does

Pushes the MythicRod Paper jar to the project page at
<https://hangar.papermc.io/xcutiboo/MythicRod> whenever a `v*` tag lands
on `master`.

## What it uses

- `.github/workflows/publish-hangar.yml`
- `io.papermc.hangar-publish-plugin` configured in
  `mythicrod-paper/build.gradle.kts`
- Repository secret: `HANGAR_API_TOKEN`
- Project slug: `MythicRod`

## Required secret

1. Go to <https://hangar.papermc.io/auth/settings/api-keys>.
2. Create a key with `create_version` permission on the MythicRod
   project.
3. Save it to the repo as `HANGAR_API_TOKEN` at
   <https://github.com/xcutiboo/MythicRod/settings/secrets/actions>.

## What gets uploaded

| Field | Source |
| --- | --- |
| Version | tag with the leading `v` stripped |
| Channel | `Snapshot` for any version containing `-`, else `Release` |
| Jar | `mythicrod-paper/build/libs/MythicRod-Paper-<version>.jar` |
| Changelog | `CHANGELOG.md` from the tagged commit |
| Platform | Paper |
| Platform versions | value of `paperVersion` in `gradle.properties` |

## Smoke check before the first release

```bash
gh workflow run publish-hangar.yml --ref master
gh run watch
```

The workflow logs `Skipping Hangar publish: HANGAR_API_TOKEN is not
configured.` when the secret is missing. That is the safe failure mode.

## When something fails

- 401 / 403 from Hangar: token expired or missing scope. Regenerate the
  key.
- 409 conflict on version: the same version already exists on Hangar.
  Delete it from the Hangar UI or pick a new tag.
- Missing jar: `shadowJar` is required. The release workflow does run it,
  but a `runs-on` change or cache regression can cause a stale build.
  Re-run from a clean workflow trigger.

[← Release guide]({{ site.baseurl }}/release/)
