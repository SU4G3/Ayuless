# String resources and binary localization

## Issue #53: two independent failures

The startup exception and the incorrect launcher label have different causes.
Neither is fixed by disabling R8 or retaining every Telegram string.

With the namespace at `f5e72cb4`, the stable ID algorithm maps the reported IDs to:

| ID | Key | Consumer |
| --- | --- | --- |
| `0x7f0fe86d` | `InternalNotificationsSystemSetting` | `NotificationsController.checkOtherNotificationsChannel()` |
| `0x7f0ffaaf` | `AyulessLauncherName` | Application label in all five distribution manifests |

`NotificationsController` calls `checkOtherNotificationsChannel()` from its static
initializer on API 26+. That method already correctly uses
`LocaleController.getString(R.string.InternalNotificationsSystemSetting)`. However,
PR #50 added `Resources.getResourceEntryName(stringRes)` to
`LocaleController.getStringV2()` so branding could identify the requested key. A
binary-only string has an R ID but no Android resource table entry after release
shrinking. Name lookup therefore throws `Resources.NotFoundException` before the
binary lookup. A throw during static initialization causes
`ExceptionInInitializerError`; later class access reports reinitialization of a
previously failed class. This source path explains the exact missing ID and startup
failure. The incident APK's R8 mapping was not available to independently resolve
the obfuscated class name `org.telegram.messenger.gs`.

The distribution manifests use `@string/AyulessLauncherName`, not `@string/AppName`.
PR #42 included `ayuless.xml` in binary localization, so `AyulessLauncherName` was
also included in forced discard. The missing label explains Android falling back
to the application class name `org.telegram.messenger.ApplicationLoaderImpl`.

Comparing `42b97322b^:TMessagesProj/src/main/res/values/strings.xml` with the current
resources confirms that #50 moved 99 keys into `values/ayuless.xml`, including
`AppName`, `AppNameBeta`, `AllowReadCall*`, `AppLocked` and
`AllowBackgroundActivityInfo*`. The filename change itself does **not** change
their treatment: both files are inputs to the same task. `AppName` and `AppNameBeta`
were already excluded from forced discard and stable assignment. The missing
manifest ID is **not** evidence that `AppName` is missing. Its current EN default
is still `Ayuless`. None of the moved permission strings needs a real Android
resource merely because it moved files.

## Build pipeline

1. `TMessagesProj` is an Android library. Its normal resource source set still
   compiles both `strings.xml` and `ayuless.xml` with AAPT. The library's
   `org.telegram.build-plugin` only registers
   `GenerateLocalizationUtilsJavaTask`, which generates
   `org.telegram.localization.LocalizationUtils`: a language-tag to asset-name
   mapping. It discovers tags from `values-*/strings.xml`, with English always
   present. It does not strip strings or generate their IDs. Currently RU also
   has `strings.xml`, so its `ayuless.xml` translation is reachable. A future
   language supplied only through `ayuless.xml` would also need to be included in
   this language discovery.
2. `TMessagesProj_App` is the application and depends on that library. Its
   `org.telegram.build-app-plugin` registers `TelegramStringsTask` per variant.
   Default inputs are library `values/strings.xml`, library `values/ayuless.xml`,
   and app `values/strings.xml` when present. Translated inputs include both
   library filenames under `values-*` and app `values-*/strings.xml`.
   Later files overlay earlier entries with the same name.
3. `TelegramStringsTask` unions all input keys and adds synthetic roots for keys
   ending in `_zero`, `_one`, `_two`, `_few`, `_many`, `_other`. It rejects Java
   string-hash collisions. Each localization becomes
   `localization_<normalized_tag>.bin`: little-endian entry count, then entries
   sorted by unsigned Java key hash, each containing the hash and a UTF-8 string
   in Telegram's padded string encoding. Escaped newlines/backslashes are
   normalized. English is the built-in fallback; translations overlay it at
   runtime.
4. The task also writes `stable-ids.txt`. Except for `AppName` and `AppNameBeta`,
   keys are sorted by name and assigned `0x7F0FFFFE - index`. The app plugin passes
   this file to AAPT with `--stable-ids`. An ID is a lookup token, **not** a promise
   of a resource table entry. Adding keys can shift these IDs between builds;
   always decode an incident using its own namespace or generated artifacts.
5. `raw/strings_discard.xml` contains `tools:discard` for the binary namespace,
   excluding the platform strings listed below. This explicitly forces removal
   even when resource usage heuristics would otherwise retain a referenced
   string. Despite a historical comment mentioning generated placeholders, the
   current task does not generate `values/generated.xml`; the actual definitions
   still come from the input resource files.
6. `GenerateStringResourceIdsAssetTask` reads the application's linked
   `RUNTIME_SYMBOL_LIST` and emits `string_resource_ids.bin`: little-endian count
   followed by `(resource ID, Java name hash)` pairs sorted by ID. This is the
   reverse binding needed after shrinking. It includes IDs even when their table
   entries will be discarded. `GenerateLottieMetadataAssetFileTask` uses the same
   symbol list for raw animation metadata and has no string-retention role.
7. The application's `localeFilters += ["zz"]` removes translated Android
   configurations, leaving defaults for real platform resources. Binary assets
   are unaffected. Release/standalone app builds enable minification and resource
   shrinking (including optimized shrinking in `gradle.properties`); debug does
   neither, so a resource-table lookup can appear to work in debug and fail only
   in release. Library `shrinkResources false` does not override the final app's
   shrinker.

