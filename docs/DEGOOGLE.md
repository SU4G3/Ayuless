<!-- SPDX-License-Identifier: GPL-3.0-or-later
     Copyright (C) 2026 Ayuless contributors -->

# Building Ayuless without proprietary Google SDKs

Issue #2 targets `:TMessagesProj_App:compileAfatReleaseJavaWithJavac` and the
`afatRelease` APK. New Java adapters live in `org.ayuless.nogms` and
`org.ayuless.maps`; upstream changes are marked `Ayuless:`. These adapters are
original implementations. No code was
copied from other Telegram forks.

## Removed dependencies

| Dependency removed | Replacement / unavailable feature |
| --- | --- |
| `com.google.android.gms:play-services-cast-framework:21.4.0` | Cast discovery, sessions and playback unavailable. `Cast` adapters retain optional UI types; Cast menus are disabled. No Cast HTTP server is started by the cover path. |
| `com.google.android.gms:play-services-maps:18.1.0` | `org.ayuless.maps.MapLibreMapsProvider`, using `org.maplibre.gl:android-sdk:11.8.0`, renders interactive maps, live markers and proximity circles. Keyless OpenFreeMap Liberty tiles retain MapLibre's OSM attribution control. Android platform location supplies the location dot and callbacks. |
| `com.google.android.gms:play-services-auth:20.4.0` | Google sign-in hidden by the services capability check; local adapters fail requests. SMS Retriever registration removed; manual code entry remains. |
| `com.google.android.gms:play-services-vision:20.1.3` | `Vision` uses the existing ZXing library for bitmap/NV21 QR and raw barcode decoding. Face detection and automatic face-mask placement unavailable. Structured North American driver-license parsing unavailable; native MRZ recognition remains. |
| `com.google.android.gms:play-services-wearable:18.0.0` | Wear auth listener removed from manifests and Java source set; outgoing Wear messages fail locally. Android notification reply support remains. |
| `com.google.android.gms:play-services-location:21.0.1` | `PlatformLocationProvider` uses Android `LocationManager`, GPS/network/passive providers, existing permissions and listener cleanup. |
| `com.google.android.gms:play-services-wallet:19.1.0` | Google Pay initialization disabled; adapters never launch payments. Existing invoice/card payment UI remains. |
| `com.google.android.gms:play-services-safetynet:18.0.1` | No attestation; Firebase SMS challenges request an alternative server code. SafetyNet key cleared. |
| `com.google.android.gms:play-services-mlkit-subject-segmentation:16.0.0-beta1` | Automatic subject extraction unavailable. Sticker segmentation completes with no subjects; photo segmentation marks itself unavailable. No model download/retry or emulator mock segmentation. Manual editing remains. |
| `com.google.android.gms:play-services-mlkit-image-labeling:16.0.8` | Automatic object labels / suggested object emoji unavailable. |
| `com.google.mlkit:language-id:17.0.6` | `LanguageDetector.hasSupport()` returns false; direct detection returns `und`. Existing server translation remains. The already-commented ML Kit translation coordinate was also removed. |
| `com.google.firebase:firebase-bom:34.19.0` | No Firebase platform dependency. |
| `com.google.firebase:firebase-messaging` | FCM service excluded and manifest registration removed. `org.ayuless.push.UnifiedPushProvider` registers a UnifiedPush endpoint (`org.unifiedpush.android:connector:3.3.5`) with the server as a Web Push token (type 10); `AyulessPushService` passes decrypted payloads to `PushListenerController.processRemoteMessage`. Without a distributor the keep-alive background connection is used. See [Push notifications](#push-notifications-unifiedpush). |
| `com.google.firebase:firebase-pnv` | `SUPPORTS_FPNV = false`; `PhoneVerification` reports no supported SIMs and fails verification. Existing passkey/manual login fallback remains. |
| `com.google.firebase:firebase-appindexing:20.0.0` | Assistant indexing acknowledgements removed; normal intent/link handling remains. |
| `com.google.firebase:firebase-crashlytics` (HockeyApp module) | Crashlytics calls removed; existing local logging remains. |
| `com.google.android.play:integrity:1.3.0` | Native integrity requests complete with `PLAYINTEGRITY_FAILED_UNAVAILABLE`. Login requests an alternative SMS flow. No credentials or successful attestations are fabricated. |
| `com.android.billingclient:billing:8.0.0` | `IS_BILLING_UNAVAILABLE = true`, invoice billing selected. `Billing` types keep existing purchase UI compilable; queries complete unavailable with empty results, purchases/consumption fail. No Play purchase or restore. |
| `androidx.credentials:credentials-play-services-auth:1.6.0-alpha04` | Open-source `androidx.credentials:credentials` remains for platform credential support; Google Play credential provider unavailable. |
| `com.google.android.recaptcha:recaptcha:18.7.1` | Captcha requests complete with `RECAPTCHA_FAILED_UNAVAILABLE`. Server-required Google challenges cannot be solved locally. |
| `TMessagesProj/libs/libgsaverification-client.aar` | Binary and dependency removed from all app modules. Google voice verification activity/service excluded and removed from app manifests. |
| `com.google.gms:google-services:4.5.0` / `com.google.gms.google-services` plugin | Root classpath and module applications removed. All five `google-services.json` files deleted; no generated Google resources required. |
| `com.google.firebase:firebase-crashlytics-gradle:3.0.8` / Crashlytics plugin | Root classpath and HockeyApp plugin removed. |
| `com.huawei.agconnect:agcp:1.9.1.301` (root classpath) | Huawei distribution excluded from the configured build; no AGConnect plugin is loaded for `afat`. |

The root build rejects the above proprietary dependency groups during resolution,
including transitive dependencies. ZXing, Gson, Guava, AndroidX credentials,
MediaRouter and other existing open-source dependencies remain. MediaRouter has
no Cast route provider in this build; the dependency guard also catches any
future attempt to introduce GMS transitively.

MapLibre's dependency excludes `com.google.android.gms` defensively; the root
guard still rejects proprietary groups everywhere else. Style URLs are centralized
in `MapLibreMapsProvider`. Both light and dark use
`https://tiles.openfreemap.org/styles/liberty` until a dark style can be verified.
Telegram's Google-specific raw style JSON is treated as a theme selection, not
passed to MapLibre. Satellite/hybrid requests fall back to street maps because
no keyless imagery source is configured. Tiles and styles require internet access
on the device; map readiness does not wait for a tile download. An external Maps
app is not required for the embedded map.

The provider forwards create/start/resume/pause/stop/destroy/low-memory events,
ignoring the upstream worker-thread prewarm and duplicate create calls. Platform
location subscriptions stop on pause/destroy; location permissions remain under
the existing Telegram UI. Markers support anchors, rotation, tags, icons, clicks
and title/snippet text; metre-radius circles use geodesic polygons and dashed
outlines. Overlays are retained while styles load and restored after style changes.
The dark-style URL, resolved dependency graph and SDK integration have not been
verified offline; see the remaining device/build checks below.

The optional Huawei distribution is excluded from `settings.gradle`; its root
AGConnect plugin/repository are removed. Its historical sources and HMS
coordinates (`push:6.5.0.300`, `maps:6.6.0.300`, `location:6.4.0.300`) remain in
that unconfigured module. It is not a supported open-source build target.
The root guard rejects Huawei SDKs if a configured project tries to resolve them.

## Source and manifest boundaries

The library source set excludes these wholly SDK-specific upstream files instead
of rewriting large implementations:

- `GoogleMapsProvider.java`
- `GoogleLocationProvider.java`
- `FirebasePhoneNumberVerificationHelper.java`
- `GcmPushListenerService.java`
- `WearAuthListenerService.java`
- `chromecast/ChromecastOptionsProvider.java`

The four historical app distributions also exclude `GoogleVoiceClientActivity`
and `GoogleVoiceClientService`. Their unavailable services/activities are not
registered. FCM, Wear auth, SMS Retriever, Google measurement, Maps, Wallet,
Vision and ML Kit manifest declarations and the Play Billing permission are
removed from active manifests. Obsolete Google resource/ProGuard keeps are
removed. Unrelated Android components remain.

## Verification

Run the source audit without Android tooling:

```sh
python3 Tools/check_degoogle.py
```

The audit checks configured Gradle modules, active Java references after source
exclusions, local adapter imports, XML declarations, and removed configuration /
AAR files. It does **not** replace a resolved dependency graph or Java compilation.

For the full Java build, use JDK 17 or 21 and the installed Android SDK:

```sh
export JAVA_HOME=/path/to/jdk-21
export ANDROID_HOME="$HOME/Android/Sdk"
git submodule update --init --depth 1 TMessagesProj/lib/jlatexmath TMessagesProj_Modules/media
./gradlew :TMessagesProj_App:compileAfatReleaseJavaWithJavac
```

Native submodules and an NDK build are not part of this Java check. If dependency
task discovery is needed, use `./gradlew :TMessagesProj_App:tasks --all`.
Inspect the eventual resolved graph with:

```sh
./gradlew :TMessagesProj_App:dependencies --configuration afatReleaseRuntimeClasspath
```

During this change, JDK 21 was found in the existing Gradle JDK cache; `/usr/lib/jvm`
contained only JDK 25. The requested Gradle wrapper could not run because Gradle
8.13 was not cached and network socket access was denied. Both Java submodules
were missing. Initialization could not write the shared Git configuration, and
a direct GitHub fetch failed DNS resolution. Full app Java compilation and
resolved dependency verification remain unverified; no APK/native build was run.

Additional offline checks passed:

- New Java adapters compiled with JDK 21, Java 8 source/target, Android API 36,
  cached AndroidX core/annotations and ZXing 3.5.3 (declared project version is
  3.5.4). Temporary type declarations supplied the unavailable MediaRouter and
  generated/app types (`R`, `Utilities.Callback`, `TLRPC.auth_Authorization`).
  This validates the adapters and provider interfaces, not all upstream callers.
- 22 JVM contract checks exercised unavailable billing, completion/failure
  callbacks, unavailable sign-in/Pay/Cast/face detection, unknown language,
  bitmap/NV21 QR decoding, blank/truncated frames, raw PDF417 decoding and QR-only
  filtering. Temporary Android data-container doubles were used; no device test
  is implied.
- Source audit, XML component-preservation checks and `git diff --check` passed.

For the MapLibre integration (#4), Java 8 syntax checks passed, and the provider
compiled against Android API 36, cached AndroidX and temporary MapLibre type
declarations. This checks Java types and the complete `IMapsProvider` contract,
not real SDK signatures. JVM checks exercised production circle geometry at
equatorial/high latitudes and the date line, metre radii, closed rings, gap-first
dash patterns, option snapshots and removal before style loading. No real SDK,
tile rendering or device lifecycle test was possible. In particular, verify the
SDK's bitmap-density handling for icon offsets and the style/render callback
timing on the device; neither can be established by these offline checks.

## Push notifications (UnifiedPush)

- The user installs a UnifiedPush distributor (ntfy, Sunup, NextPush, ...) and picks
  it in Settings > Ayuless > Notifications. On start Ayuless also tries the current
  or default distributor automatically.
- The connector library generates a P-256 key pair and a 16-byte auth secret and
  stores them. The endpoint is registered via `account.registerDevice` with
  `token_type = 10` and the token
  `{"endpoint": "...", "keys": {"p256dh": "...", "auth": "..."}}`.
- Incoming messages are RFC 8291 (`aes128gcm`) encrypted. The connector decrypts
  them with the Web Push decryptor it bundles (from Tink, Apache-2.0); Ayuless
  does not ship its own copy of the crypto. `Tools/push/Rfc8291Check.java` checks
  that decryptor against the RFC 8291 section 5 example.
- The decrypted JSON goes into the upstream push handler through a small branch in
  `PushListenerController.processRemoteMessage`. If the body carries an
  MTProto-encrypted `p` field like FCM, that blob goes through the upstream
  decryption instead. Anything unparseable still wakes the connection, so updates
  are fetched normally.
- Without a distributor (none installed, registration failed or unregistered), the
  upstream keep-alive service is switched on, unless the user already set that
  option. It is switched off again once a distributor works.

Not verified yet: real delivery from the server to a distributor, the exact
payload format the server sends for type 10 tokens, and multi-account routing
(one endpoint is shared by all accounts, as with FCM; if the payload has no
`user_id`, the selected account is used).

## Remaining work

- Issue #3 follow-up: test UnifiedPush delivery on a device with a distributor
  (see above).
- Issue #4: compile the MapLibre 11.8.0 integration when dependency downloads are
  available, then test rendering/OSM attribution, location selection, live marker
  rotation/clicks, proximity circles, bounds fitting, permissions and lifecycle
  on a device without Google services. Verify the resolved graph contains no GMS.
  Verify a keyless dark style before changing the Liberty fallback. Satellite and
  hybrid imagery remain unconfigured.
- Test login alternatives on a test account. The server decides which methods
  are offered; a mandatory Integrity/reCAPTCHA challenge cannot be satisfied by
  an SDK-free client. Platform passkeys require a compatible Android provider.
- Test manual sticker editing, scanning, device location, background messaging,
  invoices and Premium/Stars display on a device without Google services.
- Run the full Java compile and resolved dependency audit once the build tools,
  repositories and Java submodules are available. Fix any remaining integration
  errors revealed by that build before treating `afatRelease` as verified.
- Rebuild/audit existing native prebuilts separately (see `LICENSE-AUDIT.md`).
  Removing Java SDK dependencies alone does not certify the whole APK as free
  of proprietary binaries.
