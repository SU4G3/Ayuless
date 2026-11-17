# F-Droid recipe integration, 2026-11-17

**Complete build succeeded.** The corrected candidate
`afadbaf8fe909ff11e2b2f5086c153ac5dd28c46` completed preparation, source scan,
all 64 native archives, WidgetJS rebuilding, release APK assembly and F-Droid's
APK dex-class/signing-block scans. The container exited 0; the log ends
`1 build succeeded` at 17:24:42 UTC (20:24:42 MSK).

## Final artifact checks

- CLI: `fdroid build --on-server --test --scan-binary --verbose org.ayuless.messenger:72919`.
- Official buildserver-trixie image, rootless Podman, fdroidserver 2.4.5;
  image digest and provisioning details are retained below.
- APK: `org.ayuless.messenger`, versionName `13.2.1`, versionCode `72919`,
  minimum SDK 23, target SDK 36; all four Android ABIs are packaged.
- Unsigned APK size: approximately 97.6 MiB. SHA-256:
  `c6c66c27a41a5fddf0b24714a35f21d45596a94204f193a641e42721bc0c8b9c`.
- `Tools/test_noto_emoji.py` validates all 3,606 EPK3 images, all 36 handshake
  tone combinations and the uncompressed APK emoji pack.
- All 47 files under packaged `assets/ayuless-notices/` match source contents,
  including application GPL texts, Noto, font and native/SIMDe notices.
- A separate zipaligned test copy verifies with apksigner v1/v2/v3. Its signing
  certificate SHA-256 is `a08d7dc323ddf71ef3201944397e0d3cce7d40847263e11f328b68bbe19229ab`,
  the existing public Telegram test key. This is not F-Droid's release signature.
  Signed APK SHA-256:
  `e3e221b1cc6426dd9983c8bcb16269935af3d045e47b84b33329dc5fe1eba2de`.
- Test copy: private `SU4G3/Ayuless-reports`, release `test-cleanroom`, asset
  `Ayuless-test-fdroid-final.apk`. The original unsigned APK is preserved.
- Source tarball and build/tool-version logs are retained with the local build.

## Remaining release steps

Test this exact APK on a phone: installation, login, mini-apps, Noto and ntfy
notifications including a locked screen. Then finalize the release tag and submit
the recipe for F-Droid review. No inclusion request has been sent and acceptance
is not claimed. Two clean APK builds have not been compared; whole-build network
isolation was not tested. The component-level WAMR network-none test did pass.
Inherited assets match the existing Forkgram recipe source as documented in
`ASSET-REVIEW-2026-11-17.md`; they are not treated as confirmed replacement blockers.
Login identity and the original phone-data provenance remain disclosed review context.

## Earlier attempts and fixes (historical progress below)

The sections below describe intermediate attempts, not the final status above.

### Initial attempt

**In progress. No final APK, successful complete build, submission or publication
is claimed by this report.** The public build block remains disabled.

## Exact candidate and environment

- Source: `93e246726b67ef4757fee7ac5dc8024f6cda45f9`, including UnifiedPush,
  Noto, five screenshots and packaged font/native/application notices.
- Official image: `registry.gitlab.com/fdroid/fdroidserver:buildserver-trixie`,
  pinned digest `sha256:1ea8bef1820bff06c2974e31b4fc4c79c364cd24231b86e71fa4b926797e0dac`.
- fdroidserver 2.4.5, Debian trixie, JDK 21, NDK 27.2.12479018, SDK CMake 3.22.1.
- Rootless Podman; application code and srclibs fetched from public Git sources.
  No GitHub native binary cache or host Gradle cache seeded into the build.
- CLI: `fdroid build --on-server --test --scan-binary --verbose org.ayuless.messenger:72919`.
  `--on-server` is the internal mode used inside fdroiddata's official CI image;
  it is not a claim that a Vagrant VM was booted using the host `--server` flag.
- The test metadata copy removes `disable`; source pin and commands match the
  reviewed recipe. Native jobs and Gradle workers are limited to two, Gradle heap
  to 3 GB for the local machine. No signing key is supplied to the build.

## Completed checks

- Configured `fdroid readmeta` and `fdroid lint org.ayuless.messenger` pass.
- SDK/NDK provisioning, source checkout and pinned submodule initialization pass.
- DeGoogle audit passes. Both Rust toolchains and all eight Android targets are
  installed; all three Cargo workspaces vendor and resolve metadata offline.
