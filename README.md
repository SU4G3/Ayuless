<p align="center">
  <img src="branding/ayuless-icon-rounded-256.png" width="128" alt="Ayuless icon">
</p>

<h1 align="center">Ayuless</h1>

<p align="center">
  A fully open-source unofficial Android messenger client with ghost mode, anti-delete, edit history and message filters.<br>
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
> Pre-alpha. No releases yet, nothing tested on a device. Features below are merged to `main` but unreleased. See [docs/ROADMAP.md](docs/ROADMAP.md).

---

## Русский

### Что это

Ayuless — неофициальный Android-клиент с функциями в духе AyuGram, но полностью открытый: без закрытых бинарников, без Google Play Services и не основанный на exteraGram. Подход тот же, что у AyuGramDesktop: тонкий слой патчей поверх чистых официальных исходников ([DrKLO/Telegram](https://github.com/DrKLO/Telegram)).

Весь собственный код живёт в пакете `org.ayuless`. Правки в исходных файлах минимальны, чтобы ребейз на новые версии апстрима оставался простым.

### Функции v0.1 (в разработке)

Все функции включаются отдельными переключателями в разделе настроек **Ayuless** (по аккаунтам, где это уместно). Путь: **Настройки → Ayuless**. По умолчанию всё выключено. На устройстве пока ничего не тестировалось.

- **Ghost mode** — не отправлять «онлайн», «прочитано», «печатает», просмотры историй; уходить в офлайн сразу после отправки. Профили (общий и по аккаунтам), переключатель «N/5», закрепление отдельных опций, кнопка в списке чатов, предложение ghost mode перед просмотром историй.
- **Отложенная отправка** — сообщения, пересылки, ответы на истории уходят с задержкой.
- **Анти-удаление** — сообщение, удалённое собеседником или сервером, остаётся в чате с пометкой. Сохраняются и вложения (с лимитом размера). Удаление самим пользователем работает как обычно.
- **История правок** — все прошлые версии изменённого сообщения из меню сообщения.
- **Шпион** — локально сохраняет дату прочтения твоих сообщений и время последнего онлайна; исключения для выбранных чатов.
- **Фильтры** — ключевые слова и regex, теневой бан, скрытие сообщений заблокированных; работают и в превью списка чатов, и в уведомлениях.
- **Запрет сохранения** — в группах и каналах можно сохранять, копировать и пересылать копией.
- **Дополнительно** — скрытие спонсорских сообщений, локальный Premium (перевод чата), работа в фоне.
- **Бэкап** — экспорт, импорт и очистка локальной базы.

В работе (открытые PR): секретные чаты, TTL и «посмотреть один раз» (#43, #44, #45), новый вид настроек (#42), вход с подменой клиента (#35). Подробно: [ROADMAP](docs/ROADMAP.md).

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

Правила кода и лицензирования: [CONTRIBUTING.md](CONTRIBUTING.md). Код из exteraGram не используется. Части кода основаны на AyuGram4A (© Radolyn Labs), с сохранением их копирайтов; список файлов в [docs/CREDITS.md](docs/CREDITS.md).

### Благодарности

Спасибо команде [AyuGram](https://github.com/AyuGram) (Radolyn Labs): их клиент вдохновил Ayuless, набор функций и устройство настроек взяты у них за образец. Части кода Ayuless основаны на коде AyuGram4A, авторство сохранено. Спасибо [exteraless](https://github.com/exteraless) за идею названия. Остальные авторы и лицензии: [docs/CREDITS.md](docs/CREDITS.md).

---

## English

### What it is

Ayuless is an unofficial Android client with AyuGram-like features, but fully open source: no proprietary blobs, no Google Play Services, and not based on exteraGram. Same approach as AyuGramDesktop: a thin patch layer on top of clean official sources ([DrKLO/Telegram](https://github.com/DrKLO/Telegram)).

All Ayuless code lives in the `org.ayuless` package. Edits to upstream files are kept minimal so rebasing on new upstream releases stays easy.

### v0.1 features (in progress)

Every feature has its own toggle in the **Ayuless** settings section (per account where it makes sense). Path: **Settings → Ayuless**. Everything is off by default. Nothing has been tested on a device yet.

- **Ghost mode** — don't send online status, read receipts, typing or story views; go offline right after sending. Profiles (global and per account), "N/5" master switch, lockable options, a chat list button, ghost prompt before opening stories.
- **Delayed send** — messages, forwards and story replies are sent after a delay.
- **Anti-delete** — messages deleted by the other side or the server stay in the chat, marked. Attachments are kept too (with a size limit). Deleting your own messages works as usual.
- **Edit history** — all previous versions of an edited message, from the message menu.
- **Spy** — locally saves when your messages were read and users' last online time; exceptions for chosen chats.
- **Filters** — keywords and regex, shadow ban, hide blocked users; also in chat list previews and notifications.
- **Restricted saving** — save, copy and forward as copy in groups and channels.
- **Extras** — hide sponsored messages, local Premium (chat translation), work in background.
- **Backup** — export, import and clear the local database.

In progress (open PRs): secret chats, TTL and view-once (#43, #44, #45), new settings look (#42), login with client spoof (#35). Details: [ROADMAP](docs/ROADMAP.md).

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

Code and licensing rules: [CONTRIBUTING.md](CONTRIBUTING.md). No exteraGram code is used. Parts of the code are based on AyuGram4A (© Radolyn Labs) with their copyright notices kept; the file list is in [docs/CREDITS.md](docs/CREDITS.md).

### Thanks

Thanks to the [AyuGram](https://github.com/AyuGram) team (Radolyn Labs): their client inspired Ayuless, and its feature set and settings layout served as the model. Parts of Ayuless are based on AyuGram4A code, with authorship kept. Thanks to [exteraless](https://github.com/exteraless) for the name idea. Other authors and licenses: [docs/CREDITS.md](docs/CREDITS.md).

---

## License

Ayuless as a whole is distributed under **GPL-3.0-or-later** ([LICENSE](LICENSE)). Upstream sources are "GPL v2 or later" ([LICENSE.GPL-2.0](LICENSE.GPL-2.0)), which permits this. Details and third-party compatibility: [docs/LICENSE-AUDIT.md](docs/LICENSE-AUDIT.md).

Icons: [Solar Icons Set](https://www.figma.com/community/file/1166831539721848736/solar-icons-set) by 480 Design and R4IN80W, licensed under [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/). Full credits: [docs/CREDITS.md](docs/CREDITS.md).

Original upstream README: [docs/UPSTREAM_README.md](docs/UPSTREAM_README.md).
