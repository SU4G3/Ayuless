# Security policy

## Supported versions

Ayuless is pre-alpha. Only the latest commit on `main` gets fixes.

## Reporting a vulnerability

Please **do not** open a public issue. Use GitHub's private reporting:
[Report a vulnerability](https://github.com/SU4G3/Ayuless/security/advisories/new).

Include affected commit, steps to reproduce and impact. Expect a first reply within 7 days.

Things of special interest:

- Locally stored deleted/edited messages leaking (other apps, backups, logs).
- Secret chat or self-destructing message content being stored at all.
- Ghost mode leaking online/read/typing state to the server.

Vulnerabilities in the upstream MTProto client should also be reported to Telegram: https://core.telegram.org/bug-bounty
