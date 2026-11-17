#!/usr/bin/env bash
# SPDX-License-Identifier: GPL-3.0-or-later
# Source this helper; selecting a prepared toolchain must never install one.
setup_rust_env() {
    local version="$1" bindir="${2:-}" target libdir
    export RUSTUP_AUTO_INSTALL=0 RUSTUP_NO_UPDATE_CHECK=1 RUSTUP_TOOLCHAIN="$version"
    if [[ -n "$bindir" ]]; then
        export RUSTC="$bindir/rustc" CARGO="$bindir/cargo"
    else
        RUSTC="$(rustup which --toolchain "$version" rustc)" || return
        CARGO="$(rustup which --toolchain "$version" cargo)" || return
        export RUSTC CARGO
    fi
    [[ -x "$RUSTC" && -x "$CARGO" ]] || {
        echo "ERROR: prepared Rust distribution must include rustc and cargo" >&2; return 1;
    }
    [[ "$("$RUSTC" --version)" == "rustc $version "* ]] || {
        echo "ERROR: Rust $version required" >&2; return 1;
    }
    # Works with both rustup-managed and environment-supplied distributions.
    for target in aarch64-linux-android armv7-linux-androideabi x86_64-linux-android i686-linux-android; do
        libdir="$("$RUSTC" --print target-libdir --target "$target")" || return
        compgen -G "$libdir/libstd-*.rlib" >/dev/null || {
            echo "ERROR: Rust $version target $target is not provisioned" >&2; return 1;
        }
    done
}
