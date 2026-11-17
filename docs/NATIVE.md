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
sudo apt-get install cmake ninja-build make nasm meson pkg-config gperf python3 perl patch
# Rust via https://rustup.rs (the script installs the pinned toolchains and Android targets)

export ANDROID_SDK_ROOT=~/Android/Sdk
# Install SDK/NDK components with the Android SDK command-line tools:
"$ANDROID_SDK_ROOT/cmdline-tools/latest/bin/sdkmanager" \
  "platforms;android-36" "build-tools;36.0.0" "ndk;27.2.12479018" "cmake;3.22.1"
Tools/native/build-prebuilts.sh                    # all four ABIs
Tools/native/check-prebuilts.sh                    # validates all expected outputs
cp API_KEYS.example API_KEYS                      # fill in APP_ID / APP_HASH
./gradlew assembleAfatRelease
```

`build-prebuilts.sh` is a thin wrapper around upstream's
`TMessagesProj/jni/prebuild/build_all.sh`. It checks host tools, initialises the
source submodules, installs the Rust toolchains (wallet-engine's pinned 1.96.1 and
1.98.0 for tlottie, which upstream leaves at `stable`) and runs the build.

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
`CARGO_NET_OFFLINE=true`. All locked Cargo dependencies must already be available
as sources, along with the SDK, NDK and host tools. Missing inputs fail the build.
The normal invocation still prepares submodules and toolchains as CI does.

This mode does not provision F-Droid's build server. The disabled
[draft recipe](fdroid/org.ayuless.messenger.yml) needs reviewed source/toolchain
preparation and a WidgetJS bytecode source build before submission; see the
[checklist](fdroid/CHECKLIST.md). For an unsigned application release, use
`./gradlew :TMessagesProj_App:assembleAfatRelease -PayulessUnsignedRelease=true`;
the application module does not require the local release keystore in this mode.

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
