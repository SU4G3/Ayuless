#!/usr/bin/env bash
# SPDX-License-Identifier: GPL-3.0-or-later
# Functional regression checks against the engine with regenerated builtin arrays.
set -Eeuo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
output="${1:-$ROOT/build/quickjs-check}"
python3 "$ROOT/Tools/native/build-quickjs-builtins.py" "$output"
QJS="$ROOT/TMessagesProj/jni/twidget/widget-sandbox/packages/ios/Sources/CQuickJS/quickjs"
"${HOST_CC:-cc}" -O2 -fno-strict-aliasing -D_GNU_SOURCE -DQUICKJS_NG_BUILD \
    -I "$QJS" "$output/quickjs.c" "$QJS/dtoa.c" \
    "$QJS/libregexp.c" "$QJS/libunicode.c" \
    "$ROOT/Tools/fdroid/quickjs-test-runner.c" -lm -o "$output/test-builtins"
"$output/test-builtins" "$ROOT/Tools/fdroid/test-quickjs-builtins.js"
