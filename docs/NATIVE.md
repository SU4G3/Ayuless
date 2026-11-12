# Native libraries

The app links 16 static libraries per ABI from `TMessagesProj/jni/prebuild/lib/<abi>/`:
BoringSSL (`libcrypto`, `libssl`), FFmpeg (`libavcodec`, `libavformat`, `libavutil`,
`libswresample`, `libswscale`), libvpx, dav1d, opus, openh264, tdlib (`libtde2e`,
`libtdutils`), tlottie, wallet-engine and WAMR (`libiwasm`).

Upstream commits these as prebuilt `.a` files (about 204 MB). Ayuless builds them
from pinned sources in CI and locally instead. No `.a` files are stored in Git.
The two unused Linux host archives bundled with Breakpad (gflags and glog) are
also removed; the Android Breakpad targets compile their client sources directly
and do not link those archives.

## Sources

Every library comes from a git submodule under `TMessagesProj/jni/`, pinned to an
exact commit by this repository. Git checks each object against its hash on
checkout, so the inputs of a build are fixed by the commit you build from. The
only local patches are in `TMessagesProj/jni/prebuild/patches/` (FFmpeg security
fixes). FFmpeg is configured without `--enable-nonfree`.

## Building locally

```bash
# Debian/Ubuntu host tools
sudo apt-get install build-essential cmake ninja-build make nasm meson pkg-config gperf python3 perl patch
# Prepare pinned Rust with rustup, or supply preinstalled distributions below.

export ANDROID_SDK_ROOT=~/Android/Sdk
# Install SDK/NDK components with the Android SDK command-line tools:
"$ANDROID_SDK_ROOT/cmdline-tools/latest/bin/sdkmanager" \
  "platforms;android-36" "build-tools;36.0.0" "ndk;27.2.12479018" "cmake;3.22.1"
Tools/native/build-prebuilts.sh                    # all four ABIs
Tools/native/check-prebuilts.sh                    # validates all expected outputs
cp API_KEYS.example API_KEYS                      # fill in APP_ID / APP_HASH
./gradlew assembleAfatRelease
```

`build-prebuilts.sh` is a wrapper around upstream's
`TMessagesProj/jni/prebuild/build_all.sh`. It checks host tools, initialises the
source submodules, checks the Rust toolchains (wallet-engine's pinned 1.96.1 and
1.98.0 for tlottie, which upstream leaves at `stable`) and runs the build.
Prepared toolchains are reused. Online mode installs missing ones only when no
explicit distribution directory was supplied.

`assembleAfatRelease` needs all four ABIs; do not restrict `ABIS` for this build.
For a build configured for only one ABI, use e.g.
`ABIS=arm64-v8a Tools/native/build-prebuilts.sh`. Each invocation cleans previous
native outputs, including other ABIs. Run from the repository root. CMake checks
for the required archives and generated headers and points to the source build
command if any are missing. Generated `.a` files are ignored by Git.
Re-run the source build after changing native inputs; local outputs are not
automatically rebuilt by Gradle. The CI cache uses the same output directories.

## Offline builds and F-Droid

`Tools/native/build-prebuilts.sh --offline` uses already initialised, pinned
native submodules and installed Rust toolchains/Android targets. It does not run
Git updates or rustup installation, disables rustup auto-install, and sets
`CARGO_NET_OFFLINE=true`. `Tools/native/rust-env.sh` selects **both** Cargo and
rustc from the requested pin and checks the installed Android standard libraries.
The separate C++ binding generator uses the wallet toolchain too, irrespective
of its own upstream `rust-toolchain` default. Neither library script installs
rustup, toolchains or targets.

For an environment without rustup, set `WALLET_RUST_BIN=/prepared/rust-1.96.1/bin`
and `TLOTTIE_RUST_BIN=/prepared/rust-1.98.0/bin`. Each directory must contain real
`rustc` and `cargo` executables with all four Android targets provisioned in its
sysroot. No global/default toolchain selection is needed. Toolchain/version or
target mismatches fail before compilation. The normal online wrapper also
respects these explicitly supplied distributions and never tries to install them.

