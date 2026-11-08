#!/usr/bin/env bash
# SPDX-License-Identifier: GPL-3.0-or-later
#
# Compare freshly built archives in TMessagesProj/jni/prebuild/lib/ with the
# copies committed in git (HEAD). Reports byte equality and, when bytes differ,
# whether the set of defined global symbols matches. Exit code 1 if an archive
# is missing or its exported symbols differ; byte differences alone are only
# reported (host paths, archive member order and toolchain builds can change
# bytes without changing code). Rust legacy-mangled symbols end in a
# per-build hash (17h<16 hex>E) that depends on crate metadata and paths; that
# suffix is masked so only the actual symbol set is compared.

set -Eeuo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
NDK="${ANDROID_NDK_HOME:-${ANDROID_HOME:-$HOME/Android/Sdk}/ndk/27.2.12479018}"
NM="$(ls "$NDK"/toolchains/llvm/prebuilt/*/bin/llvm-nm | head -n 1)"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

status=0
cd "$ROOT"
while IFS= read -r path; do
    built="$ROOT/$path"
    committed="$TMP/committed.a"
    if [[ ! -f "$built" ]]; then
        echo "MISSING   $path"
        status=1
        continue
    fi
    git show "HEAD:$path" > "$committed"
    if cmp -s "$built" "$committed"; then
        echo "IDENTICAL $path"
        continue
    fi
    "$NM" -g --defined-only "$committed" 2>/dev/null | awk 'NF==3{print $3}' | sed -E 's/17h[0-9a-f]{16}E$/17hE/' | sort -u > "$TMP/a"
    "$NM" -g --defined-only "$built" 2>/dev/null | awk 'NF==3{print $3}' | sed -E 's/17h[0-9a-f]{16}E$/17hE/' | sort -u > "$TMP/b"
    if cmp -s "$TMP/a" "$TMP/b"; then
        echo "SYMBOLS=  $path ($(wc -l < "$TMP/a") symbols, bytes differ)"
    else
        echo "SYMBOLS!= $path"
        diff "$TMP/a" "$TMP/b" | head -n 20 || true
        status=1
    fi
done < <(git ls-files 'TMessagesProj/jni/prebuild/lib/*.a')

exit "$status"
