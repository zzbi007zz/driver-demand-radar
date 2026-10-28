# Phase 01 — Setup Project and Skeleton

## Context Links

- Plan: [plan.md](plan.md)
- Brainstorm (locked decisions): `../../reports/brainstorm-driver-demand-radar.md`
- Research (Compose/Room/DI verdict): `research/researcher-places-compose-report.md` §4
- Scope note: local-only app (no cloud sync); schema stays simple, no LWW fields

## Overview

- **Date:** 2026-10-27 · **Priority:** P1 (blocker for all) · **Status:** pending
- Scaffold single-module Kotlin + Compose app `com.february10.driverradar`, hand-rolled `AppContainer` DI (no Hilt), Room DB with plain schema, placeholder `PlaceSearcher` interface so Phase 02 slots in without refactor.

## Key Insights

- KISS verdict from research: ~10 screens, 1 DB, 1 API client → `AppContainer` singleton created in `Application`. No KSP, no Hilt.
- **No sync fields in schema** (scope decision: local-only, dropped 2026-10-27). Autoincrement integer PKs, plain delete — no LWW/tombstones.
- Google Maps API key in `gradle.properties` (gitignored); injected via `resValue` into manifest `com.google.android.geo.API_KEY`.
- Release keystore lives outside git (env `ANDROID_RELEASE_KEYSTORE`); gradle task prints both SHA-1s for GCP key restriction.

## Requirements

**Functional**
- Empty nav shell: 3 tabs (Nearby / My Locations / Settings placeholder) + FAB for later Log Pickup sheet
- Room `locations` + `pickups` tables (plain columns), DAOs compile
- `PlaceSearcher` interface + `NoopPlaceSearcher` (returns empty list)
- Settings shows app version + last export timestamp
- Gradle task `printSigningSha1` outputs debug + release SHA-1

**Non-functional**
- minSdk 26, target/compile 35; Compose BOM latest stable; Kotlin 2.x
- Build passes `./gradlew :assembleDebug`; app launches to shell without crash

## Architecture

```
Application → AppContainer { RoomDb, LocationRepository, PickupRepository, NoopPlaceSearcher }
MainActivity → NavHost { Nearby, MyLocations, Settings } (stub screens)
Repository → Room DAO + PlaceSearcher interface
```
Screen state: `ViewModel` exposing `StateFlow<UiState>` → Compose `collectAsStateWithLifecycle`.

## Related Code Files

Create (root = `/Users/february10/workspace/driver-demand-radar/`):
- `build.gradle.kts`, `settings.gradle.kts`, `gradle.properties` (template, key gitignored), `gradle/wrapper/`, `.gitignore`
- `src/main/AndroidManifest.xml` (API_KEY meta-data, INTERNET + location placeholders)
- `src/main/kotlin/com/february10/driverradar/DriverDemandRadarApplication.kt`
- `src/main/kotlin/com/february10/driverradar/di/AppContainer.kt`
- `src/main/kotlin/com/february10/driverradar/data/local/DemandRadarDb.kt`
- `src/main/kotlin/com/february10/driverradar/data/local/LocationEntity.kt` — `id INTEGER PK autoinc, source, placeId?, name, category, lat, lng, createdAt`
- `src/main/kotlin/com/february10/driverradar/data/local/PickupEntity.kt` — `id INTEGER PK autoinc, locationId FK, customerAlias, note?, loggedAt, actualPickupTime`
- `src/main/kotlin/com/february10/driverradar/data/local/LocationDao.kt`, `PickupDao.kt`
- `src/main/kotlin/com/february10/driverradar/data/places/PlaceSearcher.kt` (interface + NoopPlaceSearcher)
- `src/main/kotlin/com/february10/driverradar/repo/LocationRepository.kt`, `PickupRepository.kt`
- `src/main/kotlin/com/february10/driverradar/ui/MainActivity.kt`
- `src/main/kotlin/com/february10/driverradar/ui/screens/NearbyScreen.kt`, `MyLocationsScreen.kt`, `SettingsScreen.kt` (stubs)
- `src/main/res/` (strings, theme, icons)

## Implementation Steps

1. Manual gradle scaffold: settings.gradle.kts, root build.gradle.kts, wrapper; Compose BOM, Room + KSP, androidx.location.
2. `gradle.properties` template: `GOOGLE_MAPS_API_KEY=` (gitignored) + manifest `resValue` injection.
3. `DriverDemandRadarApplication` wiring `AppContainer` (Room, repos, no-op PlaceSearcher).
4. Room entities/DAOs per schema above; location dedupe by `place_id` (unique index when non-null).
5. UI shell: `MainActivity` Scaffold + bottom nav + FAB; three stub screens.
6. `printSigningSha1` task: `keytool -list -v` over debug keystore + `ANDROID_RELEASE_KEYSTORE` env keystore.
7. Verify: `assembleDebug` + run; DAO smoke test in `test/`.

## Todo

- [ ] Gradle scaffold + wrapper + manifest + resValue key injection
- [ ] AppContainer + Application
- [ ] Room entities/DAOs/DB (plain schema, placeId dedupe)
- [ ] Repos (upsert/insert queries)
- [ ] Noop PlaceSearcher
- [ ] Nav shell + 3 stub screens + FAB
- [ ] printSigningSha1 task + run it, record both SHA-1s for GCP
- [ ] Unit test: DAO insert/upsert/dedupe

## Success Criteria

- `./gradlew :assembleDebug` green on clean checkout (no key → app launches, Nearby shows "no API key yet" state)
- SHA-1s of debug + release keystores printed → user can finish GCP key restriction
- DAO test passes: placeId upsert dedupes; pickup round-trips

## Risk Assessment

- **Keystore lost** — user stores release keystore outside git; task documents env var + backup location
- **Schema change later** — plain schema; adding a field = standard Room migration (no sync coupling)

## Security Considerations

- API key only in gitignored `gradle.properties`; never hardcoded
- No PII fields anywhere in schema (aliases/notes only — locked)
- Release keystore path via env var, never in repo

## Next Steps

Phase 02 (locations layer) unblocked once GCP key exists (pre-flight).
