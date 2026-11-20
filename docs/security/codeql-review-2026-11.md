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
