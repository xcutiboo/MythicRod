# Security Policy

## Supported Versions

Only the latest released `2.x` line of MythicRod receives security patches.
Older releases are best-effort and may not receive backports.

| Version | Supported |
| ------- | --------- |
| `2.x`   | Yes       |
| `< 2.0` | No        |

## Reporting a Vulnerability

Do **not** open a public GitHub issue for suspected security vulnerabilities.

Instead, report privately by either:

- Opening a GitHub Security Advisory:
  <https://github.com/xcutiboo/MythicRod/security/advisories/new>
- Or contacting the maintainer through GitHub at
  <https://github.com/xcutiboo>.

Please include:

- Affected MythicRod version and Paper build.
- Reproduction steps or a minimal proof of concept.
- The impact (e.g. crash, data loss, privilege escalation, RCE).

You should receive an initial response within 7 days. A fix or mitigation is
typically prepared on a private branch and released alongside a coordinated
disclosure.

## Scope

In scope:

- The MythicRod plugin source in this repository.
- The default configuration, drops, and language files shipped in the plugin jar.
- The public API surface in the `mythicrod-api` module.

Out of scope:

- Vulnerabilities in Paper, Folia, Adventure, or other upstream dependencies —
  report those to their respective projects.
- Issues that require an operator to grant `mythicrod.admin.*` to untrusted
  players.
- Server misconfiguration unrelated to MythicRod (e.g. open RCON, missing
  firewall rules).
