# Roadmap

Each step is one or more PRs, with a manual test checklist on a test account in the PR description.

Status: ✅ done (merged to `main`) · 🚧 in progress · ⏳ planned · ❌ not planned

Nothing has been tested on a device yet. All Ayuless features are off by default and live in **Settings → Ayuless**.

## Step 0: Repository setup ✅
- Fork, README, license audit, icon, CI, CodeQL, Dependabot, issue templates.

## Step 1: Build without Google dependencies
- ✅ `APP_ID` / `APP_HASH` from the `API_KEYS` file via `BuildConfig` (#13)
- ✅ Remove Play Services, Firebase, ML Kit, Play Integrity, Billing, SafetyNet; dependency guard in CI (#14)
- ✅ New app id, name and launcher icon (#15)
- ✅ Maps via MapLibre (#17)
- 🚧 Push via UnifiedPush (#3)
- 🚧 Rebuild all native libs from source in CI, drop prebuilt `.a` (#5)
- ⏳ `./gradlew assembleAfatRelease` from a clean clone with no proprietary blobs

## Step 2: Ghost mode ✅ (#16, #23)
- ✅ Block `account.updateStatus` (online), read receipts, `messages.setTyping`, story views
- ✅ Go offline right after sending
- ✅ Global profile plus per-account overrides, "N/5" master switch
- ✅ Read on actions (mark read when you send or react), send silently
- 🚧 Warn before opening stories
- 🚧 Ghost indicator in the chat list

## Step 3: Spy: anti-delete and edit history ✅ (#18, #19, #22, #23)
- ✅ Own SQLite DB in `org.ayuless` with size limit and cleanup
- ✅ Server deletes keep the message, marked as deleted. Self-deletes behave normally.
- ✅ Edit history from the message menu
- ✅ Never store secret chats or self-destructing messages
- 🚧 Exceptions: users/chats that are never saved
- 🚧 Save read date, save last online (local only)
- ⏳ Save attachments of deleted messages

## Step 4: "Ayuless" settings section ✅ (#22, #23)
- ✅ Main screen with Ghost mode, Spy, Filters, Customization and project links
- ✅ Customization: translucent deleted messages, deleted mark icon/text and colour
- ✅ Solar icons, credited in [CREDITS.md](CREDITS.md)

## Step 5: Message filters ✅ (#24)
- ✅ Keyword / regex filters (case-insensitive, inverse), shadow ban, hide messages from blocked users
- 🚧 Apply filters to the chat list preview and notifications

## Step 6 (optional): Backup
- ✅ Export / import / clear the local DB (#23)
- 🚧 Decide whether #12 needs more than that

## Not planned ❌
- Local Telegram Premium, ad blocking, keep-alive "work in background", delayed send
