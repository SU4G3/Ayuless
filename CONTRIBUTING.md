# Contributing to Ayuless

## Rules

1. **Clean-room boundary.** Do not introduce code, comments or resource wording derived from AyuGram4A or exteraGram. Phase-B contributors use [docs/CLEANROOM-SPEC.md](docs/CLEANROOM-SPEC.md) and the remaining independent sources, without opening the reference checkout, removed implementation, or historical Git patches/objects containing it. Keep independent Ayuless code and Telegram's GPL-2.0-or-later code and notices. Source-export handoffs must exclude Git history and the reference checkout.
2. **Isolation.** New code goes in `org.ayuless` (`TMessagesProj/src/main/java/org/ayuless/`). Upstream files get the smallest possible hook, marked with a comment:
   ```java
   // Ayuless: <short reason>
   ```
   This keeps `git rebase upstream/master` manageable.
3. **License header.** Every new file written for Ayuless starts with:
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
