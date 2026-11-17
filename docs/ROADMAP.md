# Roadmap

Each step is one or more PRs, with a manual test checklist on a test account in the PR description.

Updated: 2026-11-17.

Status: ✅ implemented in the released source or merged history · 🚧 integration/review pending · ⏳ planned

The 1.0.0 APK includes the tested UnifiedPush/Noto source candidate. The full
build and APK checks pass, with positive owner device feedback; exhaustive
feature testing remains pending.
Ayuless settings live in **Settings → Ayuless**. Anti-delete, saved attachments,
sponsored-message hiding and background work default on; optional additions
default off. Explicit saved choices are preserved.

Current release work:
- Login fingerprint fix merged (#56); the earlier new-session login report is
  no longer an unresolved implementation task. Login still needs regression
  testing on the final release candidate.
- The clean-room rewrite replaced the AyuGram-derived implementation (#60),
  keeping GPL-3.0-or-later. Source provenance remains subject to review; see
  [CLEANROOM-REMOVED.md](CLEANROOM-REMOVED.md).
- F-Droid metadata and recipe draft merged (#61). Source-build improvements and
  the Noto emoji replacement are merged into `main` via [PR #62](https://github.com/SU4G3/Ayuless/pull/62),
  including the implementation from PR #30. The complete official-image build and APK checks pass;
  [packaging request #4819](https://gitlab.com/fdroid/rfp/-/work_items/4819) is submitted.
- Ayuless 1.0.0 is published with the private release signing key. Its APK was
  validated and the owner reported it works. No automatic APK publisher is running.

Latest public release: [v1.0.0](https://github.com/SU4G3/Ayuless/releases/tag/v1.0.0). Android retains Telegram base version 13.2.1 (72919). The release includes the tested UnifiedPush/Noto candidate; exhaustive feature testing remains pending.
Private test APKs: [test-cleanroom](https://github.com/SU4G3/Ayuless-reports/releases/tag/test-cleanroom).

## Step 0: Repository setup ✅
- Fork, README, license audit, icon, CI, CodeQL, Dependabot, issue templates.

## Step 1: Build without Google dependencies
- ✅ `APP_ID` / `APP_HASH` from the `API_KEYS` file via `BuildConfig` (#13)
- ✅ Remove Play Services, Firebase, ML Kit, Play Integrity, Billing, SafetyNet; dependency guard in CI (#14)
- ✅ New app id, name and launcher icon (#15)
- ✅ Maps via MapLibre (#17)
- ✅ UnifiedPush (#3, PR #30) is released in 1.0.0 and merged into `main` through #62. The standalone PR is closed as incorporated. Additional multi-account/fallback regression coverage remains.
- ✅ Rebuild all native libs from source in CI, drop prebuilt `.a` (#5). CI builds them from source and links the APK against them, see [NATIVE.md](NATIVE.md) (#26); committed `.a` files removed (#47).
- ✅ Full source-built native/WidgetJS/afat release APK and scanner checks completed in the official F-Droid image.

## Step 2: Ghost mode ✅ (#16, #23, #28, #29, #33)
- ✅ Block `account.updateStatus` (online), read receipts, `messages.setTyping`, story views
- ✅ Go offline right after sending
- ✅ Global profile plus per-account overrides, "N/5" master switch
- ✅ Read on actions (mark read when you send or react), send silently
- ✅ Replying does not send a read receipt while "Don't read messages" is on: every client-side read request (including the ones fired after sending) is filtered; "Read on actions" is the explicit opt-in
- ✅ Offer ghost mode before opening stories, one-time (#29)
- ✅ Ghost button in the chat list, tap to toggle (#28)
- ✅ Long-press an option to lock its value against the master switch (#37)
- ✅ Delayed send also for forwards, story replies and Saved Messages (#41)
- ✅ Delayed send in secret chats, held on the client (no server scheduling there) (#43)
- ✅ Block more client-side read paths, including reads fired after sending; whether the server marks a chat read on send by itself is not confirmed (#45)

## Step 3: Spy: anti-delete and edit history ✅ (#18, #19, #22, #23, #27, #31)
- ✅ Own SQLite DB in `org.ayuless` with size limit and cleanup
- ✅ Server deletes keep the message, marked as deleted. Self-deletes behave normally.
- ✅ Edit history from the message menu
- ✅ Exceptions: users/chats that are never saved (#27)
- ✅ Save read date, save last online (local only) (#31)
- ✅ Include read dates / last online in export/import; apply Spy exceptions to them
- ✅ Save attachments of deleted messages, with a size limit (#38)
- ✅ Optional retention of secret chats, TTL / auto-delete messages, view-once media and encrypted actions, each behind its own toggle (#44)
- ✅ Secret chats: save read dates and last online from typing (#43)

## Step 4: "Ayuless" settings section ✅ (#22, #23)
- ✅ Main screen with Ghost mode, Spy, Filters, Customization and project links
- ✅ Customization: translucent deleted messages, deleted mark icon/text and colour
- ✅ Solar icons, credited in [CREDITS.md](CREDITS.md)
- ✅ Ayuless entry as its own first section under the profile, with the Ayuless logo; rounded card sections; `LOC_ERR:null` fix (#42)

## Step 5: Message filters ✅ (#24, #32)
- ✅ Keyword / regex filters (case-insensitive, inverse), shadow ban, hide messages from blocked users
- ✅ Apply filters to the chat list preview and notifications (#32)
- ✅ Apply filters in secret chats too (#45)
- ✅ Re-apply filters to an already open chat without reopening it (#48)

## Step 6 (optional): Backup ✅
- ✅ Export / import / clear the local DB (#23)
- ✅ #12 closed: export/import covers it

## Step 7: Extras ✅ (#33, #34)
- ✅ Hide sponsored messages (toggle) (#34)
- ✅ Local Premium: whole-chat translation only, server-side limits stay (toggle) (#34)
- ✅ Work in background: foreground keep-alive service with a silent notification (#34)

## Step 8: Restricted content ✅
- ✅ Ignore restricted saving in groups and channels: save, copy, forward as copy (#40)
- ✅ Optional: also ignore it in private chats with "disable sharing", auto-delete chats, TTL / view-once messages, secret chats and stories with saving disabled; no `FLAG_SECURE` there (#44)

## Step 9: Login ✅
- ✅ Client identity and AyuGram-style login spoof: during sign-in the client presents itself as an older official build (layer 177), then switches back to the real version without dropping the auth key (#35)

## Step 10: F-Droid preparation 🚧 (#61, #62)
- ✅ Store metadata, four current screenshots and source tag `fdroid-13.2.1-72919`.
- ✅ WidgetJS/QuickJS, all 64 native archives and unsigned APK built in the official F-Droid image; source/APK scans pass.
- ✅ Noto replaces Apple emoji; retained SVGs/notices, 3,606 images and 36 tone combinations checked in the final APK.
- ✅ Signed test copy uploaded privately; owner reported it works on a phone.
- ✅ Known font/native licences packaged; inherited fonts/data and 366 models/animations/sounds compared with the Forkgram recipe source.
- ✅ [Packaging request #4819](https://gitlab.com/fdroid/rfp/-/work_items/4819) submitted.
- ✅ [Metadata MR !57850](https://gitlab.com/fdroid/fdroiddata/-/merge_requests/57850) submitted and linked to RFP #4819.
- ⏳ MR closed by a maintainer: the required MR template was not followed and the project does not yet meet the three-month maintenance criterion. Revisit inclusion with the required history or evidence for a policy exception. GitHub releases continue independently.
- ⏳ Two-clean-APK reproducibility comparison; individual provenance and login-identity review context remains disclosed.

Detailed status: [F-Droid checklist](fdroid/CHECKLIST.md),
[validation evidence](https://github.com/SU4G3/Ayuless/blob/v1.0.0/docs/fdroid/VALIDATION-2026-11-17.md),
[licence audit](LICENSE-AUDIT.md).

## Not done yet
- Broaden final-candidate UnifiedPush multi-account delivery, distributor lifecycle and fallback tests.
- Revisit F-Droid submission after maintenance-history/template requirements or a policy exception are addressed.
- Finish remaining resource licence/provenance review and release validation.
- Remaining AyuGram parity items: [AYUGRAM-PARITY.md](AYUGRAM-PARITY.md).
