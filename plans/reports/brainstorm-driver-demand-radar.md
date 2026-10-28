# Brainstorm Report — Driver Demand Radar

**Date:** 2026-10-27
**Status:** Approved design, report-only (no plan created yet)

## Problem Statement

Xanh SM gig driver (based VinFast Xanh SM area) wants to anticipate demand hotspots:
see popular nearby locations (restaurants/Shopee Food, schools, offices, residential)
plus their own pickup history per location — who (alias), when (time), notes — so they
can position near likely pickup demand.

## Locked Decisions

| Decision | Choice |
|---|---|
| Pickup data source | **Own manual logs only** (no gig-platform scraping) |
| Primary use case | Anticipate demand hotspots (time-pattern heat) |
| Stack | **Kotlin + Jetpack Compose** |
| Categories | Restaurants & food, Schools & campuses, Offices & residential |
| Maps/Places | **Google Maps SDK + Places API** |
| Sync | **Cloud sync from day 1 — Supabase** (Postgres + RLS + Android SDK) |
| Customer data | **Aliases + notes only** (no PII: no names/phones/addresses) |

## Data Model

```
Location(id, source: places|manual, placeId?, name, category, lat, lng)
Pickup(id, locationId, customerAlias, note, loggedAt, actualPickupTime, syncState, version)
HeatMatrix (computed: hour × weekday counts per location, min-count gate)
```

- `actualPickupTime` separate from `loggedAt` — driver often logs after the fact.
- Sync: bidirectional, row-level `version`, last-writer-wins by device+timestamp,
  tombstones for deletes. WorkManager: periodic + on-demand. Works fully offline.

## Screen Flow (v1)

1. **Nearby** — Places search from GPS, filter by category → "save to my locations"
2. **My Locations** — list with pickup counts + last-pickup recency
3. **Log Pickup** — bottom sheet, one-handed: recent locations on top, alias
   autocomplete, time defaults now (backdatable), optional note. **Target: <10s to log**
4. **Location Detail** — history + 7×24 heat strip ("Fri 16–18h: 9 pickups")
5. **Settings** — sync account, JSON export backup

## Risks (honest)

- **Cold start:** heat needs ~15–20 pickups/location to be meaningful. UI must show
  "insufficient data" states, never fake confidence. First 2 weeks = logbook.
- **Logging fatigue:** if log screen isn't the fastest on the phone, app dies.
  80% of UX effort on the Log Pickup sheet.
- **Places API cost:** small bill; free tier covers personal use. Restrict key to
  package + SHA-1.
- **Supabase free tier** fine for single user.

## Evaluated Alternatives

| Approach | Verdict |
|---|---|
| OSM/OSMDroid instead of Google | Free, works on non-GP devices, but VN place data thinner + search UX plainer. Keep as swap option behind `PlaceSearcher` interface. |
| Export/scrape Xanh SM dashboard | Rejected: ToS risk, fragile, out of scope for v1. |
| Offline-only v1, sync later | Rejected by user: chose Supabase from day 1. |
| Firebase | Works, but Postgres fits tabular pickup data + aggregations better. |

## Next Steps (when user kicks off)

1. `/ck:plan` from this report → phase files in `plans/`
2. Set up Supabase project (tables + RLS) before Phase 2 (sync)
3. Google Cloud: enable Maps SDK + Places API, create restricted API key
4. Architecture: single Compose app, Room local-truth, repository layer,
   `PlaceSearcher` interface, WorkManager sync worker

## Unresolved Questions

- Supabase project name/region (pick when creating project; VN latency → ap-southeast-1)
- Auth method: email magic-link vs Google sign-in (default: email, driver phone-only)
- App name (working: "Driver Demand Radar")
- Min-count gate for heat display: default 15 pickups/location, confirm during planning
