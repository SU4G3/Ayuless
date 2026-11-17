# Security policy

## Supported releases

Report vulnerabilities against the latest public Ayuless release. The project
currently has no separate long-term-support branch. Older alpha/test APKs use a
public test signing key; current releases use the dedicated key documented in
[RELEASE-SIGNING.md](docs/RELEASE-SIGNING.md).

## Reporting a vulnerability

Use GitHub's private [Report a vulnerability](https://github.com/SU4G3/Ayuless/security/advisories/new)
form. Include the affected version, device/Android version, impact and minimal
reproduction steps. Do not open a public issue containing an exploit against
users, private messages, session files, login codes, tokens or signing keys.
No response-time guarantee is made.

О проблемах безопасности сообщайте через приватную форму GitHub по ссылке выше.
Не публикуйте сессии, коды входа, токены, ключи или чужую переписку в Issues.

Areas of particular interest include unintended exposure of the local database,
retention despite the corresponding setting being disabled, and ghost controls
sending events that the selected profile should block. Vulnerabilities in the
upstream MTProto client should also be reported to Telegram:
https://core.telegram.org/bug-bounty
