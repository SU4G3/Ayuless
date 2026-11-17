# Подготовка релиза F-Droid

Current submission status (2026-11-17): [F-Droid packaging request #4819](https://gitlab.com/fdroid/rfp/-/work_items/4819) is submitted and verified. [Metadata MR !57850](https://gitlab.com/fdroid/fdroiddata/-/merge_requests/57850) is submitted and linked to that request. The MR was closed over template and maintenance-history requirements; resubmission is deferred. Ayuless 1.0.0 is published on GitHub, and its application source is now integrated into main through GitHub PR #62. Earlier submission/CI-pending statements describe historical preparation.

Состояние на 2026-11-17: полная сборка по рецепту и проверки APK прошли.
Пользователь сообщил, что новый тестовый APK работает («ну ворк вроде»).
Это общий результат проверки, не подтверждение каждого пункта ниже по отдельности.
Тег `fdroid-13.2.1-72919` опубликован на проверенном исходном коммите.
Рецепт включён, описание заявки подготовлено в [SUBMISSION.md](SUBMISSION.md).
Отправка в GitLab ожидает подключения браузера и входа владельца.
Принятие F-Droid пока не запрашивалось и не гарантируется.

## Что тестировать

[Скачать Ayuless-test-fdroid-final.apk](https://github.com/SU4G3/Ayuless-reports/releases/download/test-cleanroom/Ayuless-test-fdroid-final.apk)
из приватного репозитория владельца. Версия 13.2.1 (72919), около 97.6 МиБ.
SHA-256: `e3e221b1cc6426dd9983c8bcb16269935af3d045e47b84b33329dc5fe1eba2de`.

Копия подписана прежним тестовым ключом Telegram. apksigner подтверждает v1/v2/v3;
SHA-256 сертификата: `a08d7dc323ddf71ef3201944397e0d3cce7d40847263e11f328b68bbe19229ab`.
Это тестовая подпись, не будущая подпись F-Droid.

На этом APK проверить:

1. Установку/обновление и запуск; вход на тестовом аккаунте без удаления основного.
2. Мини-приложения и основные функции Ayuless.
3. Noto-эмодзи и рукопожатия с разными тонами кожи.
4. UnifiedPush с ntfy: регистрацию, доставку при заблокированном экране,
   второй аккаунт, отключение и повторную регистрацию дистрибьютора.
5. Карты/спутник/гибрид и видимую атрибуцию.

Предыдущие сообщения пользователя о рабочем ntfy, включая заблокированный экран,
сохранены, но точный старый артефакт неизвестен. Нужна регрессия на этом файле.

## Проверенная сборка

Исходники: `afadbaf8fe909ff11e2b2f5086c153ac5dd28c46`, PR #62; включает UnifiedPush,
Noto с исходными SVG, пять выбранных скриншотов и packaged notices.
Рецепт собран в официальном образе F-Droid buildserver-trixie через rootless Podman:

```sh
fdroid build --on-server --test --scan-binary --verbose org.ayuless.messenger:72919
```

Это внутренний режим официального CI-образа, не запуск Vagrant VM через `--server`.
Подготовка, source scan, 64 нативных архива, WidgetJS/QuickJS, APK и бинарные проверки
завершились с кодом 0. Идентификатор/версия, четыре ABI, все 3606 эмодзи, 36 тонов
рук и 47 файлов лицензий проверены на итоговом APK. Подробности и хеш unsigned APK:
[VALIDATION-2026-11-17.md](VALIDATION-2026-11-17.md).

Две чистые APK-сборки не сравнивались. Полная сборка без сети не проверялась;
отдельная сборка WAMR с `--network none` прошла. Готовый unsigned APK, source tarball
и логи сохранены локально. Автоматического фонового издателя APK нет.

## Ресурсы и идентификация

Roboto/Mono/Merriweather, Noto и Solar разобраны, известные лицензии сохранены.
Шрифты gram/num, OCR, PhoneFormats/shapes и 366 моделей/анимаций/звуков совпадают
с исходниками версии Forkgram из рецепта F-Droid. Это существенный прецедент;
обязательная замена этих ресурсов не объявляется. Отдельное авторство/гранты
не всех файлов установлены; исходный RMPhoneFormat описывает получение базы из
Apple SDK. Факты и сравнения сохранены в [ASSET-REVIEW-2026-11-17.md](ASSET-REVIEW-2026-11-17.md).
Нельзя приравнивать отсутствие индивидуального уведомления к доказанной несвободе.

Выбранный владельцем API ID 4, fingerprint и LoginSpoof сохранены и раскрыты
для ревью. Новая регистрация API ID не объявляется обязательной без конкретного
требования. Владельцу не нужно получать отдельное письмо для каждого GPL-файла.

## После успешного теста

1. Зафиксировать результат проверки именно указанного SHA-256.
2. Завершить ревью PR #62, выбрать финальный исходный коммит и создать новый тег
   `fdroid-13.2.1-72919`. Старые теги не передвигать. Если меняется код/сборочные
   входы относительно проверенного кандидата, проверить изменения и пересобрать.
3. Убрать `disable` в копии рецепта, указать финальный тег/коммит. Оставить
   `AutoUpdateMode: None` для первого ревью; проверить `fdroid readmeta` и `fdroid lint`.
4. Через аккаунт GitLab владельца открыть MR в `fdroid/fdroiddata` с
   `metadata/org.ayuless.messenger.yml`, ссылками на тег, лог и аудит.
   Обязательные сервисы раскрыты через NonFreeNet, спонсорские сообщения — Ads.
5. Ответить на замечания сопровождающих. F-Droid соберёт APK и подпишет своим
   ключом; обновление поверх тестовой подписи обычным способом не установится.
   Перед сменой установки сохранить нужные локальные данные отдельно.

Текст заявки:

> Add Ayuless (`org.ayuless.messenger`), a GPL-3.0-or-later Telegram Android fork
> with ghost controls, local message retention, edit history and UnifiedPush.
> Native libraries and WidgetJS/QuickJS bytecode are built from pinned source.
> Emoji use Noto artwork with retained SVG sources and packaged notices.
> The official buildserver-image recipe build and APK scan pass. Required Telegram
> service and optional sponsored messages are disclosed as NonFreeNet and Ads.
> See the release tag, build log and licence/provenance review.

Перед отправкой добавить реальные ссылки на финальный тег и лог.
