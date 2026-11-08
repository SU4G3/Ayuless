# Contributing to Ayuless

## Rules

1. **Code from other clients.** Code taken from AyuGram4A or AyuGramDesktop (© Radolyn Labs) is allowed only if:
   - the original copyright header is kept (e.g. `Copyright @Radolyn, 2023`) and the file gets the SPDX identifier of the source license (AyuGramDesktop: `GPL-3.0-or-later` compatible; AyuGram4A ships the GPL v2 text without "or later", so mark it `GPL-2.0-or-later` only once the authors confirm, see [docs/CREDITS.md](docs/CREDITS.md));
   - the file is listed in [docs/CREDITS.md](docs/CREDITS.md) under "Code derived from AyuGram".
   Do not copy code from exteraGram. AyuGram4A is based on exteraGram, so check that a piece you take is AyuGram's own code (`com.radolyn.ayugram`), not exteraGram's.
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
