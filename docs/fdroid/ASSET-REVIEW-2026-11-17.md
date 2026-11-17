# Resource review, 2026-11-17

The previous inventory mixed missing research with confirmed incompatibilities.
An `UNKNOWN` cell is not evidence of infringement or a demand to replace a
resource. Standard Telegram fonts are retained. F-Droid requires applicable
redistribution rights; it does not require every fork to redraw inherited fonts.

## Text fonts

Embedded font name tables identify the following families and versions:

| Files | Version | Applicable family licence / retained evidence |
| --- | --- | --- |
| `rmedium`, `rmediumitalic`, `ritalic` | 2.138 | Roboto 2, Apache-2.0 at tag v2.138 |
| `rcondensedbold` | 2.137 | Roboto 2, Google 2011 copyright; the same Apache-2.0 family grant |
| `rextrabold` | 3.009 | Roboto 3 Classic v3.009, OFL-1.1 |
| `rmono`, `rmono_var` | 3.000, 3.001 | Roboto Mono v3.001 OFL-1.1; variable file also embeds the grant |
| `mw_bold` | 2.002 | Merriweather v2.002 addition in google/fonts b848f42f; OFL-1.1, 2016 copyright and RFN |
| `mw_bolditalic` | 2.101 | Merriweather4 family OFL-1.1 at e586023a; embedded 2024 copyright/RFN retained alongside upstream's 2020 notice |

`FONT-LICENSE-SOURCES.json` records full revisions, original paths, URLs and
SHA-256. The original notices are packaged under `assets/ayuless-notices/`.
The mapping relies on embedded family/version/copyright evidence; it does not
claim every font is byte-identical to the fonts built by that upstream commit.
There is no detected contrary grant for these files. No typeface was replaced.

## JLaTeXMath

`settings.gradle` includes only the main `jlatexmath` module. The separate
`jlatexmath-font-greek` module contains the GPLv2 Greek fonts mentioned in its
root licence and is not an app dependency. Its presence in the source submodule
does not mean these optional font assets are packaged in the APK.
The main module already provides its per-font notices in its assets. Its root
licence, including the linking exception, is now packaged separately too.
Final APK notice/contents verification remains part of the release build.

## Inherited resources

`Tools/fdroid/audit_upstream_assets.py` checks current file contents against the
actual Git tree of Telegram `7be672ead283d022812d75c729d3a04dd4c2a2c6`.
`UPSTREAM-ASSET-PROVENANCE.tsv` records every comparison: 5,345 resources match;
127 are modified/new or outside the compared source paths. This comparison is
independent of filenames and does not assign copyright ownership.

The Telegram repository is distributed with GPL notices and invites source
forks. That is relevant provenance; it is not a basis for inventing a separate
licence for a third-party asset. No contrary licence was identified in this
review for the inherited gram/numbers fonts, OCR weights, model data, animations
or recordings. Their individual authorship/training or editable inputs have not
all been established. These are explicit reviewer questions, not findings that
all those resources are proprietary. No features are removed on that assumption.

The earlier Apple-derived emoji had a specific third-party rights concern and
were replaced with Noto. That finding is not generalized to the other resources.

Related evidence and policy:
[licence audit](../LICENSE-AUDIT.md), [source inventory](ASSET-INVENTORY.tsv),
[F-Droid inclusion policy](https://f-droid.org/en/docs/Inclusion_Policy/),
[Telegram repository](https://github.com/DrKLO/Telegram/tree/7be672ead283d022812d75c729d3a04dd4c2a2c6).
