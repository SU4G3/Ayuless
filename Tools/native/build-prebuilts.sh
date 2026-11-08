#!/usr/bin/env bash
# SPDX-License-Identifier: GPL-3.0-or-later
#
# Build every static library in TMessagesProj/jni/prebuild/lib/<abi>/ from source.
#
# Sources are the git submodules under TMessagesProj/jni (BoringSSL, FFmpeg,
# libvpx, dav1d, opus, openh264, tdlib, tlottie, wallet-engine, WAMR). Each one
# is pinned to an exact commit by the superproject, and git verifies every
# object by its hash on checkout, so the inputs are fixed by this repository's
# history. The actual per-library build logic is upstream's
# TMessagesProj/jni/prebuild/build_*.sh; this wrapper only checks the host,
# prepares the submodules and Rust toolchains, and runs build_all.sh.
#
# Usage:
#   Tools/native/build-prebuilts.sh            # all ABIs
#   ABIS="arm64-v8a" Tools/native/build-prebuilts.sh
#   Tools/native/build-prebuilts.sh --print-key   # cache key inputs only
#
# Host requirements (Linux x86_64 or macOS): Android NDK 27.2.12479018, cmake,
# ninja, make, nasm, meson, pkg-config, gperf, python3, perl, patch, rustup.

set -Eeuo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
JNI="$ROOT/TMessagesProj/jni"
PREBUILD="$JNI/prebuild"

SUBMODULES=(
    TMessagesProj/jni/third_party/boringssl
    TMessagesProj/jni/third_party/openh264
    TMessagesProj/jni/third_party/xiph/opus
    TMessagesProj/jni/third_party/libvpx
    TMessagesProj/jni/third_party/dav1d
    TMessagesProj/jni/third_party/ffmpeg
    TMessagesProj/jni/tlottie
    TMessagesProj/jni/wallet-engine
    TMessagesProj/jni/td
    TMessagesProj/jni/third_party/wamr
)

# Rust toolchains. wallet-engine pins its own in gram-wallet/rust-toolchain.toml;
# tlottie defaults to "stable" upstream, pin it here so builds do not drift.
TLOTTIE_RUST_TOOLCHAIN="${TLOTTIE_RUST_TOOLCHAIN:-1.98.0}"
export TLOTTIE_RUST_TOOLCHAIN
RUST_ANDROID_TARGETS=(aarch64-linux-android armv7-linux-androideabi x86_64-linux-android i686-linux-android)

# Everything that determines the output: submodule commits, build scripts,
# patches, this wrapper and the pinned toolchain versions.
print_key() {
    (
        cd "$ROOT"
        git ls-tree HEAD "${SUBMODULES[@]}" TMessagesProj/jni/gram-wallet
        find TMessagesProj/jni/prebuild -maxdepth 1 -name '*.sh' -print0 | sort -z | xargs -0 sha256sum
        find TMessagesProj/jni/prebuild/patches -type f -print0 | sort -z | xargs -0 sha256sum
        sha256sum Tools/native/build-prebuilts.sh
        echo "NDK=${NDK_VERSION:-27.2.12479018} TLOTTIE_RUST=$TLOTTIE_RUST_TOOLCHAIN ABIS=${ABIS:-all}"
    )
}

if [[ "${1:-}" == "--print-key" ]]; then
    print_key
    exit 0
fi

missing=()
for tool in cmake ninja make nasm meson pkg-config gperf python3 perl patch rustup cargo; do
    command -v "$tool" >/dev/null 2>&1 || missing+=("$tool")
done
if (( ${#missing[@]} )); then
    echo "ERROR: missing host tools: ${missing[*]}" >&2
    echo "       Debian/Ubuntu: sudo apt-get install cmake ninja-build make nasm meson pkg-config gperf python3 perl patch" >&2
    echo "       Rust: https://rustup.rs" >&2
    exit 1
fi

echo "==> Initialising pinned source submodules"
git -C "$ROOT" submodule update --init --recursive --depth 1 "${SUBMODULES[@]}"

echo "==> Preparing Rust toolchains"
wallet_toolchain="$(sed -n 's/^channel *= *"\(.*\)"/\1/p' "$JNI/gram-wallet/rust-toolchain.toml")"
for toolchain in "$wallet_toolchain" "$TLOTTIE_RUST_TOOLCHAIN"; do
    rustup toolchain install "$toolchain" --profile minimal
    rustup target add --toolchain "$toolchain" "${RUST_ANDROID_TARGETS[@]}"
done

echo "==> Building prebuilt libraries (this takes a while)"
"$PREBUILD/build_all.sh"

# build_all.sh rewrites build_env.txt with host paths and a timestamp; keep the
# committed copy so a source build does not leave a dirty tree.
git -C "$ROOT" checkout -- TMessagesProj/jni/prebuild/build_env.txt 2>/dev/null || true

echo "==> Produced archives"
find "$PREBUILD/lib" -name '*.a' -print0 | sort -z | xargs -0 ls -l
