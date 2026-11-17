# F-Droid submission checklist

Offline source audit: 2026-11-12. **Draft; not submitted, accepted or published.**
The recipe is deliberately disabled. This audit uses local sources and knowledge
of F-Droid policy/metadata; current policy, service terms and tool support have
not been checked online. Anti-Features disclose behaviour; they do not exempt
proprietary dependencies, missing source or incompatible licences.

## Store metadata and release identity

- [x] `fastlane/metadata/android/{en-US,ru-RU}` contains title, summary (at most
  80 characters), feature description, changelog and 512 × 512 icon copied
  byte-for-byte from `branding/ayuless-icon-512.png`.
- [x] Both descriptions disclose pre-alpha status, required Telegram service,
  optional third-party connections and no affiliation with Telegram/AyuGram.
- [x] Version name `13.2.1`, base code `7291` from `gradle.properties`. The
  application module overrides the APK code to `7291 * 10 + 9 = 72919` for
  `afat`; changelogs are therefore **72919.txt**, not 7291.txt. Recipe update
  checks apply the same `VercodeOperation`.
- [ ] Add actual phone screenshots; each locale has an explanatory README in
  `images/phoneScreenshots/`. These notes are not image assets.
- [ ] Replace `REPLACE_WITH_REVIEWED_RELEASE_COMMIT` with the immutable release
  revision containing these changes; verify version fields in the built APK.
  Local tag `v0.1.0-alpha.1` and Gradle version `13.2.1` use different naming
  schemes. `UpdateCheckMode: Tags` is configured; `AutoUpdateMode: None` is
  intentional until a tag-to-version scheme and an enabled build are reviewed.
  Then use the appropriate `Version <tag-template>` auto-update mode (there is
  no `AutoUpdateMode: Tags` value).

## Dependencies, binaries and complete corresponding source

- [x] `python3 Tools/check_degoogle.py` passes for the five configured modules.
  GMS, Firebase, ML Kit, Play Integrity, billing, reCAPTCHA and search-verification
  SDKs have been removed/replaced from the active build. The root Gradle guard
  rejects their dependency groups, including Huawei, at resolution time.
  Open-source libraries published by Google (AndroidX, Gson, Guava, ZXing, etc.)
  are not proprietary merely because of their namespace.
- [x] `git ls-files '*.jar' '*.aar' '*.so' '*.a' '*google-services.json'`
  finds only three Gradle wrapper JARs in the superproject: the root wrapper
  and two unused widget-sandbox Android demo wrappers. No tracked AAR, SO,
  static archive or Google Services JSON remains here. The recipe removes the
  two demo wrappers; F-Droid handles the root wrapper with its build tooling.
- [ ] **Packaged executable bytecode remains a source-build blocker.**
  `TMessagesProj/src/main/assets/WidgetSandbox/WidgetJS/{sandbox,validator}-bundle.qbc`
  are committed QuickJS bytecode used by the APK. Six more `.qbc` copies are
  under `TMessagesProj/jni/twidget/widget-sandbox/packages/{android,desktop,ios}`.
  Sources/tooling are in widget-sandbox (`packages/core`, `scripts`, QuickJS);
  the entry point is `packages/core/scripts/build-native-bundles.ts` (run via
  the pinned pnpm `build:native android` script, then copy outputs to the app).
  notices describe bundled linkedom, parse5, css-tree, Chart.js and D3. Add a
  pinned, offline JS dependency preparation and bytecode generation/copy step,
  remove committed generated code before scanning, and regenerate after it.
  Audit generated/minified JS and any bytecode embedded in source arrays too.
  Rebuilding the native QuickJS interpreter alone does not rebuild these assets.
  Do not suppress this with `scanignore` or label it `NonFreeComp` to bypass policy.
- [x] An unused web-demo `packages/quickjs-wasm/dist/emscripten-module.wasm`
  is identified and removed by the draft recipe, without removing QuickJS C
  sources used by the Android CMake target. It is still present upstream in Git.