- Pinned npm preparation, source esbuild preparation and SDK CMake installation
  pass. Gradle 8.13 is obtained through gradlew-fdroid's transparency/checksum flow.
- Gradle `clean` passes with the release keystore removed and unsigned-release
  properties enabled. This alone is not a completed APK assembly.
- The initial full prepared-source scan found 17 fatal findings: askama/bitstream
  regression fixtures, poly1305 fuzz inputs and wit-bindgen WASI example/archive
  binaries. `Tools/native/prune-cargo-test-data.py` removes only these reviewed
  files from specific pinned package versions before scanning.
- wit-bindgen's build.rs returns immediately for non-Wasm targets; its archive is
  not used on Android. Poly1305 inputs are consumed by tests/fuzzing, not release
  crypto. Askama/bitstream files are test fixtures. No Android source is removed.
- The helper checks each removed file against its original Cargo SHA-256, keeps
  the registry package checksum and every remaining file digest intact, and only
  removes deleted filenames from `.cargo-checksum.json`. A real-package copy
  check verified all remaining hashes and repeat-run idempotence.
- After the fix, source scanning completes without fatal errors and execution
  reaches the native `build` commands. Only the existing nine float-model data
  exclusions remain; no blanket vendor scanignore or `--skip-scan` is used.
- Native compilation is running. At the 16:45 MSK review, 54/64 archives existed
  and TDLib compilation was active. Their existence is partial progress, not the
  final `check-prebuilts.sh` result.

## Recipe corrections found during review

The public recipe still pointed at an older candidate while the test copy used
newer code. It is now pinned to the actual candidate above, with a five-hour
build timeout for four ABIs. The recipe remains disabled pending final review.

The official image provides `sdkmanager` on PATH, not the previous hardcoded
`cmdline-tools/latest/bin/sdkmanager` path. The corrected call installed CMake.

The rustup srclib's old sed substitution does not pin the installer for its newer
shell-script layout. `RUSTUP_VERSION=1.28.2` explicitly selects the archive. The
pinned installer check reports `rustup-init 1.28.2`; the rerun uses that variable.

## Still required

Complete native validation, WidgetJS compilation, unsigned APK assembly, binary
scan and checks of the actual APK's version, contents, uncompressed Noto pack and
notices. Final-candidate device/login/mini-app/notification checks remain. No full
APK reproducibility comparison has been established. Resource provenance and
login identity remain transparent review questions in the main checklist.

The user's ntfy delivery report includes a locked-screen test. However, the
provided settings screenshot lacks the Notifications entry added by PR #30, so
it is not sufficient to identify the tested APK or attribute delivery to the
new UnifiedPush implementation. Test the exact final candidate after assembly.

## Review correction: WAMR SIMDe fetch

The first native pass built 60/64 archives, including all tlottie and wallet
Android variants, then WAMR CMake started a real `git clone` for SIMDe after the
source scan. This violated the advertised offline native-build contract. The
pass was stopped; it is not a completed F-Droid build.

Commit `afadbaf8fe909ff11e2b2f5086c153ac5dd28c46` pins SIMDe as a source submodule
at `71fd833d9666141edcd1d3c109a80e228303d8d7` (upstream WAMR's v0.8.2). The cache
key/source checks include that gitlink. WAMR receives an explicit local
FetchContent source directory and `FETCHCONTENT_FULLY_DISCONNECTED=ON`; SIMD
remains enabled. Its configured Ninja path is passed explicitly to CMake too.

The corrected WAMR script was built against NDK 27.2 / SDK CMake 3.22.1 in a
separate rootless container with `--network none`, with no Git metadata in the
copied input trees. It exited 0 and produced all four `libiwasm.a` files:

- arm64-v8a: 1,179,024 bytes
- armeabi-v7a: 1,106,174 bytes
- x86: 894,312 bytes
- x86_64: 1,193,876 bytes

This proves the corrected component builds without network access. The complete
recipe must still run on the corrected candidate; the earlier 60 libraries and
this component check must not be described as a completed final APK build.

The complete recipe was restarted on corrected candidate `afadbaf8` with no
copied native output archives. Preparation and the full source scan, including
the new SIMDe submodule, pass. Native compilation is in progress; at 17:19 MSK
20/64 archives existed. The full result remains pending.
