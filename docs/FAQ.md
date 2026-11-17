# Ayuless FAQ

## Downloads / Скачивание

**Where is the official APK? / Где официальный APK проекта?**

Use [GitHub Releases](https://github.com/SU4G3/Ayuless/releases/latest).
Project announcements: [@ayulessapp](https://t.me/ayulessapp). Ayuless is an
unofficial Telegram client; “official APK” here means the project's own release.

**Why does Android show 13.2.1 instead of 1.0.0? / Почему версия 13.2.1?**

Ayuless 1.0.0 is the project release name. Android retains Telegram base
versionName 13.2.1 and afat versionCode 72919. Future updates must increase the
Android versionCode as well as use the intended Ayuless release name.

**Can I keep official Telegram installed? / Можно оставить официальный Telegram?**

Yes. Ayuless uses `org.ayuless.messenger`, a separate application ID.
Да, это отдельное приложение со своим ID.

## Updating / Обновление

**Why won't 1.0.0 install over an alpha/test APK? / Почему APK не обновляется?**

It uses a new private signing key. Export the local Ayuless database and saved
attachments before uninstalling the old APK. Uninstalling deletes local app data
and secret chats; cloud chats remain on Telegram's servers. Later public releases
must preserve the new signing identity. [Signing details](RELEASE-SIGNING.md).

В 1.0.0 сменился ключ подписи. Перед удалением старой версии экспортируйте базу
Ayuless и вложения. Локальные данные и секретные чаты при удалении потеряются;
облачные чаты вернутся после входа.

**Can anti-delete recover old messages? / Можно восстановить неполученные сообщения?**

No. It can retain content received by this installation, not fetch content the
device never received. Нет: сохраняется только содержимое, полученное устройством.

## Notifications / Уведомления

Choose a compatible UnifiedPush distributor, such as ntfy, under
**Settings → Ayuless → Notifications**. Without one, the client uses its background
connection. If delivery fails, report the Android version, distributor/version,
battery restrictions and whether it fails with the screen locked. Android
force-stop is different from closing the app normally.

Выберите дистрибьютор UnifiedPush в **Настройки → Ayuless → Уведомления**.
При проблемах укажите устройство, версию дистрибьютора, ограничения батареи и
поведение на заблокированном экране.

## Security warnings / Предупреждения защиты

An “unknown app” prompt is different from a malware detection. Record the exact
warning, phone model, security product and APK download URL/hash. A signing key
alone cannot guarantee that antivirus products will accept an app. Suspected
false positives can be reviewed by the relevant vendor; do not assume every
warning is false or ask users to disable protection globally.

Предупреждение «неизвестное приложение» и вердикт «вредоносное» — разные случаи.
Для разбора нужен точный текст, модель телефона, название защиты и источник APK.
[Google's developer guidance](https://developers.google.com/android/play-protect/warning-dev-guidance).

## F-Droid

Ayuless is not published in the official F-Droid catalog. [MR !57850](https://gitlab.com/fdroid/fdroiddata/-/merge_requests/57850)
was closed because the required template was not followed and the new project
had not met the maintenance-history requirement. The [inclusion policy](https://f-droid.org/en/docs/Inclusion_Policy/#quality-control)
asks for at least three months of active development, with stated exceptions,
including evidence of existing users. Acceptance is not automatic after a date.
The source-build recipe and reports are retained for a future submission.

Включение в F-Droid отложено. Первая заявка закрыта из-за шаблона MR и возраста
проекта; правило допускает исключения, но решение остаётся за сопровождающими.
Это не мешает пользоваться GitHub-релизами.

## Reporting a bug / Сообщить об ошибке

Open a [bug report](https://github.com/SU4G3/Ayuless/issues/new?template=bug_report.yml)
with the Ayuless release and Android version, device, installation source and
reproduction steps. Remove private messages, API credentials, login codes,
sessions and tokens from screenshots/logs. Report security vulnerabilities
privately through [Security → Report a vulnerability](https://github.com/SU4G3/Ayuless/security/advisories/new).