At runtime, `Localization` resolves an R ID to a key hash through
`string_resource_ids.bin`, then looks up that hash in the localization assets.
`LocaleController` checks cloud strings first when enabled, then bundled language
strings over English, and applies branding to allowlisted templates before
formatting arguments. `Resources.getString()`, `Context.getText()`,
`Resources.getResourceEntryName()` and Android's XML inflater cannot read these
assets.

## Retained platform strings

`TelegramStringsTask.GENERATED_EXCLUSIONS` is the explicit boundary. These keys are
excluded from `tools:discard` and are now also listed in `tools:keep`:

| Keys | Why Android must be able to resolve them |
| --- | --- |
| `AppName`, `AppNameBeta` | Application identity; `res/xml/auth.xml` uses `AppName` as the account label |
| `AyulessLauncherName` | Application label in release/debug/standalone manifests; default value is `Ayuless` |
| `Settings`, `AccountSettings` | Titles inflated from `res/xml/auth_menu.xml` |
| `EditWidgetContactsInfo`, `EditWidgetChatsInfo` | Widget descriptions in provider XML, read by the system |

`AppName`/`AppNameBeta` remain in EN/RU `ayuless.xml` with the branded values; the
filename has no special retention semantics. The explicit keep rule preserves
their real entries. Other retained keys keep their existing stable assignments;
the stable-ID exclusions remain only `AppName`/`AppNameBeta`.

`AyulessApplicationId` is generated with `variant.resValue` by each distribution
application and used in authenticator/sync XML. It is outside the task's binary
input namespace and forced discard. Other library/platform strings are likewise
unaffected unless their names appear in that namespace. `tools:text` preview-only
references to `VoipAnswerCall`/`VoipDeclineCall` are not runtime consumers and do
not require retention.

## Code changes and codebase audit

- `AppBranding` now matches Java key hashes directly, preserving its 99-key
  allowlist and special app-name/contacts behavior. `Localization.getStringResHash`
  shares the existing asset binding with it. `LocaleController.getStringV2()`
  never asks Android for a discarded entry name. Name-only and ID-only calls
  both work, including cloud templates and formatted arguments.
- `LocaleController.formatString()` previously fell back to
  `ApplicationLoader.applicationContext.getString(res)` after a formatting
  exception. It now uses the bundled English binary localization and applies
  branding before formatting. This keeps malformed cloud templates from sending
  the fallback down another missing-resource path.
- `org.ayuless.nogms.UnavailableMapsProvider` used
  `TextView.setText(R.string.AyulessMapsUnavailable)`, which implicitly calls
  Android Resources. It now passes `LocaleController.getString(...)`.
- All application Java source trees, including distribution-specific sources,
  were searched for direct string/text/quantity reads, resource-name lookups,
  string IDs passed to text/title/dialog/toast APIs, and XML/manifest references.
  Notification channels, shortcut builders and widget `RemoteViews` already
  supply localized text. `GhostShortcut` and Ayuless settings statically import
  `LocaleController.getString`; they are not `Context.getString` calls.
  Settings option arrays store integer IDs and resolve them through
  `LocaleController` when displayed. `AppBranding`'s static
  initialization only builds its regex/hash allowlist, without Android resource
  reads.
- Remaining direct text reads are `android.R.string.copy/cut/paste` in
  `TextSelectionHelper` and Android's own cutout configuration string in
  `NotchInfoUtils`. WebRTC's entry-name reads resolve view IDs, not strings.
  Preference/JSON/cursor `getString()` and editable `getText()` calls do not read
  resources. No additional direct reads of binary-only app strings were found.

For new UI strings, keep EN/RU definitions in `values/ayuless.xml` and
`values-ru/ayuless.xml` and use `LocaleController.getString`/`formatString`.
Passing `R.string.*` to Android's integer text overload is unsafe for binary-only
keys. If Android itself must resolve a key from XML/manifest or another platform
API, add it to the retained set instead. Do not add every UI key to the set or
disable shrinking to mask an incorrect lookup.

## Verification

Run the host regression suite with a JDK:

```sh
JAVA_HOME=$HOME/.gradle/jdks/eclipse_adoptium-21-amd64-linux.2 python3 Tools/test_strings_build.py
```

It checks runtime XML references against the retained set, checks branded EN/RU
names, prints the current IDs of the two incident keys, and compiles/runs the real
`Localization`, `AppBranding` and extracted `LocaleController` lookup/formatting
methods with platform boundary stubs. String-table access deliberately throws.
Cases cover a lookup in a static initializer, ID/name lookup, local/cloud branding,
unknown keys/IDs, contacts server wording, untouched formatted user arguments, and
malformed cloud format fallback. The host suite passed.

The requested offline `:TMessagesProj:compileReleaseJavaWithJavac` command was
attempted with the supplied placeholder credentials and JDK. The Gradle wrapper
failed before compilation because it could not open
`~/.gradle/wrapper/dists/gradle-8.13-bin/5xuhj0ry160q40clulazy9h7d/gradle-8.13-bin.zip.lck`
on the sandbox's read-only filesystem. No full Android compilation or new APK
was produced here. The host suite cannot verify AGP/R8 output or device startup.

On a machine that can build, make a shrunk release APK and use
`aapt2 dump resources app.apk` to confirm all retained keys have real default
values, especially the application label's ID. Check `aapt2 dump xmltree app.apk
--file AndroidManifest.xml` against that ID and ensure the localization/binding
assets are packaged. Then cold-start on API 26+ (with and without an existing
account), confirm the launcher/application label is `Ayuless`, and exercise
notification channels, widgets, settings and shortcuts. These device/APK checks
remain untested in this sandbox.
