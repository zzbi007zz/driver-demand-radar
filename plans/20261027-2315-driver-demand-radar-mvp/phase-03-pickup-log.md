# Phase 03 — Log Pickup Sheet + Location Detail History

## Context Links

- Plan: [plan.md](plan.md) · Prereq: [Phase 02](phase-02-locations-layer.md) DONE
- Brainstorm (UX lock: log sheet <10s, one-handed, recent-first; `loggedAt` vs `actualPickupTime`): `../../reports/brainstorm-driver-demand-radar.md`
- 80% of UX effort lands here — fastest-logging sheet is the app's life-or-death screen.

## Overview

- **Date:** 2026-10-27 · **Priority:** P1 (retention-critical) · **Status:** pending
- Modal bottom sheet "Log Pickup" reachable in ≤2 touches from any screen; Location Detail gains history list + pickup add/edit/delete.

## Key Insights

- Target: **<10s open-to-saved**, one-handed, thumb-zone controls. Order of screen: location picker → alias → save. Time + note are optional fast-follow, prefilled.
- **Location picker = recent locations on top** (max `actual_pickup_time` from Phase 02 DAO), then "browse all", then "quick add (last used)". Never force typing a location.
- **Alias autocomplete**: query distinct `customer_alias` values (Room `SELECT DISTINCT`), filter as-you-type, tap-to-complete; free-text always allowed (new alias creates it).
- **Backdatable time**: default `actualPickupTime = now`; quick chips (±15min) + full picker, not a keyboard. `loggedAt` set implicitly at save.
- Save = one large bottom-pinned button; instant Room write + toast "Logged" + auto-dismiss. No confirmation step.
- Delete is a plain Room delete with a 5s undo snackbar (VM keeps last-deleted row, re-inserts on undo). Local-only scope — no tombstones.

## Requirements

**Functional**
- Log sheet: location (recent-first list, searchable), alias (autocomplete + free text), time (default now, backdatable, quick chips), optional note (1 line), save; measurable <10s common path
- Location Detail: history list (time, alias, note, "logged later" badge when `loggedAt − actualPickupTime > 1h`), edit + delete per row, FAB "log here" prefilling location
- Delete: undo snackbar 5s
- Empty/low states: "0 pickups here yet" (feeds Phase 04 heat gate)

**Non-functional**: sheet open <300ms; save <100ms local; list virtualized (LazyColumn); works fully offline

## Architecture

```
LogPickupSheet (modal, composed from MainActivity) → LogPickupViewModel
  → LocationRepository.recent(limit=10) / PickupRepository.insert()
LocationDetailScreen → LocationDetailViewModel
  → PickupRepository.historyFor(locationId) / update / delete (undo held in VM)
```
`PickupRepository.insert` sets `loggedAt=now` implicitly; plain Room write, no sync state.

## Related Code Files

Create (root = `/Users/february10/workspace/driver-demand-radar/`):
- `src/main/kotlin/com/february10/driverradar/ui/screens/LogPickupSheet.kt` + `LogPickupViewModel.kt`
- `src/main/kotlin/com/february10/driverradar/ui/screens/LocationDetailScreen.kt` (replace Phase 02 stub) + `LocationDetailViewModel.kt`
- `src/main/kotlin/com/february10/driverradar/ui/components/RecentLocationPicker.kt`, `AliasAutocomplete.kt`, `BackdatePicker.kt` (chips + scroll)
- `src/main/kotlin/com/february10/driverradar/repo/PickupRepository.kt` (extend: insert/update/delete/historyFor/aliases)
- `src/main/kotlin/com/february10/driverradar/data/local/PickupDao.kt` (extend: distinct-aliases query, historyByLocation, plain delete)

## Implementation Steps

1. `PickupDao`: `aliases()` (distinct, length-bounded), `historyByLocation(id)` desc by `actual_pickup_time`, `delete(id)`, `recentLocations` already in Phase 02.
2. `PickupRepository.insert(...)`: auto-inc id, `loggedAt=now`; update/delete are plain Room ops.
3. `LogPickupViewModel`: state = location list (recent 10), selected location, alias query+filter, time default now, note; save→insert→dismiss.
4. `LogPickupSheet`: ModalBottomSheet, thumb-zone layout: recent chips/list on top, alias field with dropdown, time row (now chip + "earlier" toggle), note, big save bar.
5. Hook: FAB on all screens + per-row "log here" on Location Detail prefills location + skips picker.
6. `LocationDetailScreen`: history rows, edit (sheet), delete + undo snackbar, pickup count, "logged later" badge.
7. Unit tests: alias filter, backdate default, delete-undo restore, "logged later" boundary (>1h).

## Todo

- [ ] PickupDao queries (aliases, history, delete)
- [ ] PickupRepository insert/update/delete with loggedAt bookkeeping
- [ ] LogPickupSheet + VM + RecentLocationPicker/AliasAutocomplete/BackdatePicker
- [ ] FAB wiring on all screens + "log here" prefill
- [ ] LocationDetail history + edit/delete + undo
- [ ] Unit tests (alias filter, backdate, delete-undo, logged-later)

## Success Criteria

- Timed manual test: cold sheet → save on a recent location ≤ 10s (common path, one-handed)
- New alias can be typed free; returns suggest history; delete shows undo; history orders by actual time
- Fully offline: log 10 pickups, close app, reopen, all present
- Unit tests green

## Risk Assessment

- **Logging fatigue kills app** (brainstorm risk #2) — mitigation: this phase IS the mitigation; keep sheet stateless-light, no forced steps
- **Backdate abuse breaks heat** — heat buckets by `actualPickupTime` (locked model), so backdating is a feature; "logged later" badge surfaces it for trust
- **Sheet latency** — recent list preloaded in VM init, not on open
- **Delete cascade confusion** — pickups deleted independently; location delete is out of scope (YAGNI)

## Security Considerations

- Alias/notes only, no PII fields introduced (locked)
- All data device-local; nothing leaves the phone (Phase 04 JSON export is user-initiated)

## Next Steps

Phase 04 (heat + export + polish) reads history built here.
