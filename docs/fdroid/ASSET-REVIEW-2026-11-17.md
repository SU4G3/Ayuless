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

## Syntax grammar generation

The pinned libprisma generator ran successfully with its locked npm inputs
(PrismJS 1.29.0 and lodash.isequal 4.5.0) in a disposable copy. Output SHA-256:
`a49d8280da86f5cd462052b0a6f5ff55fe8f67085495e91f151ac21a0638f583`.
The inherited app asset has SHA-256
`45f9933f568884796c17fe57ac832ceb7d9dd072af7cb3291089a331693a9808`.
The output is therefore not claimed byte-identical or silently substituted.
The generator, local components and source lock are present; libprisma and
PrismJS carry the same MIT grant (licence files differ only by final newline).
Their notice is now packaged. Exact output correspondence remains a separate
source-generation review item, not a discovered proprietary licence.


## Additional upstream research

The current F-Droid Forkgram recipe was fetched on 2026-11-17. Its latest
13.2.3.0 entries reference Forkgram/TelegramAndroid commit
`26dbf897e92ed64b5b6033df6484e05e427fa4d7`. Direct downloads at that exact revision
match this tree byte-for-byte for `fonts/gram.ttf`, `fonts/num.otf`,
`secureid_ocr_nn.dat`, `PhoneFormats.dat`, `shapes.dat`, `models/star.binobj` and
`models/diamond_ios/main.bin`. Full hashes/URLs are recorded in
`ADDITIONAL-ASSET-PROVENANCE.json`. The recipe has no `NonFreeAssets` declaration
and explicitly scanignores the diamond model directory. Scanignore is a scanner
exception, not a copyright licence. This establishes a relevant existing recipe
and source precedent; it does not prove which files ship in its APK, independently
clear their rights, or guarantee Ayuless inclusion.

An initial comparison against Forkgram's `master` branch had missing/older files.
It is not the recipe revision and is not used for the conclusions above.
Likewise, paths queried in the current Telegram Desktop tree returned 404; no
Desktop byte-identity or resource licence conclusion is drawn from those paths.

The OCR reader constructs a Genann network with 150 inputs, one 90-node hidden
layer and 37 outputs, then reads its weights directly from the DAT asset.
`jni/genann.h` retains Lewis Van Winkle's zlib-style licence. That grants the
inference library; it does not establish the separate model/training provenance.
No model-specific grant or training recipe was found in this research.

RMPhoneFormat's own README explicitly describes obtaining `Default.phoneformat`
from Apple's private AppSupport framework and renaming it `PhoneFormats.dat`.
Its BSD notice covers the reader, with no express Apple data grant in that README.
This is a concrete origin concern for the original database; the current Ayuless
file's exact relationship to a particular Apple SDK file remains unestablished.
The identical Forkgram file is useful provenance, not proof of Apple's permission.
No formatter or data is replaced based solely on that unresolved relationship.

Sources:
- https://gitlab.com/fdroid/fdroiddata/-/blob/master/metadata/org.forkgram.messenger.yml
- https://github.com/Forkgram/TelegramAndroid/tree/26dbf897e92ed64b5b6033df6484e05e427fa4d7
- https://github.com/rmaddy/RMPhoneFormat/blob/master/README.md
- Local `TMessagesProj/jni/{genann.h,secureid_ocr.cpp}`

A complete Git-blob comparison of the model directory and raw JSON/MP3/WAV
resources found **366 of 366 identical files** at the same Forkgram revision.
`FORKGRAM-ASSET-COMPARISON.json` records every path and both blob IDs.
This strengthens the source precedent for animations and sounds as well; it
does not assign individual authorship or prove the contents of a shipped APK.
