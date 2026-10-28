# Phase 02 — Locations Layer (Places + Nearby / My Locations)

## Context Links

- Plan: [plan.md](plan.md) · Prereq: [Phase 01](phase-01-setup-project-and-skeleton.md) DONE
- Brainstorm (categories, "residential" caveat): `../../reports/brainstorm-driver-demand-radar.md`
- Research (Nearby Search New, primaryType, pricing, VN gotchas): `research/researcher-places-compose-report.md` §1–3
- Pre-flight: GCP key exists + restricted (package + both SHA-1s) before this phase

## Overview

- **Date:** 2026-10-27 · **Priority:** P1 · **Status:** pending
- Replace `NoopPlaceSearcher` with `GooglePlaceSearcher` (Places API New, Nearby Search), wire real `LocationRepository`, build Nearby + My Locations screens with GPS + category filters; "residential" handled by text search / manual entry (not a primaryType).

## Key Insights

- `POST /v1/places:searchNearby`: `primaryType` filter, `maxResultCount ≤ 60`, `rankPreference: RANKING_PRECISE`, `locationBias` = GPS lat/lng + radius m.
- "residential" is **not** a primaryType → category chip routes to Places **Text Search** ("neighborhood"-style keyword near GPS) + manual geocode fallback, not Nearby Search.
- Match/dedupe by `placeId`, never by VN address strings (diacritics differ across calls).
- Pricing: 100–500 searches/mo ≈ $0–16 + $200 credit → effectively free; no client-side throttle needed for MVP but cap `maxResultCount=20` default to keep calls cheap.

## Requirements

**Functional**
- Nearby: GPS pin + 300m/1km/3km radius chips; category chips: restaurant / school / office / residential; result list (name, category icon, distance); "Save to My Locations" (upserts by placeId, dedupes)
- My Locations: list with pickup count + last-pickup recency (from Room); tap → Location Detail screen (history wired from Phase 03, stub now); manual "add location" (text search + geocode)
- Graceful no-key / no-GPS / empty-result / network-error states
- **Non-functional**: search response < 3s p95 on SG/VN mobile; result rows < 100ms recycled

## Architecture

```
NearbyScreen → NearbyViewModel → LocationRepository.searchNearby(category, radius, gps)
  → PlaceSearcher (interface) → GooglePlaceSearcher (OkHttp/retrofit client, Places New REST)
  → PlaceDto → LocationRepository.upsertFromPlaces (dedupe by placeId, category stored local)
MyLocationsScreen → MyLocationsViewModel → LocationRepository.recentByPickupActivity() (Room query)
```
Maps: MapView optional for Phase 02 — list-first UI is enough for MVP (brainstorm flow #1). Map view = stretch, not blocking.

## Related Code Files

Create (root = `/Users/february10/workspace/driver-demand-radar/`):
- `src/main/kotlin/com/february10/driverradar/data/places/GooglePlaceSearcher.kt` (implements Phase 01 interface)
- `src/main/kotlin/com/february10/driverradar/data/places/PlaceDtos.kt` (NearbySearchRequest/Response, TextSearchResponse; category→primaryType map, "residential"→text keyword route)
- `src/main/kotlin/com/february10/driverradar/data/places/LocationCategory.kt` (enum RESTAURANT/SCHOOL/OFFICE/RESIDENTIAL → api params)
- `src/main/kotlin/com/february10/driverradar/ui/screens/NearbyScreen.kt` (replace stub) + `NearbyViewModel.kt`
- `src/main/kotlin/com/february10/driverradar/ui/screens/MyLocationsScreen.kt` (replace stub) + `MyLocationsViewModel.kt`
- `src/main/kotlin/com/february10/driverradar/ui/screens/LocationDetailScreen.kt` (stub: shows location + "no pickups yet")
- `src/main/kotlin/com/february10/driverradar/ui/components/CategoryChips.kt`, `DistanceFormatter.kt`
- `src/main/kotlin/com/february10/driverradar/repo/LocationRepository.kt` (extend Phase 01 version with search+upsert)
- `src/main/kotlin/com/february10/driverradar/util/GpsProvider.kt` (FusedLocation-style one-shot via `androidx.location` or plain last-known + update)
- `src/main/AndroidManifest.xml` (modify: FINE_LOCATION + ACCESS_COARSE_LOCATION)

## Implementation Steps

1. Implement `GooglePlaceSearcher` against `places.googleapis.com/v1` (Nearby `:searchNearby`, Text `:searchText`); map category chips → `primaryType` or text keyword; 30s timeout.
2. `LocationCategory` enum with `apiPrimaryType` / `textKeyword` fields; RESIDENTIAL has `apiPrimaryType=null` (text route).
3. Extend `LocationRepository.upsertFromPlaces(places: List<PlaceDto>)` — dedupe by placeId (unique index), set `source=places`.
4. NearbyViewModel: on GPS → fetch per selected category+radius → emit list; states LOADING/ERROR/EMPTY/NO_GPS/NO_KEY.
5. MyLocationsViewModel: `recentByPickupActivity(limit=50)` Room query (LEFT JOIN pickups max(actual_pickup_time)); "add location" via text search + manual name/lat/lng form.
6. "Save to My Locations" row action → upsert + toast; My Locations reflects it on return.
7. GpsProvider: request FINE_LOCATION once; runtime permission flow placeholder (full flow in Phase 05); last-known fallback.
8. Unit tests: category→request mapping, upsert dedupe, DTO parsing fixtures.

## Todo

- [ ] GooglePlaceSearcher (Nearby + Text) with DTOs
- [ ] LocationCategory mapping (residential → text route)
- [ ] LocationRepository.upsertFromPlaces + dedupe test
- [ ] GpsProvider one-shot
- [ ] NearbyScreen + ViewModel (chips, radius, list, save)
- [ ] MyLocationsScreen + ViewModel (recency query, add-location form)
- [ ] LocationDetailScreen stub
- [ ] Manifest location perms; error/no-key/no-gps states
- [ ] Unit tests (mapping, dedupe, parse)

## Success Criteria

- With key + GPS: Nearby shows real VN POIs within radius; save persists + dedupes placeId across re-searches
- My Locations shows recency ordering; "add location" works with residential-type place
- App fully usable offline after first load (list persists)
- All unit tests green

## Risk Assessment

- **primaryType string wrong ("school" vs "university")** — verify against Google taxonomy docs at build time (research unresolved item); if search returns empty, fall back to text keyword
- **Vietnam rural data sparse** — expected; show "few results" state, don't pretend coverage
- **Cost creep** — default radius 1km + maxResultCount 20; per-device single user, free tier absorbs it
- **No-key launch crash** — guarded: settings-level `noKey` state, no exception path

## Security Considerations

- Key restricted to package + both SHA-1s (pre-flight); anon/secret keys never bundled
- No PII in places requests (GPS only, no address strings sent); aliases/notes remain device-local until Phase 04

## Next Steps

Phase 03 (Log Pickup) builds on My Locations + Location Detail stub.
