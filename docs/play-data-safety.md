# Play Console — Data Safety form answers

Fill the **Data safety** section (Policy → App content → Data safety) exactly as
below. It must match the [Android privacy policy](https://compo-cf.github.io/woodlands-eats/android-privacy.html)
and the app's actual behavior. Keep this file in sync whenever the app's data
handling changes (e.g. when AdMob, photos, or a location feature ship).

## Overview questions

| Question | Answer |
|---|---|
| Does your app collect or share any of the required user data types? | **Yes** |
| Is all of the user data collected by your app encrypted in transit? | **Yes** (Firestore + Firebase Auth + AdMob use HTTPS/TLS) |
| Do you provide a way for users to request that their data be deleted? | **Yes** — deletion URL: `https://compo-cf.github.io/woodlands-eats/android-account-deletion.html` |

## Data collected

Firestore/Firebase is our processor (not "sharing"). AdMob **is** a third party
for its own use, so the ad identifier below is declared as both collected AND
shared. Nothing is processed ephemerally.

### Personal info → Name
- Collected: **Yes** · Shared: **No**
- Optional (only if the user signs in and sets a display name)
- Purposes: **App functionality**, **Account management**
- Linked to the user's identity: **Yes**

### Personal info → User IDs
- Collected: **Yes** · Shared: **No**
- Required (the Firebase Authentication user identifier, created when you sign in to use ranking features)
- Purposes: **App functionality**, **Account management**
- Linked to the user's identity: **Yes**

### App activity → Other user-generated content
- Collected: **Yes** · Shared: **No**
- Covers: tier placements, dietary/needs tag confirmations, visited list, "permanently closed" reports
- Optional (only created when a signed-in user contributes)
- Purposes: **App functionality**
- Linked to the user's identity: **Yes**

### Device or other IDs (AdMob banner)
- Collected: **Yes** · Shared: **Yes** (Google AdMob receives it)
- Required (the banner shows to all users; ads are non-personalized, npa=1, but AdMob still uses a device/ad identifier for frequency capping + fraud prevention)
- Purposes: **Advertising or marketing**, **Fraud prevention, security, and compliance**
- Linked to the user's identity: **No** (not joined to the account/Firestore data)
- Follow Google's published AdMob Data Safety guidance if the Console offers a prefilled list.

## Data NOT collected (do not declare)
- **Location** — not collected this release (no "near me"; map centers on region; location permission removed from the manifest). AdMob may infer coarse region from IP, but the app does not collect device location.
- **Photos/videos** — Android release has no photo upload/viewing yet.
- **Financial info, Contacts, Messages, Calendar, Files, Health, Web history** — none.
- (The Firebase UID is declared under **User IDs** above, not under Device IDs.)

## ⚠️ Update triggers
- **Photo viewing/upload added** → declare Photos and add in-app report/block (UGC).
- **"Near me"/location added** → re-add location permission + declare Location.
