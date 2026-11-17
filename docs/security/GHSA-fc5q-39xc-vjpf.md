# picomatch ReDoS: dependency exposure assessment

Checked 2026-11-17. Dependabot alert [#42](https://github.com/SU4G3/Ayuless/security/dependabot/42)
remains open. This assessment does not mark the vulnerable package fixed or
claim that every use of the development workspace is unaffected.

## Upstream status

[GHSA-fc5q-39xc-vjpf](https://github.com/advisories/GHSA-fc5q-39xc-vjpf)
(CVE-2026-112366) describes exponential regexp backtracking when picomatch receives
crafted repeated extglob patterns. The reviewed advisory lists versions through
4.0.7 as affected and no patched version. The npm registry's latest release was
also 4.0.7 when checked. A downgrade to another affected version is not a fix.

## Dependency path

`TMessagesProj/jni/twidget/widget-sandbox/pnpm-lock.yaml` pins picomatch 4.0.7.
The snapshot references are:

- Vite → picomatch.
- Vite → tinyglobby → picomatch / fdir → picomatch.

Vite is declared as a development dependency in `apps/web/package.json` and
`packages/core/package.json`; core also declares it as an optional peer dependency
for the separately exported Vite integration. Dependabot's lockfile scope is not
proof that this Node package is shipped in the Android APK.

## Android release evidence

The native build script uses esbuild directly through
`packages/core/scripts/build-native-bundles.ts` and `sandbox-build.ts`, not the
Vite development server. Its native entrypoints are:

- `src/sandbox/index.ts`.
- `src/renderer/renderer.ts` with the release's `webkit` transport.
- `src/host/validator-bundle.ts`.

All three bundles were rebuilt with the existing locked inputs and source-built
esbuild 0.28.2, adding `metafile: true` for inspection. Input counts were 255,
10 and 137 respectively. None includes picomatch, Vite, tinyglobby or fdir.
Each generated JS file was compared byte-for-byte with its entry in the signed,
published Ayuless 1.0.0 APK; all three comparisons passed. The summary and SHA-256
values are in [widgetjs-input-audit-2026-11-17.json](widgetjs-input-audit-2026-11-17.json).

This confirms that the vulnerable dependency path is absent from the shipped
WidgetJS bundles. It does not establish general safety of arbitrary widget code
or remove the vulnerable package from the development environment.

## Handling until a patch exists

Keep the alert open and update the lockfile when an actual patched release is
available. Do not feed untrusted patterns into the affected Node tooling. The
web demo already binds to `127.0.0.1`; do not expose that development server to
untrusted networks. Android release bundling does not need that server.

No package version, application code or release APK was changed by this review.
A new APK is not required solely for this advisory because the affected Node
library is absent from the inspected shipped bundle path. Any later dependency
update should repeat the bundle/input comparison.
