# Native libraries

The app links 16 static libraries per ABI from `TMessagesProj/jni/prebuild/lib/<abi>/`:
BoringSSL (`libcrypto`, `libssl`), FFmpeg (`libavcodec`, `libavformat`, `libavutil`,
`libswresample`, `libswscale`), libvpx, dav1d, opus, openh264, tdlib (`libtde2e`,
`libtdutils`), tlottie, wallet-engine and WAMR (`libiwasm`).

Upstream commits these as prebuilt `.a` files (about 204 MB). Ayuless builds them
from source in CI instead.

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

export ANDROID_SDK_ROOT=~/Android/Sdk   # needs ndk;27.2.12479018
Tools/native/build-prebuilts.sh                    # all four ABIs
ABIS=arm64-v8a Tools/native/build-prebuilts.sh     # one ABI, faster
Tools/native/compare-prebuilts.sh                  # compare with the committed archives
```

`build-prebuilts.sh` is a thin wrapper around upstream's
`TMessagesProj/jni/prebuild/build_all.sh`. It checks host tools, initialises the
source submodules, installs the Rust toolchains (wallet-engine's pinned 1.96.1 and
1.98.0 for tlottie, which upstream leaves at `stable`) and runs the build.

## CI

- `.github/actions/native-prebuilts` builds the libraries and caches them under a
  key derived from the submodule commits, build scripts, patches and toolchain
  versions (`Tools/native/build-prebuilts.sh --print-key`).
- The **Build** workflow runs it before Gradle, so the APK is linked against
  libraries built from source, not the committed ones.
- The **Native libraries** workflow (manual, or on PRs touching native inputs)
  builds them, runs `compare-prebuilts.sh` and uploads the archives as an artifact.

CI build (all four ABIs): 34 of 64 archives are byte-identical to the
committed ones (BoringSSL, libvpx, opus, WAMR, tdlib, swscale, most openh264).
The rest differ in bytes but export the same global symbols; for the Rust
wallet-engine only the per-build hash suffix of mangled names differs, which
`compare-prebuilts.sh` masks. The APK built in CI links against these.

## Status

The committed `.a` files are still in the tree while the source build proves
itself in CI. Removing them is the last step of
[#5](https://github.com/SU4G3/Ayuless/issues/5).