All locked Cargo dependencies must be available as **source**, not only compiled
registry artifacts. On the networked preparation host, after checking out the
pinned submodules and installing both toolchains/targets, run:

```bash
Tools/native/vendor-rust.sh
# Transfer the prepared checkout, vendor-fdroid trees, .cargo/config.toml files,
# toolchains, SDK/NDK and host tools to the offline build environment.
Tools/native/build-prebuilts.sh --offline
```

The preparation script runs `cargo vendor --locked --versioned-dirs` separately
for gram-wallet, wallet-engine's C++ generator and tlottie. It locates the actual
workspace/lock root, preserves Cargo's registry/git source replacement entries
and checksum files, adds `[net] offline = true`, and verifies offline metadata
resolution. Each compiler invocation runs from the relevant workspace so Cargo
actually discovers that configuration. Existing configuration is not overwritten;
merge reviewed upstream configuration manually when necessary. Vendor directories
and generated configs are local preparation outputs, not committed binaries.
Review licences of all these sources with the F-Droid scanner.

The [draft recipe](fdroid/org.ayuless.messenger.yml) now has explicit preparation:
the pinned fdroiddata `rustup` srclib installer, Rust 1.96.1 + 1.98.0, the four
Android targets, Cargo vendoring, pinned pnpm dependency installation and SDK CMake
3.22.1. Preparation can use the network on F-Droid's server; the native/WidgetJS
build phase cannot. This editing sandbox used no network. The srclib definition,
host Node/pnpm compatibility and scanner handling of esbuild's host compiler need
validation against the actual fdroiddata/build-server installation. The recipe
stays disabled because those checks, builtin sources and asset licences are open.

For an unsigned application release, use
`./gradlew :TMessagesProj_App:assembleAfatRelease -PayulessUnsignedRelease=true`;
the application module does not require the local release keystore in this mode.

## WidgetJS executable assets

`TMessagesProj:buildWidgetJs` runs before `preBuild` and produces the three JS
assets and two TWQB `.qbc` files under `TMessagesProj/build/generated/widgetjs/assets`.
`Tools/native/build-widgetjs.sh` always compiles a host qjsc from the **same
tracked** `quickjs/{quickjs,dtoa,libregexp,libunicode}.c`, `shim/qjs_shim.c` and
`packages/tools/qjsc/qjsc.c` that implement the app's QuickJS format. The selected
repository revision pins the compiler; no installed qjsc or downloaded binary
cache is accepted. Stack-trace filenames are basenames, output mtimes use
`SOURCE_DATE_EPOCH`, and C/C++ source paths are remapped. All eight committed
`.qbc` copies and the unused Emscripten WASM have been removed.

Normal development builds compile the checked-in JS bundles. They are identical
to the previous app assets: the shared iOS package's renderer uses the original
**webkit transport**, while the standalone Android demo has a different renderer.
The source location does not change the app's renderer or sandbox behaviour.

For the complete TypeScript → JS → bytecode build used by the F-Droid recipe:

```bash
# Preparation only: install pnpm 12.10.1 as pinned in widget-sandbox/package.json.
cd TMessagesProj/jni/twidget/widget-sandbox
pnpm install --frozen-lockfile --ignore-scripts
cd ../../../..
./gradlew :TMessagesProj_App:assembleAfatRelease \
  -PayulessUnsignedRelease=true -PayulessRebuildWidgetJs=true --offline
# Or without Gradle, after dependency preparation:
Tools/native/build-widgetjs.sh --rebundle /tmp/widget-assets
```