- [ ] **Data/model/font provenance remains open.** Inventory includes
  `src/main/assets/models/diamond_ios/*.bin`, `secureid_ocr_nn.dat`,
  `PhoneFormats.dat`, `grammars.dat`, `emoji.pack`, `shapes.dat`, and fonts
  `gram.ttf`, `mw_bold.ttf`, `mw_bolditalic.ttf`, `num.otf`, `r*.ttf`.
  Establish each input's licence, editable/preferred source and generation
  procedure as applicable. Inventory images, sounds, stickers, Lottie resources,
  widget resources and bundled font variants too. A binary data file is not
  automatically proprietary, but the repository licence alone is not proof of
  rights to every upstream asset. Do not call this a completed asset audit.
- [ ] All 19 top-level submodules are uninitialised in this checkout. Gitlink
  hashes pin their revisions, but their recursive files/licences/binaries were
  not available to audit offline. Initialise recursively in a prepared checkout,
  inventory binaries there and inspect dependency manifests/lockfiles, including
  the media and jlatexmath modules and native/Rust transitive dependencies.
- [ ] Resolve Gradle's complete `afatReleaseRuntimeClasspath` and inspect POMs,
  licences and transitive SDKs; then run the F-Droid source scanner and an APK
  scanner (e.g. Exodus signatures). Source guards are not a full dependency or
  tracker audit. Review the old `com.stripe:stripe-android:2.0.2` SDK especially
  for licence, telemetry and transitive dependencies.
- [ ] Historical `TMessagesProj_AppHuawei` is excluded from settings, but still
  contains HMS declarations/source. Have fdroiddata reviewers decide whether
  unused distributions and excluded Google implementation files should be
  removed in recipe `rm` entries. Never blanket-ignore scanner findings.

## Native build and reproducibility

- [x] `.github/workflows/build.yml`, `.github/actions/native-prebuilts/action.yml`,
  `Tools/native/` and [NATIVE.md](../NATIVE.md) describe source builds of all 16
  archives per ABI (64 total), including generated FFmpeg and wallet headers.
  No GitHub Actions binary cache is an input to the F-Droid recipe. Source
  submodules are required (`submodules: true`); FFmpeg omits `--enable-nonfree`.
- [x] Recipe uses `afat` in `TMessagesProj_App`, NDK **27.2.12479018**, SDK 36,
  build-tools 36.0.0, SDK CMake 3.22.1 and all four ABIs. CI uses JDK 17;
  the requested local Java compile probe uses JDK 21. Pin the chosen JDK and
  host build tools in the actual F-Droid environment.
- [x] `Tools/native/build-prebuilts.sh --offline` now avoids Git fetch/update
  and rustup installation, checks prepared native gitlink revisions and Android
  Rust targets, disables rustup auto-install and sets `CARGO_NET_OFFLINE=true`.
  CI's default online preparation mode is retained.
- [ ] **Offline Rust provisioning is unresolved.** The default wrapper installs
  wallet Rust **1.96.1**, tlottie Rust **1.98.0**, and four Android targets;
  Cargo uses registry/git dependencies despite `--locked`. Arrange toolchains,
  checksummed source tarballs/srclibs and Cargo vendoring/configuration through
  an F-Droid-approved preparation mechanism. Lockfiles are not source archives.
  The disabled recipe specifies requirements and uses offline mode; it does
  not pretend an unprovisioned build works. Also provision SDK CMake 3.22.1;
  distro CMake alone does not satisfy AGP's pinned SDK CMake requirement.
- [x] Existing native scripts normalize `SOURCE_DATE_EPOCH` (usually 1), TZ,
  locale and C/C++/Rust source paths; several archives use `ZERO_AR_DATE`.
  The recipe fixes epoch/TZ/locale. No build-time random UUID, current-time
  version code or current-date BuildConfig field was found in the Gradle/buildSrc
  files examined. Runtime message IDs, crypto randomness and cache timestamps
  are necessary runtime behaviour and must not be replaced with fixed values.
- [ ] `common.sh:write_build_environment` writes wall-clock time and host paths
  to diagnostic `prebuild/build_env.txt`; the wrapper restores its tracked copy
  after a successful build. It is not an Android asset/source input in the
  inspected build. Confirm this on a real build and ensure generated headers,
  codec configuration strings, Rust dependencies and JS bundles do not leak
  paths/timestamps. Compare two clean APK builds from different directories
  with the same credentials/toolchains using diffoscope/apksigcopier as needed.
  Existing normalization is not proof of bit-for-bit reproducibility.
