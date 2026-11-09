# Roadmap

Each step is one or more PRs, with a manual test checklist on a test account in the PR description.

Status: ✅ done (merged to `main`) · ✅ in progress · ⏳ planned

Nothing has been tested on a device yet. All Ayuless features are off by default and live in **Settings → Ayuless**.

> **Known issues**
> - Signing in with a new session may fail on current builds (reported after the first device test, cause not known yet). An existing session works.
> - Ported AyuGram4A code is GPLv2 without "or later", while Ayuless is GPL-3.0-or-later: needs Radolyn's permission or a license decision.

First test build: [v0.1.0-alpha.1](https://github.com/SU4G3/Ayuless/releases/tag/v0.1.0-alpha.1) (pre-release).

## Step 0: Repository setup ✅
- Fork, README, license audit, icon, CI, CodeQL, Dependabot, issue templates.

## Step 1: Build without Google dependencies
- ✅ `APP_ID` / `APP_HASH` from the `API_KEYS` file via `BuildConfig` (#13)
- ✅ Remove Play Services, Firebase, ML Kit, Play Integrity, Billing, SafetyNet; dependency guard in CI (#14)
- ✅ New app id, name and launcher icon (#15)
- ✅ Maps via MapLibre (#17)
- ✅ Push via UnifiedPush (#3): draft #30, needs a device test (blocked by the login issue above)
- ✅ Rebuild all native libs from source in CI, drop prebuilt `.a` (#5). CI builds them from source and links the APK against them, see [NATIVE.md](NATIVE.md) (#26); deleting the committed `.a` files is pending.
- ⏳ `./gradlew assembleAfatRelease` from a clean clone with no proprietary blobs

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
- ⏳ Re-apply filters to an already open chat without reopening it

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

## Not done yet
- Fix new-session login, then test everything on a device.
- UnifiedPush device test and merge of draft #30 (#3).
- Check `./gradlew assembleAfatRelease` from a clean clone with the native build documented in [NATIVE.md](NATIVE.md).
- Resolve the AyuGram4A license question.
- Remaining AyuGram parity items: [AYUGRAM-PARITY.md](AYUGRAM-PARITY.md).
