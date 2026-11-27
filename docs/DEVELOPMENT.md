# Development and GitHub checks

Changes target `main` through pull requests and use squash merge. Keep feature
branches small enough to review. `master` mirrors Telegram upstream.

## Workflows

| Workflow | When it runs | Result |
| --- | --- | --- |
| CI | Every PR to `main`, every push to `main`, or manually | Required `Repository checks`: workflow lint, translation validation/tests, binary string lookup regression, registration estimate and ghost privacy tests, and the DeGoogle source audit |
| Build | App, Gradle or native build input changes; or manually | Universal `afat` APK, source-built native libraries and packaged emoji validation |
| CodeQL | Non-documentation PRs/pushes, weekly, or manually | Java source and GitHub Actions security analysis |
| Native libraries | Manually | Native archives and headers for investigating the native build independently |
| Crowdin translations | Daily, source/config changes, or manually | A reviewed-translation PR and a `Repository checks` commit status after checking its actual head commit |

CI does not need the Android SDK or repository secrets. It runs even on
documentation changes so its required status cannot be left pending by path
filters. Expensive APK builds are filtered to application/build inputs.
After changing the Build workflow itself, run it manually if an APK build is
needed to validate the change. All external Actions are pinned to commit SHAs;
Dependabot proposes grouped Actions updates weekly.

The native action caches libraries by source/build input hashes and validates
restored outputs. The separate native workflow is manual because Build already
performs the same source build and validation. Native libraries are not checked
into Git.

## Local checks

With Python 3 and JDK 21 on `PATH`, run from the checkout:

```sh
bash Tools/ci/check.sh
```

To check workflow syntax with the same pinned linter as CI:

```sh
go run github.com/rhysd/actionlint/cmd/actionlint@v1.7.12 -shellcheck= -pyflakes=
```

The linter command downloads its Go dependencies on first use. The repository
checks themselves use Python's standard library and the JDK. These host checks
do not replace compiling the Android app or testing behavior on a device.

## APK artifacts and releases

Open a successful Build run under **Actions**, then download `ayuless-apk`.
It contains only `TMessagesProj_App`'s universal `afat` APK. Artifacts expire
after 14 days; the run summary records its SHA-256. Native diagnostic artifacts
expire after 7 days.

CI APKs use the repository's public test signing key. Official release APKs use
the separate private release key, so the two are not interchangeable as updates.
See [release signing](RELEASE-SIGNING.md). No workflow uploads or stores the
private release key, and no workflow automatically publishes a release.
`.github/release.yml` only groups GitHub's generated notes; release descriptions
still need the project's bilingual format and actual shipped changes.

## Review and maintenance

`Repository checks` must pass for ordinary PR merges. App changes also need a
successful Build and relevant device testing; workflow/security changes should
have their CodeQL results reviewed. The owner retains the existing ruleset
bypass for repository maintenance. Force pushes and branch deletion are blocked
for `main` outside that bypass.

Reports start with `needs-triage`; maintainers classify and reproduce them before
promising a fix. Use `bug`, `enhancement`, `translations`, `documentation` and
the existing feature labels. Dependabot uses `dependencies` and `build`.
Neither dependency updates nor translation PRs are auto-merged.

Use [Crowdin](https://crowdin.com/project/ayuless) for translations and
[TRANSLATING.md](TRANSLATING.md) for its integration settings. Report security
issues through [SECURITY.md](../SECURITY.md).
