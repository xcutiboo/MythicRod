# Contributing

## Setup

- JDK 25.
- A Paper test server. The version we build against lives in
  `gradle.properties` under `paperVersion`.
- The Gradle wrapper is in the repo, so you don't need Gradle installed
  globally.

## Layout

| Module | What's in it |
|---|---|
| `mythicrod-api` | Public API, service contracts, value objects. No Bukkit/Paper types. |
| `mythicrod-common` | Drop logic, config, stats, text. Depends only on `mythicrod-api`. |
| `mythicrod-paper` | Paper runtime: listeners, commands, GUIs, scheduler bridge, item factory. |

If you find yourself pulling Bukkit/Paper types into `mythicrod-api` or
`mythicrod-common`, that's a design break. Look for another seam.

A `mythicrod-spigot` module is not in the repo yet. The split is set up so
adding it is straightforward, but a second runtime is real maintenance load
and I'm not shipping a half-baked one. If you want it sooner,
[Ko-fi](https://ko-fi.com/xcutiboo) is the fastest signal.

## Build and test

```bash
./gradlew build                          # compile + unit tests
./gradlew test                           # tests only
./gradlew :mythicrod-paper:shadowJar     # shaded plugin jar
```

CI runs the same `./gradlew build` on every push and PR.

## Style

- 4-space indents, no wildcard imports, no trailing whitespace.
- `mythicrod-common` and `mythicrod-api` build with `-Werror`. Don't suppress
  warnings to make it pass; fix them.
- No `System.out`, `System.err`, `printStackTrace`, or stray debug prints.
  Use the plugin logger.
- `mythicrod-api` should stay source-compatible across minor releases.
  Breaking changes go in a major version bump and get a CHANGELOG entry.

## Folia notes

`folia-supported: true` is in the descriptor. When you touch listeners,
GUI code, entity mutations, or the scheduler:

- Don't assume one global main thread.
- Schedule entity/inventory work through `PlatformScheduler` (the Folia
  bridge lives in `FoliaSchedulerService`), not `Bukkit.getScheduler()`.
- Async callbacks must reschedule back to the right owner before touching
  world state.

## PRs

1. For anything non-trivial, file an issue first so the design can be
   talked through.
2. One logical change per PR.
3. Add a CHANGELOG entry under `[Unreleased]`.
4. `./gradlew build` before pushing.
5. Link the issue from the PR.

## Reporting bugs

Please use the issue templates in `.github/ISSUE_TEMPLATE/`. The fields are
there for a reason: server log, MythicRod version, Paper build, repro
steps. Without those it's hard to act on the report.

## Translations

Source strings live in `mythicrod-paper/src/main/resources/lang/en_US.yml`.
All other locales mirror its key layout.

Use Crowdin if you can: <https://crowdin.com/project/mythicrod>. The
GitHub action keeps source strings in sync and opens a PR when
translations land. Direct file edits against locale yml files are accepted
as a fallback but they get overwritten the next time Crowdin syncs unless
the same change exists upstream.

Keys and placeholders stay identical to `en_US.yml`. Translate values
only.

## Security

Don't file security issues in the public tracker. See
[SECURITY.md](SECURITY.md).