The build never installs JS dependencies; `COREPACK_ENABLE_NETWORK=0` prevents
implicit Corepack downloads. `--rebundle` runs the pinned pnpm/tsx/esbuild tooling
and upstream `build-native-bundles.ts` against `packages/core` and its lockfile.
The source JS files are regenerated in the shared package, then copied to the
generated app asset tree. Missing dependencies fail instead of being fetched.
esbuild's npm host executable still needs F-Droid scanner/build-tool review.

**Remaining source blocker:** QuickJS `quickjs.c` includes generated
`builtin-array-fromasync.h`, `builtin-iterator-zip.h` and
`builtin-iterator-zip-keyed.h`. Their byte arrays are executable code, not data.
Their preferred JS inputs and regeneration build rules are absent in this
checkout. Restore matching upstream sources at a pinned revision and bootstrap
regeneration for both host and Android builds before enabling F-Droid. Retaining
them is explicitly an unresolved blocker; deleting or stubbing them would alter
JavaScript features. No scanner exclusion is used.

## Reproducibility and comparison

Use the same source revision, recursive gitlinks, toolchains, dependency sources,
credentials, ABI list and `SOURCE_DATE_EPOCH` (default **1**) in two fresh checkouts
under different absolute paths. Build with `TZ=UTC LC_ALL=C`; the native scripts
enforce both, a fixed umask and `ZERO_AR_DATE=1`. C/C++ prefix maps cover the JNI
tree, checkout/build paths and NDK; Rust maps sources, vendored inputs, Cargo home,
sysroots and targets. Every final archive passes objcopy's deterministic-archive
mode and NDK `llvm-ar sD`, which zero member/index metadata while preserving
order and duplicate member names.
File searches used for outputs/header copying are sorted. Runtime cryptographic
randomness, message IDs and network/cache timestamps are untouched.

`write_build_environment` records the source epoch and tool versions, without
wall-clock time, job count or executable/SDK host paths. Its output is now the
ignored `prebuild/build/build_env.txt`; the old tracked developer-machine report
and the wrapper's `git checkout` restoration have been removed. This diagnostic
file is not an Android asset or native input.

After building each checkout, create manifests relative to each root:

```bash
find TMessagesProj/jni/prebuild/lib TMessagesProj/jni/prebuild/include \
     TMessagesProj/build/generated/widgetjs/assets -type f -print0 \
  | LC_ALL=C sort -z | xargs -0 sha256sum > /tmp/build-a.sha256
# In checkout B, repeat with /tmp/build-b.sha256, then:
diff -u /tmp/build-a.sha256 /tmp/build-b.sha256
diffoscope /path/a/TMessagesProj_App/build/outputs/apk/afat/release/app.apk \
           /path/b/TMessagesProj_App/build/outputs/apk/afat/release/app.apk
```

Compare **unsigned** APKs first. Signing certificates/signatures are a separate
input; use apksigcopier when comparing with an F-Droid-signed APK. Inspect any
differences down to generated headers, embedded codec configuration strings and
ZIP entries. A matching WidgetJS result is not proof of a reproducible native
build/APK. Locally the two `.qbc` results matched each other and their removed
upstream copies byte-for-byte; full native/APK comparison needs prepared inputs.

## CI

- `.github/actions/native-prebuilts` builds the libraries and caches them under a
  key derived from the submodule commits, build scripts, patches and toolchain
  versions (`Tools/native/build-prebuilts.sh --print-key`).
- Both cache hits and source builds must pass `Tools/native/check-prebuilts.sh`,
  which requires all 64 archives and the generated FFmpeg and wallet headers.
- The **Build** workflow runs the action before Gradle, so the APK links libraries
  built from pinned sources or restored from that cache.
- The **Native libraries** workflow (manual, or on PRs touching native inputs)
  uses the same action and uploads the validated archives and headers.
- The Gradle guard against proprietary dependencies remains enabled for APK builds.

For historical audits only, `Tools/native/compare-prebuilts.sh <git-ref>` compares
fresh outputs with archives in a pre-removal commit. It fails if the reference
contains no archives; normal builds never need a historical binary baseline.
