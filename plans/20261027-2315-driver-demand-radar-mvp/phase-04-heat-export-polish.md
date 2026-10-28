# Phase 04 — 7×24 Heat Strip + JSON Export + GPS / Release Polish

## Context Links

- Plan: [plan.md](plan.md) · Prereq: [Phase 03](phase-03-pickup-log.md) DONE
- Brainstorm (cold-start risk, min-count gate 15, insufficient-data states, JSON export, API key restriction): `../../reports/brainstorm-driver-demand-radar.md`
- Research (SHA-1 restriction checklist, pricing sanity): `research/researcher-places-compose-report.md` §2
- Scope: local-only — heat is a single SQL query over Room; JSON export replaces cloud backup

## Overview

- **Date:** 2026-10-27 · **Priority:** P2 · **Status:** pending
- 7×24 heat strip on Location Detail ("Fri 16–18h: 9 pickups"), honest insufficient-data states, JSON export/backup in Settings, real GPS permission flow, release keystore + GCP key restriction checklist to close MVP.

## Key Insights

- **Never fake confidence**: heat shows only when location has ≥ **15** pickups (default gate, editable in Settings); else "Not enough data yet — N of 15 logged" (brainstorm risk #1: first 2 weeks are a logbook).
- Heat buckets by **`actual_pickup_time`** (backdated-aware), weekday × 24h grid; window = trailing **8 weeks** (confirmed in validation). Top-3 slots as text rows + full 7×24 as a small Canvas grid (no chart lib — KISS).
- JSON export = full backup (locations + pickups + meta), user-initiated, share via `Intent.ACTION_SEND`. This is the only data-leaving-the-phone path and it's deliberate — no cloud, no background upload.
- GPS: full runtime permission flow (request, rationale, denied → last-known, permanently denied → settings deep link) — Phase 02 shipped a placeholder.
- Release polish: signed APK build path + GCP key carries **both** debug & release SHA-1s (Phase 01 task printed them); verify Maps loads on release build.

## Requirements

**Functional**
- Location Detail: 7×24 heat strip (top-3 text rows + full grid), "insufficient data (N/15)" state, gate editable in Settings
- Settings: min-count gate (5–50, default 15), JSON export (share intent) + last-exported timestamp; import out of scope (YAGNI)
- GPS permission flow per above; Nearby reuses it
- Release build: signed APK + `printSigningSha1` output applied to GCP key

**Non-functional**: heat compute < 20ms for 1k rows (single Room/SQL query, no in-memory loop over all pickups); export < 2s for 10k rows

## Architecture

```
LocationDetailViewModel → HeatRepository.matrixFor(locationId, weeks=8)
  → Room @Query: bucket actual_pickup_time into 168 (weekday×hour) cells + total count; gate applied client-side (total ≥ minCount)
ExportRepository.export() → JSON {locations[], pickups[], exportedAt} → Intent.ACTION_SEND
GpsProvider (Phase 02) ← full runtime permission flow (util/LocationPermission.kt)
```

## Related Code Files

Create (root = `/Users/february10/workspace/driver-demand-radar/`):
- `src/main/kotlin/com/february10/driverradar/repo/HeatRepository.kt` (7×24 SQL bucketing query + min-count gate check)
- `src/main/kotlin/com/february10/driverradar/ui/screens/LocationDetailScreen.kt` (modify: heat strip + insufficient state) + `LocationDetailViewModel.kt`
- `src/main/kotlin/com/february10/driverradar/ui/components/HeatStrip.kt` (Canvas-drawn 7×24 grid + top-3 rows, color scale)
- `src/main/kotlin/com/february10/driverradar/repo/ExportRepository.kt` + `ui/screens/SettingsScreen.kt` (modify: gate editor, export)
- `src/main/kotlin/com/february10/driverradar/util/LocationPermission.kt` (request/rationale/denied/permanent flows)
- `src/main/kotlin/com/february10/driverradar/model/HeatCell.kt`, `ExportModels.kt`

## Implementation Steps

1. `HeatRepository.matrixFor`: one Room `@Query` returning 168 `(weekday, hour, count)` rows for trailing 8 weeks + total count; gate = total ≥ settings.minCount (default 15, stored in DataStore/prefs).
2. `HeatStrip` composable: top-3 slots as text ("Fri 16–18h: 9"), full 7×24 Canvas grid (weekday rows × 24 cols, intensity = count/max), legend; tap a cell → filter list of matching pickups.
3. Insufficient-data state: progress toward gate ("12/15 logged — keep logging"), never a fake heat.
4. `ExportRepository`: serialize both tables (exclude sync internals — there are none), JSON pretty-print, save to cache dir, `Intent.createChooser(ACTION_SEND)`.
5. Settings: gate editor (5–50, default 15), export button + last-exported timestamp.
6. `LocationPermission`: full flow wired into NearbyScreen (replace Phase 02 placeholder); last-known fallback; denied/permanent states with settings deep link.
7. Release: build `release` variant signed via `ANDROID_RELEASE_KEYSTORE` env keystore; run `printSigningSha1`; confirm both SHA-1s in GCP key restrictions; smoke-test Maps load on release.
8. Unit tests: heat bucketing fixtures (backdated entries land in correct bucket), gate boundary (14 vs 15), export JSON round-trip parse.

## Todo

- [ ] HeatRepository 7×24 SQL + gate
- [ ] HeatStrip composable + top-3 rows + cell tap
- [ ] Insufficient-data state on Location Detail
- [ ] Settings: gate editor + JSON export
- [ ] Full GPS permission flow
- [ ] Signed release build + GCP SHA-1 checklist verified
- [ ] Unit tests (bucketing, gate boundary, export round-trip)

## Success Criteria

- Location with ≥15 pickups shows heat strip; <15 shows progress state; 14→15 flip visible
- Backdated pickup appears in its `actualPickupTime` bucket, not `loggedAt`
- Export file re-parses (round-trip test) and opens on a new phone after reinstall (manual)
- Release APK installs, Maps render (key carries release SHA-1)
- All tests green; app cold launch < 3s

## Risk Assessment

- **Heat looks wrong to user (backdating)** — mitigated: always bucket by actual time + "logged later" badge (Phase 03)
- **8-week window washes out long-term patterns** — acceptable for MVP; window is a constant, cheap to change
- **Release key missing from GCP restriction → Maps silently breaks on release build** — checklist step 7 explicit, verified on-device
- **Data loss on reinstall** — mitigated: JSON export is the documented backup; Settings copy reminds user to export before uninstall

## Security Considerations

- Export contains only aliases/notes/locations — no PII by design (locked); Settings copy warns export includes notes
- No data leaves the phone except deliberate user share-intent
- GCP key restricted to package + both SHA-1s — the only place secrets are enforced; re-verify after every keystore change
- Release keystore stays outside git (env `ANDROID_RELEASE_KEYSTORE`); document backup location in Settings/help copy

## Next Steps

MVP freeze: 1-week personal dogfood (brainstorm next-steps), confirm Places primaryType strings (unresolved item), then tag v1.0.0. Cloud sync remains a future option — schema has no sync coupling, so adding Supabase later is a clean, additive phase.
