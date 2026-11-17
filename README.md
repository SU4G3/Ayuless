<p align="center">
  <img src="branding/ayuless-icon-rounded-256.png" width="128" alt="Ayuless icon">
</p>

<h1 align="center">Ayuless</h1>

<p align="center">
  An open-source unofficial Telegram client for Android with ghost mode, anti-delete, edit history and message filters.<br>
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
> Ранняя версия: [v0.1.0-alpha.2](https://github.com/SU4G3/Ayuless/releases/tag/v0.1.0-alpha.2), проверена на одном устройстве. Полная проверка функций ещё не завершена. Изменения из открытого PR #62 не входят в этот APK.
>
> Early version: [v0.1.0-alpha.2](https://github.com/SU4G3/Ayuless/releases/tag/v0.1.0-alpha.2), tested on one device. Full feature validation is pending. Changes in open PR #62 are not included in this APK.

---

## Русский

### Что это

Ayuless — неофициальный Android-клиент с функциями в духе AyuGram, с открытым исходным кодом, без Google Play Services. Основан на официальном Telegram, не на exteraGram. Подход тот же, что у AyuGramDesktop: тонкий слой патчей поверх чистых официальных исходников ([DrKLO/Telegram](https://github.com/DrKLO/Telegram)).

Весь собственный код живёт в пакете `org.ayuless`. Правки в исходных файлах минимальны, чтобы ребейз на новые версии апстрима оставался простым.

### Скачать

[Скачать APK v0.1.0-alpha.2](https://github.com/SU4G3/Ayuless/releases/download/v0.1.0-alpha.2/Ayuless-v0.1.0-alpha.2.apk) · [Все релизы](https://github.com/SU4G3/Ayuless/releases)

ID приложения: `org.ayuless.messenger`. Тестовые APK для разработки размещаются
в [приватном репозитории отчётов](https://github.com/SU4G3/Ayuless-reports/releases/tag/test-cleanroom)
и доступны участникам с правами доступа. В F-Droid приложение пока не опубликовано.

### Функции

Все функции включаются отдельными переключателями в разделе настроек **Ayuless** (по аккаунтам, где это уместно). Путь: **Настройки → Ayuless**. Анти-удаление, сохранение вложений, скрытие спонсорских сообщений и работа в фоне включены по умолчанию; дополнительные функции выключены. Явно сохранённые настройки сохраняются при обновлении.

- **Ghost mode** — не отправлять «онлайн», «прочитано», «печатает», просмотры историй; уходить в офлайн сразу после отправки. Профили (общий и по аккаунтам), переключатель «N/5», закрепление отдельных опций, кнопка в списке чатов, предложение ghost mode перед просмотром историй.
- **Отложенная отправка** — сообщения, пересылки и ответы на истории уходят с задержкой; в секретных чатах задержка выполняется локально.
- **Анти-удаление** — сообщение, удалённое собеседником или сервером, остаётся в чате с пометкой. Сохраняются и вложения (с лимитом размера). Удаление самим пользователем работает как обычно.
- **Дополнительное сохранение** — отдельные настройки для секретных чатов, TTL / автоудаления и медиа «посмотреть один раз».
- **История правок** — все прошлые версии изменённого сообщения из меню сообщения.
- **Шпион** — локально сохраняет дату прочтения твоих сообщений и время последнего онлайна; исключения для выбранных чатов.
- **Фильтры** — ключевые слова и regex, теневой бан, скрытие сообщений заблокированных; работают и в превью списка чатов, и в уведомлениях.
- **Запрет сохранения** — в группах и каналах можно сохранять, копировать и пересылать копией.
- **Дополнительно** — скрытие спонсорских сообщений, локальный Premium (перевод чата), работа в фоне.
- **Инструменты чата** — меню Ayuless для отдельного чата, просмотр сохранённых удалённых сообщений, очистка и экспорт переписки в HTML/JSON; действие «Прочитать» в меню сообщения.
- **Карты** — выбор обычной, спутниковой или гибридной карты.
- **Профили** — ID и примерная дата регистрации пользователя или дата создания чата.
- **Бэкап** — экспорт, импорт и очистка локальной базы.

Секретные чаты, TTL, медиа «посмотреть один раз», обновлённые настройки и исправление fingerprint при входе уже в `main`. Пользователь подтвердил работу предыдущего тестового APK и мини-приложений; это не полная проверка всех функций. Текущий план: [ROADMAP](docs/ROADMAP.md).

### Открытость

Код проекта распространяется под GPL-3.0-or-later. Проверка происхождения и лицензий отдельных унаследованных ресурсов продолжается: [аудит](docs/LICENSE-AUDIT.md). Для работы нужны несвободные серверы Telegram.

- Все нативные библиотеки (BoringSSL, FFmpeg, libvpx, dav1d и др.) собираются из исходников.
- Без Google Play Services, Firebase и ML Kit. Карты через [MapLibre](https://maplibre.org); поддержка [UnifiedPush](https://unifiedpush.org) добавлена в PR #30 и объединена с этой F-Droid-веткой. Выбор дистрибьютора: **Настройки → Ayuless → Уведомления**. Пользователь подтвердил доставку в тестовом артефакте; публичный APK alpha.2 не содержит этот PR.

### Сборка

> Ниже обычная сборка из исходников. Полная сборка по рецепту в официальном образе F-Droid и проверки APK прошли. Воспроизводимость двух чистых APK не установлена. Подробности: [проверки](docs/fdroid/VALIDATION-2026-11-17.md).

Нужно: JDK 17, Android SDK 36, NDK 27.2.12479018. Перед Gradle собери native-библиотеки для всех четырёх ABI; инструменты и подробности — в [docs/NATIVE.md](docs/NATIVE.md). Готовые `.a` в Git не хранятся.

```bash
git clone --recursive --shallow-submodules https://github.com/SU4G3/Ayuless.git
cd Ayuless
cp API_KEYS.example API_KEYS   # впиши свои APP_ID / APP_HASH с https://my.telegram.org/apps
export ANDROID_SDK_ROOT=~/Android/Sdk
Tools/native/build-prebuilts.sh
./gradlew assembleAfatRelease
```

Файл `API_KEYS` в git-ignore и никогда не коммитится. APK появится в `TMessagesProj_App/build/outputs/apk/afat/release/app.apk`. ID приложения: `org.ayuless.messenger`.

### F-Droid

Подан [запрос на включение #4819](https://gitlab.com/fdroid/rfp/-/work_items/4819). Проверенный [рецепт сборки](docs/fdroid/org.ayuless.messenger.yml) включён; тег `fdroid-13.2.1-72919` указывает на исходники протестированного APK. Публикация в F-Droid ожидает ревью; отдельный MR пока ждёт импорта форка GitLab. Метаданные EN/RU находятся в `fastlane/metadata/android/`. Приложение требует несвободные серверы Telegram; UnifiedPush #30 для подачи не обязателен.


В [PR #62](https://github.com/SU4G3/Ayuless/pull/62) подготовлены сборка WidgetJS и
QuickJS builtins из исходников, профиль API ID 4 с соответствующим fingerprint
и замена Apple-эмодзи на Noto с исходниками SVG и лицензиями. Локальные проверки
ресурсов и компиляция прошли; итоговый APK с Noto и работа на устройстве ещё не
проверены. Остаются проверка прав на остальные ресурсы, review идентификации
при входе, полная серверная сборка и проверка воспроизводимости.

Порядок завершения релиза и подачи: [RELEASE.md](docs/fdroid/RELEASE.md).

### Участие

Правила кода и лицензирования: [CONTRIBUTING.md](CONTRIBUTING.md). Код из exteraGram не используется. Порт на основе AyuGram удалён, а функции заново реализованы независимо по письменной спецификации [docs/CLEANROOM-SPEC.md](docs/CLEANROOM-SPEC.md), без доступа к исходникам AyuGram. Более ранняя история Git (до этого изменения) всё ещё содержит прежний порт.

### Благодарности

Спасибо команде [AyuGram](https://github.com/AyuGram) (Radolyn Labs): их клиент вдохновил Ayuless, набор функций и устройство настроек взяты у них за образец. Спасибо [exteraless](https://github.com/exteraless) за идею названия. Остальные авторы и лицензии: [docs/CREDITS.md](docs/CREDITS.md).

---

## English

### What it is

Ayuless is an unofficial Android client with AyuGram-like features, with open source code and no Google Play Services. It is based on official Telegram, rather than exteraGram. Same approach as AyuGramDesktop: a thin patch layer on top of clean official sources ([DrKLO/Telegram](https://github.com/DrKLO/Telegram)).

All Ayuless code lives in the `org.ayuless` package. Edits to upstream files are kept minimal so rebasing on new upstream releases stays easy.

### Download

[Download APK v0.1.0-alpha.2](https://github.com/SU4G3/Ayuless/releases/download/v0.1.0-alpha.2/Ayuless-v0.1.0-alpha.2.apk) · [All releases](https://github.com/SU4G3/Ayuless/releases)

Application ID: `org.ayuless.messenger`. Development test APKs are shared through
the [private reports repository](https://github.com/SU4G3/Ayuless-reports/releases/tag/test-cleanroom)
with collaborators who have access. Ayuless is not yet available in F-Droid.

### Features

Every feature has its own toggle in the **Ayuless** settings section (per account where it makes sense). Path: **Settings → Ayuless**. Anti-delete, saved attachments, sponsored-message hiding and background work default on; optional additions default off. Explicit saved choices are preserved across updates.

- **Ghost mode** — don't send online status, read receipts, typing or story views; go offline right after sending. Profiles (global and per account), "N/5" master switch, lockable options, a chat list button, ghost prompt before opening stories.
- **Delayed send** — messages, forwards and story replies are sent after a delay; secret-chat delays are handled locally.
- **Anti-delete** — messages deleted by the other side or the server stay in the chat, marked. Attachments are kept too (with a size limit). Deleting your own messages works as usual.
- **Optional retention** — separate settings for secret chats, TTL / auto-delete messages and view-once media.
- **Edit history** — all previous versions of an edited message, from the message menu.
- **Spy** — locally saves when your messages were read and users' last online time; exceptions for chosen chats.
- **Filters** — keywords and regex, shadow ban, hide blocked users; also in chat list previews and notifications.
- **Restricted saving** — save, copy and forward as copy in groups and channels.
- **Extras** — hide sponsored messages, local Premium (chat translation), work in background.
- **Chat tools** — a per-chat Ayuless menu, saved deleted messages, cleanup and HTML/JSON chat export; a message-menu action to mark messages as read.
- **Maps** — choose map, satellite or hybrid view.
- **Profiles** — IDs and approximate user registration dates or chat creation dates.
- **Backup** — export, import and clear the local database.

Secret chats, TTL, view-once media, updated settings and the login fingerprint fix are merged into `main`. The user confirmed that an earlier test APK and mini-apps worked; this is not a complete feature test. Current plan: [ROADMAP](docs/ROADMAP.md).

### Openness

Project code is distributed under GPL-3.0-or-later. Provenance and licence review of individual inherited resources is ongoing: [audit](docs/LICENSE-AUDIT.md). The app requires Telegram’s proprietary servers.

- All native libraries (BoringSSL, FFmpeg, libvpx, dav1d, etc.) are built from source.
- No Google Play Services, Firebase or ML Kit. Maps via [MapLibre](https://maplibre.org); [UnifiedPush](https://unifiedpush.org) support is implemented in PR #30 and integrated into this F-Droid branch. Select a distributor in **Settings → Ayuless → Notifications**. The user reported delivery in a test artifact; the public alpha.2 APK does not include this PR.

### Building

> These are the normal source-build steps. The full recipe build in the official F-Droid image and APK checks pass. Two-clean-APK reproducibility has not been established. See the [validation notes](docs/fdroid/VALIDATION-2026-11-17.md).

Requires JDK 17, Android SDK 36, NDK 27.2.12479018. Build native libraries for all four ABIs before Gradle; host tools and details are in [docs/NATIVE.md](docs/NATIVE.md). No `.a` archives are stored in Git.

```bash
git clone --recursive --shallow-submodules https://github.com/SU4G3/Ayuless.git
cd Ayuless
cp API_KEYS.example API_KEYS   # put your own APP_ID / APP_HASH from https://my.telegram.org/apps
export ANDROID_SDK_ROOT=~/Android/Sdk
Tools/native/build-prebuilts.sh
./gradlew assembleAfatRelease
```

`API_KEYS` is git-ignored and must never be committed. The APK ends up in `TMessagesProj_App/build/outputs/apk/afat/release/app.apk`. Application ID: `org.ayuless.messenger`.

### F-Droid

[Packaging request #4819](https://gitlab.com/fdroid/rfp/-/work_items/4819) is submitted. The validated [build recipe](docs/fdroid/org.ayuless.messenger.yml) is enabled, and `fdroid-13.2.1-72919` tags the tested APK source. F-Droid publication awaits review; a separate metadata MR awaits GitLab fork import. EN/RU store metadata lives in `fastlane/metadata/android/`. The app requires Telegram's proprietary servers; UnifiedPush #30 is not required for submission.


[PR #62](https://github.com/SU4G3/Ayuless/pull/62) prepares source-built WidgetJS
and QuickJS builtins, the selected API ID 4 profile with its matching fingerprint,
and Noto emoji with SVG sources and licences in place of Apple-derived artwork.
Local asset checks and compilation passed; the final Noto APK and device rendering
remain unverified. Remaining work includes other resource rights, login-identity
review, a complete server build and reproducibility verification.

Release completion and submission steps: [RELEASE.md](docs/fdroid/RELEASE.md).

### Contributing

Code and licensing rules: [CONTRIBUTING.md](CONTRIBUTING.md). No exteraGram code is used. The AyuGram-derived port was removed, and the features were re-implemented independently from the written specification [docs/CLEANROOM-SPEC.md](docs/CLEANROOM-SPEC.md), without access to AyuGram sources. Older Git history (before this change) still contains the earlier port.

### Thanks

Thanks to the [AyuGram](https://github.com/AyuGram) team (Radolyn Labs): their client inspired Ayuless, and its feature set and settings layout served as the model. Thanks to [exteraless](https://github.com/exteraless) for the name idea. Other authors and licenses: [docs/CREDITS.md](docs/CREDITS.md).

---

## License

Ayuless as a whole is distributed under **GPL-3.0-or-later** ([LICENSE](LICENSE)). Upstream sources are "GPL v2 or later" ([LICENSE.GPL-2.0](LICENSE.GPL-2.0)), which permits this. Details and third-party compatibility: [docs/LICENSE-AUDIT.md](docs/LICENSE-AUDIT.md).

Icons: [Solar Icons Set](https://www.figma.com/community/file/1166831539721848736/solar-icons-set) by 480 Design and R4IN80W, licensed under [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/). Full credits: [docs/CREDITS.md](docs/CREDITS.md).

Original upstream README: [docs/UPSTREAM_README.md](docs/UPSTREAM_README.md).
