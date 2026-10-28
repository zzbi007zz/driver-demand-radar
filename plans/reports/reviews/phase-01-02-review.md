# Code Review — Phases 01–02 (driver-demand-radar)

**Reviewer:** main agent (inline, post-2 subagent truncations) · **Date:** 2026-10-28
**Scope:** data layer, Places client, GpsProvider, repos, UI shell, DAO tests. Phase 03 in-flight files excluded.

## Verdict

**SAFE to build Phase 03 on top.** No blockers. 5 minor notes below.

Live-compile + test snapshot at review time: `:assembleDebug` BUILD SUCCESSFUL, `test` 6/6 pass (both variants).

## Findings (minor, non-blocking)

1. **`LocationRepository.upsert` race (minor)** — two concurrent `upsert`s of a new placeId can both miss `findByPlaceId` → double insert → the unique `idx_locations_place_id` triggers `OnConflictStrategy.REPLACE`, which deletes the first row and keeps the second. UI is single-user/sequential, so probability is ~zero in practice; note for a future multi-device sync design. No action now.
2. **`GpsProvider` last-known-only (by design)** — no fresh-fix path; if the user hasn't opened a map app recently, `lastLocation` can be stale by minutes. Acceptable for POI search (300m–3km radius). KISS-acceptable.
3. **`NearbyViewModel.distanceMeters` math helpers (minor)** — companion-object `sin/cos/asin/sqrt` wrappers are noisy; `kotlin.math.*` direct use is cleaner. Cosmetic only; leave unless touched.
4. **`PlayServices` `lastLocation` property syntax (verified OK)** — Kotlin synthetic property over `getLastLocation()` via `kotlinx-coroutines-play-services` `await()`; confirmed compiling on `play-services-location:19.0.0` whose client is constructor-based (no `getInstance` static — verified via javap on the cached AAR).
5. **Test depth (minor)** — 6 DAO tests cover dedupe / distinct-placeId / round-trip / history ordering / FK rejection. No `recentWithPickupStats` test yet (the aggregate query is only exercised at runtime). Suggested Phase 03 or 04: add one Robolectric test — insert locations + 2 pickups, assert counts/order.

## Verified-correct (the "risky" items checked)

- **Places API (New) field mapping** — `includedPrimaryTypes` (list form, correct for `searchNearby`), `rankPreference: RANKING_PRECISE`, `locationBias: {lat,lng}`, response `displayName.text` / `id` / `location.{latitude,longitude}` / `primaryType`; field mask `places.displayName,places.id,places.location,places.primaryType`. Text Search (New) uses `textQuery` + same bias. All consistent with current API shape. Open risk remains: exact `primaryType` string values for school/office confirmed as `restaurant`/`school` in code; "office" validity + residential keyword quality should be smoke-tested against the live API in the pre-flight step.
- **`recentWithPickupStats` SQL** — flat column projection (`l.*` + 2 correlated subqueries) maps positionally into the flat `LocationWithStats` data class; KSP verified at build time. O(N²) subquery shape is irrelevant at MVP scale (hundreds of rows).
- **`LocationWithStats` flat mapping** — confirmed flat (not embedded-entity) after KSP rejection of the embedded form; VM folds it back into `LocationEntity` manually. Correct.
- **RLS/N/A** — local-only scope, no cloud. N/A.
- **Lifecycle** — `stateIn(viewModelScope, SharingStarted.Lazily)` on Room flows is the standard safe pattern; no leaked coroutines.

## Action items

- [ ] Add one `recentWithPickupStats` DAO test (Phase 04 batch) — finding 5
- [ ] Smoke-test live Places API (restaurant + residential keyword) in pre-flight — finding under "verified-correct"
- No Phase 03 blockers.
