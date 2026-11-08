# Roadmap

Each step is one or more PRs, with a manual test checklist on a test account in the PR description.

Status: ✅ done (merged to `main`) · 🚧 in progress · ⏳ planned

Nothing has been tested on a device yet. All Ayuless features are off by default and live in **Settings → Ayuless**.

> **Known issue:** builds using the public `api_id` of official clients (4, 2040) cannot log in: the server answers `auth.sendCode` with `API_ID_PUBLISHED_FLOOD`. Build with your own `APP_ID` / `APP_HASH` from https://my.telegram.org/apps.

## Step 0: Repository setup ✅
- Fork, README, license audit, icon, CI, CodeQL, Dependabot, issue templates.

## Step 1: Build without Google dependencies
- ✅ `APP_ID` / `APP_HASH` from the `API_KEYS` file via `BuildConfig` (#13)
- ✅ Remove Play Services, Firebase, ML Kit, Play Integrity, Billing, SafetyNet; dependency guard in CI (#14)
- ✅ New app id, name and launcher icon (#15)
- ✅ Maps via MapLibre (#17)
- 🚧 Push via UnifiedPush (#3): draft #30, needs a device test (blocked by the login issue above)
- 🚧 Rebuild all native libs from source in CI, drop prebuilt `.a` (#5). CI builds them from source and links the APK against them, see [NATIVE.md](NATIVE.md) (#26); deleting the committed `.a` files is pending.
- ⏳ `./gradlew assembleAfatRelease` from a clean clone with no proprietary blobs

## Step 2: Ghost mode ✅ (#16, #23, #28, #29, #33)
- ✅ Block `account.updateStatus` (online), read receipts, `messages.setTyping`, story views
- ✅ Go offline right after sending
- ✅ Global profile plus per-account overrides, "N/5" master switch
- ✅ Read on actions (mark read when you send or react), send silently
- ✅ Replying does not send a read receipt while "Don't read messages" is on: every client-side read request (including the ones fired after sending) is filtered; "Read on actions" is the explicit opt-in
- ✅ Offer ghost mode before opening stories, one-time (#29)
- ✅ Ghost button in the chat list, tap to toggle (#28)
- ⏳ Long-press an option to lock its value against the master switch

## Step 3: Spy: anti-delete and edit history ✅ (#18, #19, #22, #23, #27, #31)
- ✅ Own SQLite DB in `org.ayuless` with size limit and cleanup
- ✅ Server deletes keep the message, marked as deleted. Self-deletes behave normally.
- ✅ Edit history from the message menu
- ✅ Exceptions: users/chats that are never saved (#27)
- ✅ Save read date, save last online (local only) (#31)
- ⏳ Include read dates / last online in export/import; apply Spy exceptions to them
- ⏳ Save attachments of deleted messages

## Step 4: "Ayuless" settings section ✅ (#22, #23)
- ✅ Main screen with Ghost mode, Spy, Filters, Customization and project links
- ✅ Customization: translucent deleted messages, deleted mark icon/text and colour
- ✅ Solar icons, credited in [CREDITS.md](CREDITS.md)

## Step 5: Message filters ✅ (#24, #32)
- ✅ Keyword / regex filters (case-insensitive, inverse), shadow ban, hide messages from blocked users
- ✅ Apply filters to the chat list preview and notifications (#32)
- ⏳ Re-apply filters to an already open chat without reopening it

## Step 6 (optional): Backup ✅
- ✅ Export / import / clear the local DB (#23)
- ✅ #12 closed: export/import covers it

## Step 7: Extras ✅ (#33, #34)
- ✅ Hide sponsored messages (toggle) (#34)
- ✅ Local Premium: whole-chat translation only, server-side limits stay (toggle) (#34)
- ✅ Work in background: foreground keep-alive service with a silent notification (#34)
- ✅ Delayed send: send as a short scheduled message so you never appear online (#33); also forwards, story replies and Saved Messages. Not delayed: secret chats, quick reply shortcuts (no `schedule_date` in `messages.sendQuickReplyMessages`), retries of messages that failed as normal sends
