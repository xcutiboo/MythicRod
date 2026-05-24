# Release guide

![divider](assets/divider.svg)

Step-by-step for cutting a public release of MythicRod. Every secret named
here is configured at **Settings → Secrets and variables → Actions** on the
GitHub repository. Nothing in this file expects you to paste a token into a
file on disk.

## Children

- [Hangar publishing](release/hangar.md)
- [Modrinth publishing](release/modrinth.md)
- [GitHub Actions reference](release/github-actions.md)
- [Release checklist](release/checklist.md)

## 1. Required repo secrets

| Secret             | Used by                                | Notes                                                          |
| ------------------ | -------------------------------------- | -------------------------------------------------------------- |
| `HANGAR_API_TOKEN` | `.github/workflows/publish-hangar.yml` | Personal API key from Hangar account settings.                  |
| `MODRINTH_TOKEN`   | `.github/workflows/publish-modrinth.yml` | Personal access token with `Write versions`/`Create versions`. |
| `CROWDIN_PERSONAL_TOKEN` | `.github/workflows/crowdin.yml`  | Optional; only needed for the Crowdin push/pull job.            |
| `SONAR_TOKEN`      | `.github/workflows/sonar.yml`          | Optional; SonarCloud scanning.                                  |

Verify they exist before tagging:

```bash
gh secret list --repo xcutiboo/MythicRod
```

If a publish secret is missing the corresponding workflow logs `not
configured` and skips publishing. Local builds never need any of these.

## 2. Pre-flight checks

```bash
./gradlew clean build
./gradlew test
```

Both must succeed. The Paper jar lands at
`mythicrod-paper/build/libs/MythicRod-Paper-<version>.jar`. The Spigot stub
lands at `mythicrod-spigot/build/libs/MythicRod-Spigot-<version>.jar`.

Spot-check on a live Paper server using the manual runbook in
`docs/testing.md`. Folia validation is the last section there.

## 3. Update the changelog

Add the new release entry to the top of `CHANGELOG.md` using calver
(`<year>.<release>.<patch>`). Keep the entry concise and human-written.

## 4. Tag and push

```bash
git tag v2026.1.0
git push origin v2026.1.0
```

Three workflows fire in parallel:

- **Build and Release** assembles, uploads the jar, attaches SHA-256
  checksums, and creates the GitHub Release. Tags containing `-rc`,
  `-beta`, `-alpha`, or `-snapshot` are marked pre-release automatically.
- **Publish to Hangar** uses `io.papermc.hangar-publish-plugin` and the
  Hangar token to upload the shaded Paper jar.
- **Publish to Modrinth** uses `Kir-Antipov/mc-publish` with channel
  auto-detected from the tag.

## 5. After the workflows finish

- Confirm the GitHub Release page lists `MythicRod-Paper-<version>.jar`
  plus its `.sha256`.
- Confirm the Hangar version page is live at
  <https://hangar.papermc.io/xcutiboo/MythicRod/versions>.
- Confirm the Modrinth version page is live at
  <https://modrinth.com/plugin/mythicrod/versions>.
- Bump the docs `version-targets` in `docs/index.md` if anything moved.

## 6. Hot-fix retag

```bash
git tag -d v2026.1.0
git push origin :refs/tags/v2026.1.0
# fix, commit, then re-tag and re-push
```

Workflows are idempotent. Re-tagging cleanly retries the publish steps.

## 7. Versioning policy

CalVer: `<year>.<release>.<patch>`.

- Patch (`2026.1.1`) ships bug fixes only. No API changes.
- Release (`2026.2.0`) adds features. Public API stays binary-compatible.
- Year roll-over (`2027.1.0`) may rename or remove API only when the
  changelog calls it out explicitly.

Never mix the plugin version with `paper-plugin.yml: api-version`. The
plugin version is independent of the Minecraft API version.

---

[← Back to docs home](./) · [GitHub](https://github.com/xcutiboo/MythicRod) · [Hangar](https://hangar.papermc.io/xcutiboo/MythicRod)
