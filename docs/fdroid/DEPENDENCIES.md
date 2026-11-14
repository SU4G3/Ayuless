# Classpath and POM review

Local evidence, 2026-11-12. This is not a resolved release graph, an SDK source
audit, a tracker scan or a network-traffic test.

`python3 Tools/fdroid/review_poms.py` produces [POM-INVENTORY.tsv](POM-INVENTORY.tsv)
from declarations in the library, root buildscript and buildSrc. It reads only
cached POMs, follows parent licence declarations, records checksums and source /
licence URLs, and reports missing or conflicting metadata instead of fetching it.
POMs are publisher claims; their presence does not verify an AAR's actual contents
or the licence of all transitives. Build tools, compile-only dependencies and
runtime libraries are intentionally identified as **declarations**, not asserted
to be the selected APK classpath.

The available AndroidX, Markwon, Gson, websocket, mp4parser, Moshi, JavaParser,
Kotlin and build-tool POMs predominantly declare Apache-2.0; MapLibre declares
BSD, NanoHTTPD BSD, Guava/ZXing Apache-2.0 via parents. desugar_jdk_libs declares
GPLv2 with the Classpath exception; checker annotations declare GPLv2 with the
Classpath exception / MIT. Preserve applicable notices. Check the TSV for the
exact declaration and parent evidence, including any missing POM.

## Stripe 2.0.2

The cached `com.stripe:stripe-android:2.0.2` POM declares **MIT** and links
[stripe/stripe-android](https://github.com/stripe/stripe-android). Its only listed
dependency is `com.android.support:support-annotations:25.1.0` (compile scope).
There is no proprietary SDK declared in that POM. The licence URL references
`master`, so retrieve and retain the licence/source for **2.0.2** in the release
review rather than trusting a mutable current branch.

The cached AAR has SHA-256
`1affcb48131775fa5b31b8df02e49e4bb6ca1dd6d5c379ee5700d28cd8cd4bba`.
Its classes.jar contains 40 class files, all under `com/stripe/android`, and no
bundled nested JAR, native library or asset payload was found. Literal HTTPS URLs
in those classes point to `api.stripe.com`, `stripe.com` (API/docs), and a
`twitter.com/stripestatus` help link. This small archive/constant-pool inspection
does **not** prove absence of telemetry or dynamic connections. PaymentFormActivity
constructs Stripe when using that optional payment provider. Preserve the payment
feature; review SDK source, errors/diagnostics, request fields and actual traffic
on a prepared build. Required Telegram and optional payment services remain
disclosed in metadata; no unverified Tracking Anti-Feature is invented.

## Complete the release review

With pinned recursive submodules, Android SDK and a writable Gradle cache:

```bash
./gradlew --offline :TMessagesProj_App:dependencies \
  --configuration afatReleaseRuntimeClasspath > /tmp/afat-runtime.txt
./gradlew --offline :TMessagesProj_App:dependencyInsight \
  --configuration afatReleaseRuntimeClasspath --dependency stripe-android
```

Resolve every selected component, including native/Cargo/npm dependencies and
generated merged resources; match each artifact to source/licence and check for
proprietary SDKs. Review the complete POM/Gradle-module metadata graph, not only
the direct POM sample. Then run fdroidserver's source scanner and scan the final
APK for tracker/native signatures and packaged notices. Record selected versions,
artifact hashes and the review results before enabling the recipe.

The requested Java compile probe failed while opening the root Gradle wrapper's
lock in read-only `~/.gradle`, before configuration or resolution. The missing
native submodules also prevented a complete release build. No successful classpath
resolution or scanner run was claimed by that initial offline review.

## Online follow-up, 2026-11-13

All 19 submodules were initialized and `afatReleaseRuntimeClasspath` resolved
successfully. No GMS/Firebase/Huawei dependency appeared in the selected graph.
Java release compilation passed with JDK 21 and public example credentials in
the process environment. F-Droid scanning of the recipe-prepared source tree
with installed npm dependencies found zero fatal problems; scanning the previous
PR #62 test APK also found zero known non-free-code/signing-block problems.
This supersedes the tooling/access limitations above, but does not establish
every transitive licence or actual runtime network behaviour. Source-built
esbuild and the cleanup exceptions are documented in
[VALIDATION-2026-11-13.md](VALIDATION-2026-11-13.md).
