# Roadmap

Work goes in this order. Each step is one or more PRs, with a manual test checklist on a test account.

## Step 0: Repository setup ✅
- Fork, README, license audit, icon, CI skeleton, issue templates.

## Step 1: Build without Google dependencies
- Load `APP_ID` / `APP_HASH` from the `API_KEYS` file via `BuildConfig`, not hardcoded in `BuildVars`.
- Remove Play Services, Firebase, ML Kit, Play Integrity, Billing, SafetyNet, `libgsaverification-client.aar`.
- Push via UnifiedPush; maps via MapLibre.
- Rebuild all native libs from source in CI (no prebuilt `.a`).
- New app id, name and adaptive icon from `branding/`.
- `./gradlew assembleAfatRelease` works from a clean clone.

## Step 2: Ghost mode
Intercept outgoing MTProto requests at send level (exact TL names verified against source):
- `account.updateStatus` (online)
- `messages.readHistory`, `channels.readHistory`, `messages.readMessageContents`, `channels.readMessageContents`
- `messages.setTyping`
- `stories.readStories`, `stories.incrementStoryViews`
- Option: send `account.updateStatus(offline=true)` right after sending a message.

## Step 3: Anti-delete + edit history
- Own SQLite DB in `org.ayuless` (schema versions + migrations, size-based cleanup).
- Server deletes keep the message, shown greyed out. Self-deletes behave normally and are not stored.
- Edit history: every previous version, reachable from the message menu.
- Never store secret chats or messages with a self-destruct timer.

## Step 4: "Ayuless" settings section
- All toggles in one place, per account where relevant.

## Step 5 (optional): Backup
- Export / import local deleted and edited history.
