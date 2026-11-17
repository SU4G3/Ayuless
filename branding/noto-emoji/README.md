# Noto Emoji source artwork

The built-in emoji use Google's Noto Emoji artwork instead of Apple's artwork.
The application still uses Telegram's original EPK3 reader and the two-layer
handshake picker; their Java implementation and Unicode/category positions have
not been changed.

Source repository: https://github.com/googlefonts/noto-emoji

Pinned revision: `99701e07d3d1fe1bdbcdb14964547f0fd592966f`.

`SOURCES.json` records every original path and SHA-256, plus the Unicode sequence
to source mapping. `svg/` is a subset of upstream `2D/svg/`; `flags/` is a subset
of `third_party/region-flags/waved-svg/`. The SVGs are retained without changes.
No Apple emoji artwork or font is used as an input. The complete inputs are
already in the repository, so generating application assets requires no network.

Licences and modifications:

- Noto SVG artwork: Copyright 2013 Google, Inc., Apache-2.0. The original notice
  is in `LICENSE-NOTO.txt`; full terms are in `LICENSE-APACHE-2.0.txt`.
- Flag artwork: public domain or copyright-exempt according to upstream's
  `LICENSE-FLAGS.txt`, retained verbatim. Individual upstream provenance is
  accessible through `third_party/region-flags/html/` at the pinned revision.
- Ayuless rasterizes the SVGs to 64px and encodes lossless WebP images in the
  existing EPK3 FULL_IMAGE records. The renderer oversamples at 128px first.
  The six monochrome/single-tone handshake drawings are separated into their
  existing `Lhand*` and `Rhand*` SVG groups to keep all 36 combinations, including
  independent placeholder tinting and skin-picker animations.
- Noto's flag aliases are used where needed. Ten long-form gender-neutral
  same-tone couple/heart/kiss sequences use Noto's equivalent short-form
  `1f491_<tone>` / `1f48f_<tone>` drawings. No skin tone is discarded.

The copyright, Apache licence and flag notices are packaged with the APK under
`assets/ayuless-notices/` and referenced in `assets/ayuless-notices.txt`.

## Generate and check

On Debian/Ubuntu install `python3-cairosvg` and `python3-pil` (with WebP support).
Ordinary builds use the checked-in pack and handshake PNGs in
`TMessagesProj/src/main/assets/`; regeneration needs no network.
To regenerate and validate them:

```sh
python3 Tools/build-noto-emoji.py TMessagesProj/src/main/assets
python3 Tools/test_noto_emoji.py TMessagesProj/src/main/assets
# Validate the packaged, uncompressed EPK3 asset in a release APK:
python3 Tools/test_noto_emoji.py TMessagesProj_App/build/outputs/apk/afat/release/app.apk
```

The generator verifies every source hash and rejects missing emoji mappings,
external SVG resources and non-vector text/script inputs. The checker decodes
all 3,606 EPK3 images and verifies every category/index and all 36 hand-tone
combinations. Newly added EmojiData sequences require an updated source mapping;
regeneration fails instead of substituting an unrelated glyph or a missing box.

The source manifest lets maintainers retrieve/verify the original subset without
depending on an unpinned font installed on a developer's computer. When updating
Noto, retain its licence files and review mapping/layer changes before regenerating.
