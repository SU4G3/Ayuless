<p align="center">
  <img src="branding/ayuless-icon-rounded-256.png" width="128" alt="Ayuless icon">
</p>

<h1 align="center">Ayuless</h1>

<p align="center">
  A fully open-source unofficial Android messenger client with ghost mode, anti-delete and edit history.<br>
  <a href="#русский">Русский</a> · <a href="#english">English</a>
</p>

<p align="center">
  <a href="https://github.com/SU4G3/Ayuless/actions/workflows/build.yml"><img src="https://github.com/SU4G3/Ayuless/actions/workflows/build.yml/badge.svg?branch=main" alt="Build"></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/license-GPL--3.0--or--later-blue" alt="License: GPL-3.0-or-later"></a>
  <img src="https://img.shields.io/badge/status-pre--alpha-orange" alt="Status: pre-alpha">
</p>

> [!IMPORTANT]
> **Неофициальный клиент. Не связан с Telegram, AyuGram и Radolyn Labs. Идея названия от exteraless.**
>
> **Unofficial client. Not affiliated with Telegram, AyuGram or Radolyn Labs. Name idea from exteraless.**

> [!WARNING]
> Pre-alpha. No releases yet. Features below are the plan for v0.1, not shipped functionality. See [docs/ROADMAP.md](docs/ROADMAP.md).

---

## Русский

### Что это

Ayuless — неофициальный Android-клиент с функциями в духе AyuGram, но полностью открытый: без закрытых бинарников, без Google Play Services и не основанный на exteraGram. Подход тот же, что у AyuGramDesktop: тонкий слой патчей поверх чистых официальных исходников ([DrKLO/Telegram](https://github.com/DrKLO/Telegram)).

Весь собственный код живёт в пакете `org.ayuless`. Правки в исходных файлах минимальны, чтобы ребейз на новые версии апстрима оставался простым.

### Функции v0.1 (в разработке)

Все функции включаются отдельными переключателями в разделе настроек **Ayuless** (по аккаунтам, где это уместно).

- **Ghost mode** — не отправлять статус «онлайн», «прочитано», «печатает», не отмечать просмотры историй; опция «уйти в офлайн сразу после отправки».
- **Анти-удаление** — сообщение, удалённое собеседником или сервером, остаётся в чате и выделяется серым. Удаление самим пользователем работает как обычно.
- **История правок** — все прошлые версии изменённого сообщения доступны из меню сообщения.
- **Секретные чаты и самоуничтожающиеся сообщения** никогда не сохраняются.
- **Бэкап** локальной истории удалённых и изменённых сообщений (опционально).

### Открытость

- Все нативные библиотеки (BoringSSL, FFmpeg, libvpx, dav1d и др.) собираются из исходников.
- Без Google Play Services, Firebase и ML Kit. Пуши через [UnifiedPush](https://unifiedpush.org), карты через [MapLibre](https://maplibre.org).

### Сборка

> Пока идёт этап отвязки от Google-зависимостей, инструкция может не работать полностью. Статус: [ROADMAP](docs/ROADMAP.md).

Нужно: JDK 17, Android SDK 36, NDK 27.2.12479018.

```bash
git clone --recursive --shallow-submodules https://github.com/SU4G3/Ayuless.git
cd Ayuless
cp API_KEYS.example API_KEYS   # впиши свои APP_ID / APP_HASH с https://my.telegram.org/apps
./gradlew assembleAfatRelease
```

Файл `API_KEYS` в git-ignore и никогда не коммитится. APK появится в `TMessagesProj_App/build/outputs/apk/afat/release/app.apk`. ID приложения: `org.ayuless.messenger`.

### Участие

Правила кода и лицензирования: [CONTRIBUTING.md](CONTRIBUTING.md). Код из AyuGram4A, exteraGram и AyuGramDesktop не копируется — логика реализуется самостоятельно.

---

## English

### What it is

Ayuless is an unofficial Android client with AyuGram-like features, but fully open source: no proprietary blobs, no Google Play Services, and not based on exteraGram. Same approach as AyuGramDesktop: a thin patch layer on top of clean official sources ([DrKLO/Telegram](https://github.com/DrKLO/Telegram)).

All Ayuless code lives in the `org.ayuless` package. Edits to upstream files are kept minimal so rebasing on new upstream releases stays easy.

### v0.1 features (in progress)

Every feature has its own toggle in the **Ayuless** settings section (per account where it makes sense).

- **Ghost mode** — don't send online status, read receipts, typing status or story views; optional "go offline right after sending".
- **Anti-delete** — messages deleted by the other side or the server stay in the chat, greyed out. Deleting your own messages works as usual.
- **Edit history** — all previous versions of an edited message, available from the message menu.
- **Secret chats and self-destructing messages** are never stored.
- **Backup** of the local deleted/edited history (optional).

### Openness

- All native libraries (BoringSSL, FFmpeg, libvpx, dav1d, etc.) are built from source.
- No Google Play Services, Firebase or ML Kit. Push via [UnifiedPush](https://unifiedpush.org), maps via [MapLibre](https://maplibre.org).

### Building

> De-Googling is in progress, so these steps may not fully work yet. Status: [ROADMAP](docs/ROADMAP.md).

Requires JDK 17, Android SDK 36, NDK 27.2.12479018.

```bash
git clone --recursive --shallow-submodules https://github.com/SU4G3/Ayuless.git
cd Ayuless
cp API_KEYS.example API_KEYS   # put your own APP_ID / APP_HASH from https://my.telegram.org/apps
./gradlew assembleAfatRelease
```

`API_KEYS` is git-ignored and must never be committed. The APK ends up in `TMessagesProj_App/build/outputs/apk/afat/release/app.apk`. Application ID: `org.ayuless.messenger`.

### Contributing

Code and licensing rules: [CONTRIBUTING.md](CONTRIBUTING.md). No code is copied from AyuGram4A, exteraGram or AyuGramDesktop; behaviour is reimplemented independently.

---

## License

Ayuless as a whole is distributed under **GPL-3.0-or-later** ([LICENSE](LICENSE)). Upstream sources are "GPL v2 or later" ([LICENSE.GPL-2.0](LICENSE.GPL-2.0)), which permits this. Details and third-party compatibility: [docs/LICENSE-AUDIT.md](docs/LICENSE-AUDIT.md).

Original upstream README: [docs/UPSTREAM_README.md](docs/UPSTREAM_README.md).
