#!/usr/bin/env bash
# SPDX-License-Identifier: GPL-3.0-or-later
# Compile TWQB bytecode with the exact QuickJS/shim sources linked into JNI.
set -Eeuo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
WIDGET="$ROOT/TMessagesProj/jni/twidget/widget-sandbox"
QJS="$WIDGET/packages/ios/Sources/CQuickJS"
rebundle=false
if [[ "${1:-}" == --rebundle ]]; then rebundle=true; shift; fi
[[ $# == 1 ]] || { echo "Usage: $0 [--rebundle] <generated-assets-directory>" >&2; exit 2; }
mkdir -p "$1"
output="$(cd "$1" && pwd)"
# The app's original renderer bundle uses the webkit transport and is byte-for-byte
# identical to this package. Preserve that choice while moving generated assets.
input="$WIDGET/packages/ios/Sources/WidgetSandbox/WidgetJS"
export SOURCE_DATE_EPOCH="${SOURCE_DATE_EPOCH:-1}" TZ=UTC LC_ALL=C
[[ "$SOURCE_DATE_EPOCH" =~ ^[0-9]+$ ]] || { echo 'Invalid SOURCE_DATE_EPOCH' >&2; exit 1; }
umask 022
export COREPACK_ENABLE_NETWORK=0 npm_config_manage_package_manager_versions=false npm_config_offline=true
if "$rebundle"; then
    # Dependency installation belongs to preparation, never to this build step.
    expected=$(python3 -c 'import json,sys; print(json.load(open(sys.argv[1]))["packageManager"].split("@")[1])' "$WIDGET/package.json")
    [[ "$(pnpm --version)" == "$expected" ]] || { echo "Pinned pnpm $expected required" >&2; exit 1; }
    (cd "$WIDGET/packages/core" && pnpm exec tsx scripts/build-native-bundles.ts ios --out "$input")
fi
work="$output/../widgetjs-host"
rm -rf "$output/WidgetSandbox/WidgetJS"
mkdir -p "$work" "$output/WidgetSandbox/WidgetJS"
# Always rebuild: no host binary/cache or mtime is trusted as a compiler input.
"${HOST_CC:-cc}" -O2 -g0 -fno-ident -fno-strict-aliasing -std=gnu11 \
    -D_GNU_SOURCE -DQUICKJS_NG_BUILD -Wno-unused-parameter \
    "-ffile-prefix-map=$ROOT=/src" "-fdebug-prefix-map=$ROOT=/src" \
    -I "$QJS/include" -I "$QJS/quickjs" \
    "$QJS/quickjs/quickjs.c" "$QJS/quickjs/dtoa.c" \
    "$QJS/quickjs/libregexp.c" "$QJS/quickjs/libunicode.c" \
    "$QJS/shim/qjs_shim.c" "$WIDGET/packages/tools/qjsc/qjsc.c" \
    -lm -o "$work/qjsc"
for name in renderer sandbox validator; do
    cp "$input/$name-bundle.js" "$output/WidgetSandbox/WidgetJS/"
done
for name in sandbox validator; do
    # The stack-trace name must never contain the absolute build directory.
    "$work/qjsc" "$input/$name-bundle.js" \
        "$output/WidgetSandbox/WidgetJS/$name-bundle.qbc" "$name-bundle.js"
done
python3 - "$output/WidgetSandbox/WidgetJS" "$SOURCE_DATE_EPOCH" <<'PY'
import os, pathlib, sys
for path in sorted(pathlib.Path(sys.argv[1]).iterdir()):
    os.utime(path, (int(sys.argv[2]), int(sys.argv[2])))
PY
