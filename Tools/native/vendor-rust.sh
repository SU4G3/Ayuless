#!/usr/bin/env bash
# SPDX-License-Identifier: GPL-3.0-or-later
# Preparation step, allowed to fetch locked sources. Build steps stay offline.
set -Eeuo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
source "$ROOT/Tools/native/rust-env.sh"
JNI="$ROOT/TMessagesProj/jni"
for project in gram-wallet wallet-engine/bindgen/cpp/bindgen tlottie; do
    version=1.96.1; bindir="${WALLET_RUST_BIN:-}"
    if [[ "$project" == tlottie ]]; then version=1.98.0; bindir="${TLOTTIE_RUST_BIN:-}"; fi
    setup_rust_env "$version" "$bindir"
    manifest="$JNI/$project/Cargo.toml"
    # The generator can be a member of wallet-engine's workspace. Locate the
    # actual lock/config root instead of assuming a nested Cargo.lock exists.
    workspace_manifest="$("$CARGO" locate-project --workspace --message-format plain --manifest-path "$manifest")"
    directory="$(dirname "$workspace_manifest")"
    # Cargo discovers configuration from cwd, not --manifest-path. Keep the
    # config and vendored sources with each workspace, including the generator.
    (cd "$directory"
        [[ -f Cargo.lock ]] || { echo "Missing pinned $project/Cargo.lock" >&2; exit 1; }
        mkdir -p .cargo
        [[ ! -e .cargo/config.toml ]] || { echo "Refusing to overwrite $project/.cargo/config.toml" >&2; exit 1; }
        "$CARGO" vendor --manifest-path "$manifest" --locked --versioned-dirs vendor-fdroid > .cargo/config.toml.tmp
        printf '\n[net]\noffline = true\n' >> .cargo/config.toml.tmp
        mv .cargo/config.toml.tmp .cargo/config.toml
        "$CARGO" metadata --locked --offline --format-version 1 > /dev/null
    )
done