- [x] `-PayulessUnsignedRelease=true` skips the release signing assignment in
  `TMessagesProj_App`; default developer signing behaviour is retained. The
  recipe removes the committed development `release.keystore`. Neither private
  release keys nor signing passwords need be provided to F-Droid.
- [ ] Confirm an unsigned `:TMessagesProj_App:assembleAfatRelease` succeeds with
  that keystore absent. Normal signed output is
  `TMessagesProj_App/build/outputs/apk/afat/release/app.apk`; this module explicitly
  overrides the filename to `app.apk` for all variants. F-Droid signs with its own key,
  so users cannot update an installation signed with a different key in place.

## API credentials, login identity and network behaviour

- [x] Gradle reads ignored root `API_KEYS`, with environment overrides
  `APP_ID`, `APP_HASH`, `CERT_FINGERPRINT`. The recipe copies `API_KEYS.example`
  (`APP_ID=12345`, hash `0123456789abcdef0123456789abcdef`): these are public
  **compile placeholders**, not usable Telegram credentials. Credentials are
  compiled into the app and cannot be treated as secrets in distributed APKs.
- [ ] **Choose usable, distributable API credentials with reviewers.** Replace
  the example during recipe preparation by a documented, authorized credential
  source or adopt an acceptable user-supplied configuration design. Building
  with placeholders does not demonstrate login. Do not use CI secrets as an
  undocumented F-Droid prerequisite.
- [ ] **Login identity is a review risk.** The CI comment describes `api_id 4`
  with Telegram's official SHA-256 certificate fingerprint. That certificate
  constant is the fallback in `BuildVars.OFFICIAL_FINGERPRINT`; the recipe's
  placeholder ID is 12345, not 4. `LoginSpoof` sends package ID
  `org.telegram.messenger.web` and empty installer on every connection, and
  pre-login identifies as Telegram 10.10.1 / layer 177 before restoring the real
  version. It does not reflect F-Droid's actual signing certificate. Have
  Telegram API permission, trademark/identity, transparency and login reliability
  reviewed; a fingerprint constant is not the certificate or permission to use
  that identity. This preparation does not silently change authentication.
- [x] Recipe declares **NonFreeNet** for required proprietary Telegram servers,
  plus **Ads** because upstream sponsored messages can be displayed when local
  hiding is disabled (hiding currently defaults to true). Optional payments,
  bots, mini-apps, video embeds, link previews and map tile requests also need
  disclosure. The availability of FOSS server-side map software does not make
  the required Telegram server free software.
- [ ] No independent analytics/tracker SDK was found in the active dependency
  declarations inspected. AppCenter methods in base `ApplicationLoader` are
  empty and Firebase/Crashlytics integrations were removed. This does not prove
  absence of tracking in transitive libraries, payment providers or embedded
  web content. Audit those paths and actual network traffic; add **Tracking**
  if verified. Do not declare it merely because an account or internet is used.
  Server telemetry/statistics, diagnostics, updates and external APK/download
  flows also require review. No `NonFreeDep`/`NonFreeComp` exception is claimed.
- [x] Removed stale AppCenter update activity/receiver declarations from the
  configured HockeyApp module's manifest (no SDK was present). The manifest
  audit now rejects their reintroduction. These declarations were not part of
  `TMessagesProj_App`'s afat APK, but left broken component references in the
  wider configured source tree.
- [ ] Determine whether any shipped content/services need **NonFreeAssets** or
  **NonFreeAdd**; licence uncertainty is an unresolved blocker, not evidence
  for inventing an Anti-Feature or a substitute for the asset audit.
- [x] UnifiedPush **draft #30**, tracked under #3 in [ROADMAP.md](../ROADMAP.md),
  is not merged here. Existing FCM capability is unavailable; background
  connection/service behaviour remains. README/store text no longer claims
  working UnifiedPush. Completing it is **not required for submission**; test
  background notifications independently and do not promise push delivery.

## Artwork, licences and provenance

