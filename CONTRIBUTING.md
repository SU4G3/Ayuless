# Contributing to Ayuless

## Rules

1. **No copied code** from AyuGram4A, exteraGram or AyuGramDesktop. Reimplement behaviour from its description. Telegram-FOSS and Mercurygram may be read to understand an approach, not copied.
2. **Isolation.** New code goes in `org.ayuless` (`TMessagesProj/src/main/java/org/ayuless/`). Upstream files get the smallest possible hook, marked with a comment:
   ```java
   // Ayuless: <short reason>
   ```
   This keeps `git rebase upstream/master` manageable.
3. **License header.** Every new file starts with:
   ```java
   /*
    * SPDX-License-Identifier: GPL-3.0-or-later
    * Copyright (C) 2026 Ayuless contributors
    */
   ```
4. **No secrets.** Never commit `API_KEYS`, keystores or real API hashes.
5. **No proprietary deps.** No Google Play Services, Firebase, ML Kit or prebuilt binaries.
6. **Verify against source.** When intercepting MTProto requests, check TL class names and behaviour in `TLRPC` / `tl` sources, not from memory.

## Branches

- `master` mirrors `DrKLO/Telegram` (upstream). Don't commit to it.
- `main` is the Ayuless branch. PRs go to `main`.

Sync upstream:

```bash
git remote add upstream https://github.com/DrKLO/Telegram.git
git fetch upstream
git checkout main && git rebase upstream/master
```

## PRs

Each PR describes which upstream files it touches and why, and includes a manual test checklist.
