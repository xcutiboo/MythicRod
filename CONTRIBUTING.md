# Contributing to MythicRod

Thanks for taking the time to look at MythicRod. The notes below cover the
parts that are easy to miss.

## Prerequisites

- JDK **25** (Temurin or any recent build with Java 25 support).
- A Paper-compatible test server matching the version listed in
  `gradle.properties` (`paperVersion`).
- Git with line-ending settings appropriate for your platform.

You do not need a global Gradle install — the wrapper (`./gradlew`) bundles the
correct version.

## Module Layout

| Module             | Purpose                                                                                                                      |
| ------------------ | ---------------------------------------------------------------------------------------------------------------------------- |
| `mythicrod-api`    | Public, stable integration surface. Avoid implementation classes here. Do not import Bukkit/Paper types directly.            |
| `mythicrod-common` | Platform-neutral logic (drops, config, statistics, text). Depends only on `mythicrod-api`.                                   |
| `mythicrod-paper`  | Paper-specific runtime: listeners, commands, GUIs, scheduler bridge, item factory. Depends on `mythicrod-common` and Paper. |

If a change pulls Paper/Bukkit types into `mythicrod-api` or `mythicrod-common`,
treat that as a design break and find another seam.

## Build and Test

```bash
./gradlew build           # compiles all modules and runs unit tests
./gradlew test            # tests only
./gradlew :mythicrod-paper:shadowJar   # shaded plugin jar in mythicrod-paper/build/libs
```

CI runs the same `./gradlew build` on every push and pull request.

## Style and Conventions

- Match the existing code: 4-space indentation, no wildcard imports, no
  trailing whitespace.
- Compilation is run with `-Werror` on `mythicrod-common`/`mythicrod-api`.
  Treat new warnings as errors locally — do not suppress them blindly.
- Do not introduce `System.out`, `System.err`, `printStackTrace`, or ad-hoc
  debug prints. Use the plugin logger.
- Keep `mythicrod-api` source-compatible across minor releases. Breaking
  changes belong in a major version bump and must be documented in
  `CHANGELOG.md`.

## Folia Awareness

MythicRod ships with `folia-supported: true`. When touching listeners, GUI
code, entity mutations, or schedulers:

- Do not assume a single global main thread.
- Use `FoliaSchedulerService` (`PlatformScheduler`) instead of `Bukkit
  .getScheduler()` directly when interacting with entities or regions.
- Async work must not mutate world, entity, or inventory state without
  scheduling back to the owning region or entity thread.

## Pull Requests

1. Open an issue first for non-trivial changes so the design can be discussed.
2. Keep PRs focused — one logical change per PR.
3. Update `CHANGELOG.md` under the `[Unreleased]` section.
4. Run `./gradlew build` locally before pushing.
5. Reference related issues in the PR description.

## Reporting Bugs

Use the issue templates under `.github/ISSUE_TEMPLATE/` so the maintainers have
the information they need. Server logs, the MythicRod version, Paper build,
and reproduction steps are the most useful.

## Security Issues

See [`SECURITY.md`](SECURITY.md). Do not file security issues in the public
tracker.