- [x] Project licence is GPL-3.0-or-later; upstream GPL-2.0-or-later notices and
  `LICENSE.GPL-2.0` remain. Review rationale in [LICENSE-AUDIT.md](../LICENSE-AUDIT.md).
  Own store icon is generated by GPL-licensed `branding/generate.py`; source
  artwork is retained in `branding/`. No Telegram/AyuGram brand art was imported
  for these new store icons.
- [x] Solar icons: **480 Design / R4IN80W**, **CC BY 4.0**, converted to Android
  vectors with color/style adaptations; attribution is in drawable comments and
  [CREDITS.md](../CREDITS.md). Added packaged `assets/ayuless-notices.txt`, including
  licence/source links and modification credit. Removed the packaging exclusion
  for dependency `licences/` directories, so those notices can survive packaging.
- [ ] Verify every asset's licence and required notices are actually included
  in the final APK/source distribution; see the unresolved data/font inventory.
- [x] Map providers are documented in [CREDITS.md](../CREDITS.md) and credited on
  maps: OpenFreeMap/OSM street style (OSM data ODbL); EOX **Sentinel-2 cloudless
  2016**, CC BY 4.0, modified Copernicus Sentinel data; EOX Bright overlay with
  OSM (ODbL) / Natural Earth (public domain) data and EOX/MapServer rendering.
  The overlay's published service permission is not a CC BY grant for rendering.
- [ ] Reconfirm EOX's exact vintage, rendering licence/redistribution terms,
  endpoint permission and all street-style/font/sprite notices. Online provider
  terms were not verified in this no-network task. Preserve source attribution
  and clickable map credits in all map modes; do not assume free access means
  free content or an open source service.
- [x] Current code provenance is documented in [CLEANROOM-REMOVED.md](../CLEANROOM-REMOVED.md),
  [CLEANROOM-SPEC.md](../CLEANROOM-SPEC.md) and [CLEANROOM-PHASE-B.md](../CLEANROOM-PHASE-B.md).
  The last records independent reimplementation after removal of the derived
  port; [CREDITS.md](../CREDITS.md) states the same. No exteraGram code or new
  AyuGram code was imported for this task.
- [ ] Reviewer must still validate that no AyuGram/exteraGram-derived code or
  assets remain in the selected release, including upstream hooks and history.
  Older Git history retains the removed AyuGram port and its original licence;
  removal does not relicense historical revisions. Any retained/reintroduced
  AyuGram4A material needs its original Radolyn licence/header, a credits entry
  and actual GPL/F-Droid-compatible permission. Inspiration is not proof of
  provenance or of licence compatibility.

## Validation and submission still required

- Local checks passed: DeGoogle source/manifest audit, YAML parsing, version
  agreement, summary/changelog character limits, PNG dimensions/content,
  checklist links, shell syntax and `git diff --check`. Six isolated offline
  wrapper checks using stubbed tools covered prepared inputs and rejection of
  uninitialised/wrong/conflicted submodules, missing Rust targets and missing
  toolchains. They verified no install/update command was invoked and that
  offline environment flags reached the build. These are control-flow checks,
  not a native compiler run.
- The requested offline Java compile command was attempted with JDK 21 and
  placeholder `APP_ID`, `APP_HASH`, `CERT_FINGERPRINT`. It failed in Gradle
  wrapper initialization, before configuration or compilation: the lock file
  `~/.gradle/wrapper/dists/gradle-8.13-bin/5xuhj0ry160q40clulazy9h7d/gradle-8.13-bin.zip.lck`
  cannot be created on the read-only filesystem. No Android compile success is
  claimed, and no external cache permissions were changed.
- [ ] Run `fdroid lint`, `fdroid readmeta`, `fdroid checkupdates` and
  `fdroid build --on-server org.ayuless.messenger:72919` on the enabled, reviewed
  metadata in a prepared fdroiddata checkout. Check the mapping form of
  localized Anti-Features, update-code transformation and scanner removals
  against the installed fdroidserver version. F-Droid tooling is not installed
  in this workspace, so schema/build-server validation remains outstanding.
- [ ] Full release/native/WidgetJS build, reproducibility comparison, login,
  background notifications, map attribution and feature tests on a device.
- [ ] Submit an inclusion request and fdroiddata merge request only after the
  above blockers are resolved. No request, remote issue, upload or publication
  was performed by this task.
