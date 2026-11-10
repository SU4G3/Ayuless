# Credits

## Icons

Ayuless uses icons from the **Solar Icons Set**
(https://www.figma.com/community/file/1166831539721848736/solar-icons-set)
by **480 Design** and **R4IN80W**, licensed under
[CC BY 4.0](https://creativecommons.org/licenses/by/4.0/).
The icons were converted to Android vector drawables; colors may be adjusted.

Files: `TMessagesProj/src/main/res/drawable/ayuless_solar_*.xml`.

`ayuless_solar_eye.xml` is an eye adaptation drawn in the Solar Linear style
for the retained TTL media badge, with the same CC BY 4.0 attribution above.
`ayuless_solar_clock.xml` is a clock adaptation in the same Solar Linear style
for timed media, credited to 480 Design / R4IN80W under CC BY 4.0.

CC BY 4.0 is one-way compatible with GPLv3: the icons can be distributed as part of
Ayuless (GPL-3.0-or-later) as long as this attribution is kept.

## Inspiration

Thanks to the [AyuGram](https://github.com/AyuGram) team (Radolyn Labs). Ayuless follows AyuGram's feature set. Ayuless is not affiliated with AyuGram or Radolyn Labs. Name idea from exteraless.

The AyuGram-derived port was removed, and the features were re-implemented
independently on top of Telegram from the written specification
[CLEANROOM-SPEC.md](CLEANROOM-SPEC.md), without access to AyuGram sources.
The settings layout and Solar adaptations are also independent implementations.
Older Git history (before this change) still contains the earlier port.
The removal inventory is in [CLEANROOM-REMOVED.md](CLEANROOM-REMOVED.md).

## Upstream

Ayuless is built on the official Telegram for Android sources
([DrKLO/Telegram](https://github.com/DrKLO/Telegram)), GPL v2 or later.
See [LICENSE-AUDIT.md](LICENSE-AUDIT.md) for third-party code inside the upstream tree.

## Location map imagery and labels

The optional Map / Satellite / Hybrid menu is implemented independently in
`org.ayuless.maps`, using MapLibre and Telegram's existing location menu.
It is disabled by default in Ayuless > Customization; the selected type is stored
locally. Street maps continue to use the existing OpenFreeMap style and its
source attribution. No API keys or paid accounts are used.

* **Satellite:** EOX IT Services GmbH, **Sentinel-2 cloudless 2016**, licensed
  [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/). Only the 2016 vintage
  is used: newer vintages have different restrictions. Public WMTS template:
  `https://tiles.maps.eox.at/wmts/1.0.0/s2cloudless_3857/default/g/{z}/{y}/{x}.jpg`.
  Attribution shown on the map: “Sentinel-2 cloudless – https://s2maps.eu by EOX
  IT Services GmbH (Contains modified Copernicus Sentinel data 2016)”, with links
  to the licence and EOX::Maps. See the provider's
  [licensing FAQ](https://cloudless.eox.at/documentation/faq).
* **Hybrid labels and roads:** the public EOX::Maps **Bright overlay**,
  `https://tiles.maps.eox.at/wmts/1.0.0/overlay_bright_3857/default/g/{z}/{y}/{x}.png`.
  The provider [permits application use with attribution and a link back](https://maps.eox.at/).
  Data © [OpenStreetMap contributors](https://www.openstreetmap.org/copyright)
  ([ODbL](https://opendatacommons.org/licenses/odbl/1-0/)) and
  [Natural Earth](https://www.naturalearthdata.com/about/terms-of-use/) (public
  domain); rendering © EOX and MapServer. These credits and links are displayed
  on hybrid maps. The overlay service permission is EOX's published permission,
  not a claim that EOX's rendering is CC BY.

Both raster sources are requested only for the visible map, through the public
`tiles.maps.eox.at` service (not EOX's customer-specific endpoints). EOX provides
this service as-is, may rate-limit it, and gives no availability guarantee.
Tile rows precede columns in the URLs. Sources are capped at zoom 14 and
magnified locally at higher zooms; satellite detail remains limited to the old
2016 mosaic. Required attribution is visible over the map with clickable links,
and is also included in MapLibre source metadata.

## Approximate profile registration dates

`org.ayuless.profile.RegistrationEstimate` is original Ayuless code. Its rounded
reference table was authored for this project from general public knowledge of
Telegram's ID growth eras, without importing client code, user records, or
third-party registration-date datasets. It is a **heuristic**, not a collection
of verified (ID, registration date) observations. Public user-count milestones
cannot be equated with numeric IDs: deleted accounts, bots, allocation gaps and
changes in allocation all affect the sequence.

The approximate user-ID anchors are: 1 million → November 2013; 10 million →
February 2014; 100 million → March 2015; 300 million → December 2016;
500 million → January 2018; 1 billion → January 2020; 2 billion → October 2021;
5 billion → July 2022; 6 billion → May 2023; 7 billion → April 2024;
8 billion → April 2025. Dates between anchors are interpolated linearly in UTC
calendar days and displayed only as an approximate month and year (≈). The
error can be months or years. IDs outside the range show “Unknown”; there is no
extrapolation, remote lookup or claim of an exact registration date.

Chat IDs belong to a different allocation sequence and are never passed through
the user table. Chat profiles show the month of Telegram's `Chat.date` creation
timestamp (or “Unknown” when absent); migration may affect that timestamp.
Numeric IDs are the raw Telegram user/chat ID, not Bot API dialog IDs. Both rows
are copyable and controlled by “Show ID and registration date” (default on,
as explicitly requested for this preference).
