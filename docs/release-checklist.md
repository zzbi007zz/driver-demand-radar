# Release Checklist (MVP freeze)

Local-only build; the keystore stays **outside git** and is referenced through
environment variables. Do NOT commit keystores or passwords.

## 1. Create the release keystore (manual, one-time)

```bash
keytool -genkey -v -keystore ~/driverradar-release.keystore \
  -alias driverradar -keyalg RSA -keysize 2048 -validity 10000
```

Keep the keystore + password somewhere durable (the app can never recover
self-signed keys). Record them in a password manager, not in this repo.

## 2. Sign the release build

```bash
export ANDROID_HOME=$HOME/Library/Android/sdk
export ANDROID_RELEASE_KEYSTORE=$HOME/driverradar-release.keystore
export ANDROID_RELEASE_KEYSTORE_PASS='<storepass>'
export ANDROID_RELEASE_ALIAS=driverradar
export ANDROID_RELEASE_ALIAS_PASS='<keypass>'

./gradlew :assembleRelease
```

`build.gradle.kts` wires the `release` signing config from these env vars;
without `ANDROID_RELEASE_KEYSTORE` set, `assembleRelease` fails fast (no
unsigned release APK).

## 3. Apply both SHA-1 fingerprints to the GCP key restriction

```bash
./gradlew printSigningSha1
```

Prints `[signing:debug]` and `[signing:release]` lines. In Cloud Console →
APIs & Services → Credentials, add **both** the debug and the release SHA-1
to the Maps/Places key's Android-app restriction (package
`com.february10.driverradar`). The release key MUST carry both, or Maps
silently stops rendering on the release build (the classic "works in Studio,
dead in prod" failure).

## 4. Verify Maps loads on the release build

- `adb install app/release/*.apk` on a device with INTERNET.
- Open Nearby, allow location, run a search → Places results must render.
- Confirm no `API_KEY_INVALID` / `FAILED_PRECONDITION` in logcat.

## 5. 1-week dogfood note

Before v1.0.0: one week of real use. Confirm:

- Heat strip appears once a location crosses the gate (default 15).
- Backdated pickups land in their actual-time slot ("logged later" badge).
- JSON export re-parses on a clean install (Settings → Export backup → share
  → reopen → row counts match).
- Cold launch < 3s on the dogfood device.

Then tag v1.0.0. Cloud sync remains a future additive option (schema has no
sync coupling).
