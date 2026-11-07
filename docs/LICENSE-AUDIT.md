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
- **Native submodules** (BoringSSL, FFmpeg, libvpx, dav1d, opus, openh264, tdlib, abseil, WAMR, ...) are under permissive or LGPL licenses. All GPLv3-compatible. FFmpeg must be built without `--enable-nonfree`.

## Proprietary parts to remove

These are not license-compatible with a fully free build and are tracked in the roadmap (step 1):

- `TMessagesProj/jni/prebuild/lib/**/*.a`: 64 prebuilt static libs (204 MB). Must be rebuilt from source with the scripts in `TMessagesProj/jni/prebuild/`.
- `TMessagesProj/libs/libgsaverification-client.aar`: closed Google binary.
- Gradle deps: `com.google.android.gms:*`, `com.google.firebase:*`, `com.google.mlkit:*`, `com.google.android.play:integrity`, `com.android.billingclient:billing`, `androidx.credentials:credentials-play-services-auth`.

## New code

Every new file in Ayuless carries:

```
SPDX-License-Identifier: GPL-3.0-or-later
```
