# Ayuless launcher variants

These icons use Ayuless's own chevron/dot mark. Classic uses a blue background
and a folded-paper color treatment; Ocean uses a blue/green gradient. Premium,
Turbo and Night use their respective pink/purple, cyan/blue and starry dark styles.
The default Ayuless icon is unchanged.

The artwork is independently drawn, rather than reusing Telegram's paper-plane
logo. [Telegram API terms §2.4](https://core.telegram.org/api/terms) prohibit using
the official Telegram logo for a third-party app. This is the branding reason for
adapting these styles to Ayuless; it is not a claim that Telegram's source license
forbids all reuse of its resources.

Regenerate with:

```sh
python3 branding/generate-launcher-variants.py
```

Requires ImageMagick with SVG support. The script writes the source SVGs here,
Android background/foreground drawables, adaptive and monochrome icon resources,
and ordinary/round PNG fallbacks at all five Android launcher densities. Legacy
PNGs use the same full-size glyph as the original default bitmap; adaptive
foregrounds apply the existing 0.72 safe-zone scale. Runtime code does not need
ImageMagick or SVG support.

The picker preview, launcher aliases in both main and standalone manifests, and
adaptive resources reference the same variants. Component names remain unchanged
so the selected alias can survive an update. All six choices are available without
Telegram Premium; the enum's `premium` field only groups artwork in the upstream
Premium preview screen.

Artwork and generator: Copyright (C) 2026 Ayuless contributors, GPL-3.0-or-later.
