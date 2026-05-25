# Security

## Supported versions

| Version | Supported |
|---|---|
| `26.x` (current CalVer line) | yes |
| Older `2.x` and earlier | best effort, no backports |

## Reporting

Please don't file security issues in the public tracker.

Use the GitHub Security Advisory form instead:
<https://github.com/xcutiboo/MythicRod/security/advisories/new>.

If that doesn't work for you, ping <https://github.com/xcutiboo> directly.

What helps in the report:

- MythicRod version and Paper build.
- Repro steps or a minimal proof of concept.
- The impact (crash, data loss, privilege escalation, RCE, etc.).

Expect a first response within 7 days. Fixes usually land on a private
branch and get released alongside the public disclosure.

## Scope

In scope:

- Plugin source in this repo.
- Default config, drops, and language files shipped in the jar.
- The public API surface (`mythicrod-api`).

Out of scope:

- Vulnerabilities in Paper, Folia, Adventure, or any other upstream
  dependency. Please report those to the upstream project.
- Issues that require an operator to grant `mythicrod.admin.*` to
  untrusted players.
- Server misconfiguration unrelated to MythicRod (open RCON, missing
  firewall rules, leaked op list, etc.).
