#!/usr/bin/env bash
# SPDX-License-Identifier: GPL-3.0-or-later
# Validate build/cache outputs independently of the Git index.
set -Eeuo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
PREBUILD="$ROOT/TMessagesProj/jni/prebuild"
ABIS="${ABIS:-arm64-v8a armeabi-v7a x86_64 x86}"
libraries=(avcodec avformat avutil crypto dav1d iwasm openh264 opus ssl
           swresample swscale tde2e tdutils tlottie vpx wallet_engine)
status=0
require_output() {
    if [[ ! -s "$PREBUILD/$1" ]]; then
        echo "MISSING: $PREBUILD/$1" >&2
        status=1
    fi
}

for abi in $ABIS; do
    case "$abi" in
        arm64-v8a|armeabi-v7a|x86_64|x86) ;;
        *) echo "ERROR: unsupported ABI: $abi" >&2; exit 1 ;;
    esac
    for library in "${libraries[@]}"; do
        require_output "lib/$abi/lib$library.a"
    done
    require_output "include/$abi/libavutil/avconfig.h"
    require_output "include/$abi/libavutil/ffversion.h"
done
for binding in wallet_engine gram_wallet; do
    require_output "include/wallet-engine/$binding.cpp"
    require_output "include/wallet-engine/$binding.hpp"
    require_output "include/wallet-engine/${binding}_scaffolding.hpp"
done
if (( status )); then
    echo "Run Tools/native/build-prebuilts.sh from the repository root before Gradle (see docs/NATIVE.md)." >&2
    exit "$status"
fi
echo "Native build outputs complete for: $ABIS"
