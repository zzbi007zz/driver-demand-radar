---
title: "Driver Demand Radar MVP"
description: "Local-only Android app (Kotlin/Compose, Room) for a gig driver to log pickups per location and see 7x24 demand heat; JSON export as backup."
status: pending
priority: P2
effort: 21h
branch: none (not a git repo yet)
tags: [android, compose, room, places, local-only, mvp]
created: 2026-10-27
---

# Driver Demand Radar MVP

Locked design: `../reports/brainstorm-driver-demand-radar.md`.
Research: `research/researcher-places-compose-report.md`.

**Scope change (2026-10-27, user decision):** cloud sync dropped — app is **local-only**.
Backup/migration = user-initiated JSON export. Heat view kept (works 100% offline).
Supabase phase + research report removed; schema simplified (no LWW/tombstone fields).
Adding cloud later stays a clean additive option — nothing here couples to it.

## Pre-flight (before Phase 02) — manual, user-executed

- [ ] Google Cloud: enable Maps SDK for Android + Places API (New); create API key restricted to package `com.february10.driverradar` + SHA-1 of **debug AND release** keystores (create release keystore first)

## Phases

| # | Phase | Est. | Status | Depends on |
|---|-------|------|--------|------------|
| 01 | [Setup: gradle, AppContainer, Room schema]([phase-01-setup-project-and-skeleton.md](phase-01-setup-project-and-skeleton.md)) | 4h | done (2026-10-28, commit 0791891 + ee0a83d) | release keystore |
| 02 | [Locations layer: Places + Nearby/My Locations]([phase-02-locations-layer.md](phase-02-locations-layer.md)) | 6h | done (2026-10-28, commit 677ea36) | 01, GCP key |
| 03 | [Log Pickup sheet + Location detail history]([phase-03-pickup-log.md](phase-03-pickup-log.md)) | 5h | done (2026-10-28, commit 4104782) | 02 |
| 04 | [7x24 heat + JSON export + GPS/release polish]([phase-04-heat-export-polish.md](phase-04-heat-export-polish.md)) | 6h | done (2026-10-28, commit 20f9356) | 03 |

**Total: 21h.** Strictly sequential (each builds the previous screens/repositories).

## Data model (locked)

- `Location(id autoinc, source: places|manual, place_id?, name, category, lat, lng, created_at)` — place_id unique index (local dedupe)
- `Pickup(id autoinc, location_id FK, customer_alias, note?, logged_at, actual_pickup_time)`
- 7×24 HeatMatrix computed locally (single SQL query, trailing 8 weeks); min-count gate default **15**; "insufficient data" states, never fake confidence
- No PII: aliases + notes only; nothing leaves the phone except deliberate JSON export

## Test matrix

- Unit: heat bucketing + gate boundary, alias autocomplete filter, export JSON round-trip, DAO dedupe/undo-delete
- Manual E2E: log pickup <10s one-handed; backdated entry lands in correct heat bucket; export re-parses on clean install; Maps loads on release build

## Unresolved

- Confirm "school"/"university" `primaryType` strings in Google taxonomy at build time
- Verify 2026 Places SKU rates in Cloud Console before relying on $0 estimate
- DataStore vs SharedPreferences for settings (min-count gate, last-exported) — trivial pick at Phase 04

## Validation Summary

**Validated:** 2026-10-27 · **Questions asked:** 4 (+ scope change after)

### Confirmed Decisions
- Heat window: trailing 8 weeks — keep
- Package name locked: `com.february10.driverradar`
- **Scope: local-only** — cloud sync/Supabase dropped (YAGNI for a single-phone, low-usage app); JSON export is the backup/migration path
- Heat view retained (core "anticipate demand" feature, fully offline)
