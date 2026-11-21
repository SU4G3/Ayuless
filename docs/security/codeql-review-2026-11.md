# CodeQL review, November 2026

The baseline main-branch analysis reported 618 findings with 120 rules. This is a partial source review, not a clean security audit. Findings without a verified explanation remain open. The exact reviewed dismissal decisions and rationale are recorded in [the triage manifest](codeql-triage-2026-11.json).

## Source changes

All seven SVG parsing entrypoints use a shared SAX reader that explicitly disables external general and parameter entities, rejects external resolution, and rejects DTD declarations through the lexical handler supported by Android Expat. Android already disables external entities by default; this makes the policy explicit and blocks internal DTD expansion too. SVG documents containing a DOCTYPE are intentionally rejected. Alert #272 is tracked by the subsequent analysis rather than dismissed manually.

NumberPicker previously wrapped 10 into -1 for the inclusive range 0–5. The replacement uses the inclusive range width and long intermediate arithmetic, preserving values within range and handling extreme saved integers. Array-index findings in picker callbacks are left for the subsequent analysis and further review rather than dismissed en masse.

## Verification

Standalone checks compile the actual production helper classes with JDK 21. SvgXmlReaderCheck covers normal UTF-8/UTF-16 input, external files, external HTTP DTDs, parameter entities and internal expansion; the HTTP fixture must receive zero requests. PickerBoundsCheck covers the original failure, integer extrema and 10,000 deterministic random ranges. The complete release Java compilation (`:TMessagesProj:compileReleaseJavaWithJavac`, offline, JDK 21) also passed. These checks do not require an Android runtime and do not replace device testing.

Run each check by compiling its Tools/security/*Check.java together with the corresponding production class under org/ayuless/security into a temporary class directory, then running its main class.

## Remaining review

The remaining findings include content access and JavaScript bridges, file/URI handling, arithmetic, credential storage and logging, authentication-token backup, legacy wallet cryptography and network/intent destinations. No global rule exclusion, empty SARIF report or configuration deletion is used to hide them. Intentional JavaScript enablement is accepted only for that individual policy warning; it does not approve other WebView settings or origins.

These changes target main for a future release. They do not modify the published v1.0.0 APK.

## Follow-up: credential logging and URI parsing

The subsequent analysis completed successfully but still reported the SVG finding; the scanner has not recognized the shared reader's protections. The finding remains open rather than being claimed as resolved.

The sensitive-log traces identify real credential-bearing data reaching FileLog. The follow-up removes values at their source: future login token bytes, contact-import tokens, FCM registration tokens, raw deep links, and browser URLs that can contain an autologin token. Contact-import diagnostics retain only the numeric error code because free-form server errors may repeat request data. Event messages remain available with logging enabled. This prevents those particular values from being written by these callsites; it does not erase existing logs or certify all logging in the application. The FCM service is not part of the Google-free application build, but its source is hardened too. The theme debug log also no longer echoes the incoming style parameter.

The URI authority parser's optional, overlapping regex components allowed excessive backtracking on malformed external input (alert #273). The old expression exceeded a five-second subprocess timeout for a 200,000-character input followed by an invalid fragment. The replacement uses possessive quantifiers within delimiter-bounded components while preserving the original RFC 3986 appendix B capture groups. UriParsePatternCheck compares all captures against the old expression on representative links and 20,000 deterministic generated inputs, then checks the long malformed input with the production pattern. The test passed within a 15-second process timeout. Device performance has not been measured.

Alerts #477, #478, #557, #558, #559 and #273 are left to the next analysis; no manual dismissal is used to claim these source changes were recognized by CodeQL.

The follow-up also passed offline `:TMessagesProj:compileReleaseJavaWithJavac` with JDK 21. FCM source remains excluded by the Google-free build configuration. No APK was built or published for this review.

## Follow-up analysis and remaining log paths

Analysis 2217779705 of commit 253115354 completed successfully with 615 findings, leaving 595 open after the 20 earlier dismissals. GitHub marked #273 (URI regex), #477 (contact-token error logging) and #559 (theme log injection) fixed. #478 remains open: the query reports additional paths after the earlier callsites are removed. A reduced aggregate count is not evidence that every sensitive log is resolved.

The next source change removes another direct dump of `future_auth_token` in LoginActivity. It also removes incoming export URI values from both single- and multiple-share diagnostics, usernames/peer IDs from the resolver cache diagnostic, and the rich-media cache key from its diagnostic. Class names and event messages remain available. The ImageLocation trace originates from a broad object flow involving account password state; this review does not establish that a plaintext password was printed there. Removing its unnecessary cache identifier reduces metadata exposure without modifying cache keys or image lookup behavior.

The additional log changes passed offline release Java compilation with JDK 21 (2m 14s). No APK was built or published.

## Fourth review: embedded WebViews, host parsing and validated picker indices

Analysis 2217986696 of d424f3d64 completed with 614 findings and 594 open after previous dismissals. The additional sensitive-log alert was no longer present in the open list.

The article embed WebView loads remote URLs or HTML with an HTTPS base URL; it has no content-provider/file resource requirement. Its explicit content access is disabled, and file access plus file-origin cross-origin access are explicitly disabled. The temporary mini-app popup WebView only forwards navigation to the normal URI handler and then destroys itself; the same local-resource settings are disabled before it is handed to WebViewTransport. JavaScript and normal network embeds remain configured as before. Device tests of article embeds and mini-app external links are still needed; Java compilation does not verify rendering.

LaunchActivity rewrites a recognized username.t.me host into a t.me link. Its previous A-z class admitted ASCII punctuation, and its prefix match also recognized username.t.me.attacker.example as a Telegram subdomain. The replacement explicitly lists ASCII letters, digits, underscore and hyphen, preserves underscore usernames, and requires an absolute end of input. Regression tests exercise the production pattern with valid usernames and invalid punctuation, suffix hosts, paths, userinfo, query strings and trailing newlines.

The exact additional manual decisions are in [the follow-up manifest](codeql-triage-followup-2026-11.json). The 38 picker-index findings were reviewed against the actual min/max bounds, formatter guards, icon callsites and parallel array lengths after the earlier inclusive wrapping fix. They are dismissed as remaining modeling false positives, not as proof that the original wrapping implementation was correct. PickerBoundsCheck again passed edge cases and 10,000 ranges.

The broad control-character sanitization range (#278) and fixed-width protocol-hash wraparound (#518) are intentional. The SVG finding (#272) is now explicitly dismissed after source review of the shared reader and its regression checks; this is a manual false-positive decision, not a claim that the last CodeQL scan recognized the protection. Earlier paragraphs record the decision at the time of each preceding analysis.

The fourth review passed offline release Java compilation with JDK 21, TelegramSubdomainPatternCheck and PickerBoundsCheck. No APK was built or published. Rendering and external-link behavior on a device remain untested for these WebView changes.
