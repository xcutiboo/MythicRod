# Release checklist

Hand-checkable list. Run top to bottom before tagging.

## Code

- [ ] `git status` clean on `master`
- [ ] `./gradlew clean build` green
- [ ] `./gradlew test` green (114+ tests)
- [ ] `mythicrod-paper/build/libs/MythicRod-Paper-<version>.jar` exists
- [ ] `mythicrod-spigot/build/libs/MythicRod-Spigot-<version>.jar` exists
- [ ] No `System.out`, `System.err`, `printStackTrace`, TODO, FIXME, or
      em dash in the tracked tree (`./scripts` not present; use
      ripgrep)
- [ ] `paper-plugin.yml` matches latest Paper API generation

## Live servers

- [ ] Paper server starts cleanly with the new jar
- [ ] Folia server starts cleanly with the new jar
- [ ] `mythicrod status` runs on both
- [ ] `mythicrod reload` runs on both
- [ ] `mythicrod drops preview minecraft:ocean` runs on both
- [ ] Player-only commands fail cleanly from console
- [ ] No exceptions in the server log during shutdown

## Locales

- [ ] `BundledLocaleParityTest` green
- [ ] `LanguageFileLoader.BUNDLED_LANGS` lists every bundled locale
- [ ] Crowdin shows en_US as source
- [ ] Crowdin shows ja_JP under translations

## Docs

- [ ] `CHANGELOG.md` has the new release entry at the top
- [ ] `README.md` version badge points at the upcoming tag
- [ ] `docs/index.md` version-targets table updated
- [ ] `docs/release.md` matches the workflows
- [ ] All Pages workflow links resolve

## Secrets

- [ ] `HANGAR_API_TOKEN` exists
- [ ] `MODRINTH_TOKEN` exists and passes the probe
- [ ] `CROWDIN_PERSONAL_TOKEN` exists
- [ ] `CROWDIN_PROJECT_ID` variable exists
- [ ] `SONAR_TOKEN` exists

## Tag and push

```bash
git tag v<version>
git push origin v<version>
```

Three workflows fire in parallel:

- `Build and Release` creates the GitHub release
- `Publish to Hangar` uploads the Paper jar
- `Publish to Modrinth` uploads the Paper jar

## After publish

- [ ] GitHub release page lists the jar + `.sha256`
- [ ] Hangar version page is live
- [ ] Modrinth version page is live
- [ ] Update `docs/index.md` version-targets table to the next planned
      version

[← Release guide](../release.md)
