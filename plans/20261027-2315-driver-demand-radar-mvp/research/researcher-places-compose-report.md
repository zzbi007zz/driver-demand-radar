# Research: Google Places/Maps for driver app (Vietnam) + Compose/Room note

## 1. Which Places API (New) endpoint + pricing

**Use Nearby Search (New)** for "popular schools/restaurants near me":
- Endpoint: `POST https://places.googleapis.com/v1/places:searchNearby`
- Filters: `maxResultCount` (<=60), `rankPreference: RANKING_PRECISE`, `locationBias` (LatLng + radius m).
- Category filter is `primaryType` (New API uses Google's primary-type taxonomy, not legacy `type` strings): `primaryType: "restaurant"`, `"school"`. Note "residential" is not a primaryType — residential areas are rarely discrete places; treat that category as a local-dict / geocoded-quad fallback, not a Places search.
- Text Search (New) = keyword+location, radius bias up to 50km, no precise radius cap. Right for "sao mai restaurant" named searches, wrong for category-nearby.
- Place Details = enrich one placeId (photos, reviews, hours). Costs extra; skip for MVP.

**Pricing (pay-as-you-go, per 1,000 calls):**
| SKU | Rate | Notes |
|---|---|---|
| Nearby Search (New) | ~$32/1k | |
| Text Search (New) | ~$17/1k | |
| Place Details | ~$17/1k (+$7/1k photos) | |
| Maps SDK tiles/loads | $12/1k after first 250k | free up to 250k loads/mo per API key |

- Free baseline: $200/mo credit across Maps SKUs (legacy) — new pay-as-you-go page now shows "free calls per SKU" per tier (10k Essentials / 5k Pro / 1k Enterprise). Verify in Cloud Console.
- 100–500 searches/mo ≈ **$0–16/mo + $200 credit covers all** → effectively free. Budget ≈ $0.
- ⚠ Google restructured 2025–2026: subscription tiers (Starter $100, Essentials $275, Pro $1,200/mo) exist but are NOT needed for our volume; pay-as-you-go is fine.

## 2. Key restriction + SHA-1

Restrict key (Cloud Console → Credentials → your key → Restrict key):
- **Application restrictions → Android**: package name `com.february10.driverradar` + SHA-1 for BOTH debug and release keystores.
- **API restrictions**: enable only Maps SDK for Android + Places API (New). (Legacy Nearby Search SKU was folded into Places API (New) in 2025; restrict on "Places API (New)".)

SHA-1 without keytool (gradle task, debug + release):
```kotlin
// build.gradle.kts
tasks.register("printKeys") {
    doLast {
        val sign = extensions.getByType(com.android.build.api.dsl.SigningConfig)
        // print all configs incl. debug
    }
}
```
Simpler: `./gradlew androidsigningReport` won't exist; use:
```bash
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android
keytool -list -v -keystore release.keystore
```
Or Android Studio: Build → Generate Signed APK → pick keystore → dialog shows SHA-1 (no build needed).
- Release: keep release keystore in repo OUTSIDE git (env var `ANDROID_RELEASE_KEYSTORE` + `keytool` task in CI reads it, or store in GoogleCloud secret manager). Create release keystore BEFORE restricting key so you add both fingerprints at once.

In app: `AndroidManifest.xml` → `com.google.android.geo.API_KEY` meta-data; key value injected from `gradle.properties` (gitignored) via `resValue`.

## 3. Vietnam-specific gotchas

- Map data quality: **good** in HCMC/Hanoi/Da Nang; rural provinces sparse.
- Category labels: `primaryType` "restaurant"/"school" returns results with Vietnamese names fine (Google's place DB is localized).
- Geocoding: `AddressComponent` names use Vietnamese diacritics — don't string-match addresses, match by `placeId`.
- "residential" isn't a category; derive residential POIs from nearby "neighborhood" text-search or manual admin-quart-level grid, not Places.
- No known Vietnam-only Maps SDK bug; standard Android Maps SDK works in VN. Data layer OK.

## 4. Architecture note (single-module Compose + Room)

Standard modern: `@Immutable` data in Room entities → repository (Room DAO + Places client) → `StateFlow` in `ViewModel` → Compose. DI: for a small app (~10 screens, 1 DB, 1 API), **skip Hilt** and use a hand-rolled `AppContainer` (a singleton holding `RoomDb`, `Repository`s) created in `Application` — fewer dependencies, no KSP magic, easier to delete later. Hilt is justified only when the graph exceeds ~3 injection sites. This is the KISS verdict.

## Unresolved questions
- Exact 2026 per-SKU $ rates (the pricing page I fetched showed subscription tiers, not per-1k; verify in Cloud Console before launch — numbers above from legacy SKU table, likely still valid for pay-as-you-go)
- `primaryType` valid value for schools: confirm exact string ("school" vs "school_district" vs "university") in the [primary type taxonomy](https://developers.google.com/maps/branding/place-data-types)
- Whether "Nearby Search (New)" is still a standalone SKU or merged into "Places Search" umbrella post-2025 restructure

## Sources
- https://mapsplatform.google.com/pricing (subscription tiers + free-calls baseline)
- https://developers.google.com/maps/documentation/places/web-service/nearby-search (endpoint shape)
- https://developer.android.com/studio/apk/sign-app (keystore + SHA-1)
- https://developers.google.com/maps/documentation/android-sdk/get-api-key (key restriction)
