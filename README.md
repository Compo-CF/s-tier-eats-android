# S-Tier Eats — Android

Native Android companion to the iOS [S-Tier Eats](https://apps.apple.com/app/id6773501518)
restaurant tier-list app. Kotlin + Jetpack Compose, reading and writing the
**shared Firestore backend** (`fir-tier-eats`) that iOS mirrors every write to,
so rankings and community consensus are one dataset across both platforms.

## Architecture

- **UI**: Jetpack Compose + Material 3, single-Activity + NavHost (tabs: Map /
  Browse / My Tiers / Community / Profile).
- **Data**: `data/FirestoreRepository.kt` — the Android twin of iOS
  `FirebaseService`. Same collections, doc-id conventions, and field names
  (see `docs/firestore-schema.md` in the `woodlands-eats` repo — the source of
  truth for the wire contract).
- **Identity**: Google Sign-In → Firebase Auth (`auth/AuthManager.kt`). The
  Firebase UID is the user's Firestore key — stable across reinstalls, the
  Android analogue of iOS's iCloud identity. Identities are per-platform (an
  Apple user and an Android user are distinct keys); this was an accepted
  tradeoff since cross-platform device switching is rare.
- **Catalog**: bundled restaurant seed (parity with iOS `Restaurants.json`) +
  community additions from the `liveRestaurants` collection.

## Parity notes / TODO

- `Tier.fromAverage` must match iOS `Tier.from(averageScore:)` — verify.
- Board reads scan the whole `placements` collection (same cost caveat as iOS);
  a maintained consensus doc is the eventual optimization.
- Dish-photo upload/view needs Firebase Storage (deferred on iOS too).
- Screens are being built incrementally on top of the data + auth foundation.

## Build

CI (`.github/workflows/android.yml`) assembles a debug APK on every push —
avoids local Gradle setup. Locally: open in Android Studio (generates the
Gradle wrapper), or run `gradle wrapper` then `./gradlew assembleDebug`.

### Setup requirements

1. `app/google-services.json` from the Firebase console (present).
2. **Google Sign-In enabled** in Firebase Auth → re-download
   `google-services.json` so it contains the OAuth web client (generates
   `R.string.default_web_client_id`, required by `AuthManager`).
3. **SHA-1** of the signing key added to the Android app in Firebase (needed
   for Google Sign-In). Debug key for local testing; CI release key for
   distribution.
4. `MAPS_API_KEY` CI secret (and local `MAPS_API_KEY` Gradle property) for the
   Maps SDK.
