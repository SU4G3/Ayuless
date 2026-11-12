# License audit

Snapshot of upstream DrKLO/Telegram at `7be672ea` (13.2.1, build 7291), done 2026-11-07.

## Can the combined work be GPL-3.0-or-later?

Yes, with the caveats below.

| Check | Result |
|---|---|
| Java/Kotlin files in `TMessagesProj*/src` | 3000 |
| Files with header "GNU GPL v. 2 or later" | 437 |
| Files mentioning GPL but **without** "or later" | 0 |
| Files with no license header at all | 1851 (covered by repo-level license) |
| Apache-2.0 headers (androidx copies, audioinfo, customtabs, `me.vkryl`, ...) | 114 |
| BSD headers (`org.telegram.PhoneFormat`) | 4 |
| LGPL / MPL headers | 0 |

Commands used (bash):

```bash
I="--include=*.java --include=*.kt"
grep -rlE "GPL v\. ?2 or later" $I . | wc -l
grep -rlE "GNU GPL|GPL v" $I . | xargs grep -L "or later"
grep -rL "GPL" $I TMessagesProj/src | wc -l
grep -rl "Apache License" $I . | wc -l
```

## Notes

- **Header-less files.** The upstream repo ships the plain GPLv2 text as `LICENSE`, while every file that has a header says "v2 or later". We treat header-less upstream files the same way (this is also how Telegram-FOSS and other forks have treated them). Kept upstream license text as `LICENSE.GPL-2.0`.
- **Apache-2.0** code is compatible with GPLv3 (but not GPLv2-only), so moving to v3 is fine.
- **BSD** code is compatible with both.
- **CC BY 4.0** artwork: the Solar icons (`TMessagesProj/src/main/res/drawable/ayuless_solar_*.xml`) are licensed under CC BY 4.0, which is one-way compatible with GPLv3 (CC BY 4.0 material may be included in a GPLv3 work). Attribution is kept in [CREDITS.md](CREDITS.md) and in each file's header comment.
- **Native submodules** (BoringSSL, FFmpeg, libvpx, dav1d, opus, openh264, tdlib, abseil, WAMR, ...) are under permissive or LGPL licenses. All GPLv3-compatible. FFmpeg must be built without `--enable-nonfree`.

## Dependency and binary audit status

The Java header snapshot above is not a complete audit of assets or recursive
dependencies. Current F-Droid blockers and verification status are tracked in
[fdroid/CHECKLIST.md](fdroid/CHECKLIST.md).

- `TMessagesProj/jni/prebuild/lib/**/*.a`: the committed copies have been removed. CI builds all 64 static archives from pinned source submodules ([NATIVE.md](NATIVE.md)). A prebuilt archive is not inherently proprietary; F-Droid needs a source build and compatible licensing of its inputs.
- ~~`TMessagesProj/libs/libgsaverification-client.aar`~~: closed Google binary, removed in #14 (`Tools/check_degoogle.py` fails the build if it comes back).
- Proprietary Gradle SDKs (GMS, Firebase, ML Kit, Play Integrity, billing and Play credentials provider) have been removed from the active build; see [DEGOOGLE.md](DEGOOGLE.md). Historical Huawei sources remain outside the configured build. Full transitive resolution still needs review.
- Packaged QuickJS `.qbc` assets still need a source-generation step. Upstream binary models, data, fonts and artwork need individual licence/source review; do not infer their licensing from the Java header snapshot.
- Solar artwork attribution is also packaged in `assets/ayuless-notices.txt`. Dependency `licences/` directories are no longer excluded by the root packaging configuration. Verify notices in the built APK.

## New code

Every new file in Ayuless carries:

```
SPDX-License-Identifier: GPL-3.0-or-later
```
