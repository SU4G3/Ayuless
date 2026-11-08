# Settings visual specification

Reference: the default settings in `vanilla-AyuGram-lite-20261104.apk`, inspected
read-only in `/tmp/ayugram-apk`. Dimensions below are dp (including text sizes,
which these components set with `COMPLEX_UNIT_DIP`), unless stated otherwise.
This is a visual specification, not a source port. Ayuless uses its own logo,
translations and existing Solar icons; no exteraGram code or assets are copied.

| Element | Reference geometry and appearance |
| --- | --- |
| Screen / cards | `windowBackgroundGray` behind `windowBackgroundWhite` cards. Side inset **12**, corner radius **20** by default. The first card without a preceding header has **4** top inset. Empty section spacer **12**. No elevation or drop shadow between cards. |
| Section headers | Outside the card, on the screen background (the reference defaults `sectionsSeparatedHeaders` to true). **14**, bold, `windowBackgroundWhiteBlueHeader`; horizontal inset **24** from the screen edge, top margin **6**, bottom margin **3**. Header minimum height **40 + 3**; text occupies at least **34**. Together with an empty spacer, this leaves **55** between successive cards when the next has a header. |
| Navigation / value rows | Height **50**, plus **1 physical pixel** when divided. Title **16**, regular, `windowBackgroundWhiteBlackText`. Icon intrinsic size **24**, start inset **21**, title start **71** when an icon is present; no icon background. Without an icon, title starts at **21**. Trailing value **16**, `windowBackgroundWhiteValueText`, end inset **21**. All directions mirror in RTL. |
| Secondary text | Navigation subtitle **13**, `windowBackgroundWhiteGrayText`; the reference helper selects a **64**-high row with title start **60** for subtitle rows. Switch subtitle **13**, `windowBackgroundWhiteGrayText2`; subtitle switch row **64**. Explanations outside cards: **14**, `windowBackgroundWhiteGrayText4`, screen inset **24**, padding **10** above / **17** below. |
| Dividers / selection | **1 physical pixel**, `divider`; navigation divider start **68** with icon / **20** without, extending to the end edge. Switch divider start **21**. No divider after the last row of a card. Selection uses the theme list selector (`settings_listSelector` in Ayuless's Telegram base). |
| Switches | Layout **37 × 20**, end margin **22**, ordinary label **16**, row **50**. The default `newSwitchStyle` is enabled: drawn track **42.25 × 26**, radius **13**, off-state outline **1.625**; thumb diameter **13** off / **19.5** on, center travel **16.25**. Drawing extends outside the layout box; the row disables child clipping. These are the Material-style dimensions **52 × 32**, outline **2**, thumb **16 / 24**, travel **20**, scaled by **0.8125**. Track keys `switchTrack` / `switchTrackChecked`; on thumb `windowBackgroundWhite` (Monet: `switchTrackBlueThumbChecked`). Off colors derive from `switchTrack` in Lab: fill L* **90 / 22** on light/dark surfaces; thumb L* **50 / 60**, chroma capped at **32**. Non-Monet thumb/outline blends fill toward that thumb tone by **0.55 / 0.8**. The optional classic reference style is **31 × 14**, thumb **20**; Ayuless follows the default modern appearance. |
| Branded top header | Full-width block on the gray screen background, custom-shadow item height **198**. Centered **72 × 72** logo with default shape radius **18**; top margin **28**. Name **20**, bold, `windowBackgroundWhiteBlackText`, top gap **16**, horizontal margins **50**. Version + installed version code **15**, bold, `windowBackgroundWhiteGrayText`, top gap **2**, horizontal margins **60**, bottom margin **28**, extra line spacing **2**. No separate white card behind the banner. |
| Main Settings entry | A **50**-high single-line row, regular **16**, `windowBackgroundWhiteBlackText`, no subtitle/value. Icon tile **28 × 28**, with **24 × 24** foreground, start margin **18** and **20** gap before the text; trailing text inset **20**. Reference has a flat branded purple tile; Ayuless instead renders its complete, untinted `ayuless_settings_logo` at **28 × 28**. |

Evidence (paths relative to `/tmp/ayugram-apk`):

- `smali_classes5/com/radolyn/ayugram/preferences/AyuMainPreferencesActivity.smali`:
  `fillItems` uses the **0xc6 = 198** header item, categories, and section spacers;
  `getListTopPadding` adds **12** to the system inset.
- `smali_classes4/com/exteragram/messenger/preferences/BasePreferencesActivity.smali`:
  `createView` selects sectioned cards; `uButton` selects ordinary Telegram rows
  or **0x40 = 64** subtitle rows with **0x3c = 60** text start.
- `smali_classes3/org/telegram/ui/Components/UniversalRecyclerView.smali`:
  `setSections()` supplies **12**, configured radius, and no full top padding.
  `smali/com/exteragram/messenger/ExteraConfig.smali` initializes the radius to
  **20.0f** and enables separated headers. These are configurable reference
  defaults; Ayuless applies the measured geometry only to settings screens.
- `smali_classes4/com/exteragram/messenger/preferences/components/HeaderSettingsCell*.smali`:
  logo, name and version dimensions; `getPath` requests **72 × 72 / 18**.
- `smali_classes6/org/telegram/ui/Cells/{HeaderCell,TextCell,TextCheckCell,TextInfoPrivacyCell}.smali`
  and `smali_classes3/org/telegram/ui/Components/UniversalAdapter.smali`:
  typography, Theme keys, padding, row heights, dividers and **12** spacers.
- `smali_classes7/org/telegram/ui/Components/Switch.smali` and
  `smali_classes4/com/exteragram/messenger/utils/ui/{SwitchUiHelper,ColorTones}.smali`:
  switch geometry, **0.8125** scale and theme-derived Lab tones. Ayuless draws its
  own capsule and thumb using Telegram's existing state animator; the reference's
  extra spring deformation is not reproduced.
- `smali_classes6/org/telegram/ui/SettingsActivity.smali`, `fillItems`, and
  `SettingsActivity$SettingCell.smali`: single-line branded entry and tile layout.
- `res/drawable/{ayu_ghost,msg_filter}.xml`: **24 × 24** navigation icons.
  `res/drawable/ic_background_settings.xml` and `ic_logo_foreground2.xml` are
  reference branding only and are not imported. `res/layout/` and
  `res/values/dimens.xml` do not define this settings geometry: it is constructed
  programmatically. In particular, `cardview_default_radius` (**2**) is unrelated.

Implementation: independent classes under `org.ayuless.settings`, reusing the
existing Telegram cells, switches and rounded section decoration. The decoration
gets **12 / 20** with matching positive predicates for visible and off-screen rows;
header factories count as spacers, so labels cannot join adjacent cards. The main
Settings list uses the same radius. Settings behavior and defaults are unchanged;
this restyle adds no feature flag or new functional setting.
