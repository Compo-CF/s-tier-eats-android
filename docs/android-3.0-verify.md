# Android 3.0-parity — Pixel 7a verification checklist

Six batches landed on `main`, all CI-green but **none run on a device yet**. CI
proves they compile, not that they behave. Install the latest CI `app-release-aab`
and walk these.

## Install
- Latest CI run → **Artifacts → `app-release-aab`** → download → install on the Pixel
  (`adb install -r app-release.aab` won't work for an AAB; use bundletool, or upload
  to the **Internal testing** track and install from Play — required anyway for the
  subscription, see below).

## A. Share cards (batch 1 + Wrapped)
- **My Tiers → Share** → preview sheet → "Share image" → a tier card PNG reaches the
  share sheet.
- **My Stats → Share icon** (top bar, only when you've ranked ≥1) → Food stats card.

## B. Friends / social (batch: follow graph + Taste Match)
- **Follow loop** — easiest test is the custom-scheme deep link:
  ```
  adb shell am start -a android.intent.action.VIEW -d "stier://tier/<a-real-firebase-uid>"
  ```
  (use a second account's uid, or your iOS account's — any uid with placements in
  Firestore). It should open that user's tier screen → **Follow**.
- **Community → Friends board** should now populate (was empty before).
- **Profile → Friends & Taste Match** → the hub; **trophy** → leaderboard; tap a
  matched friend → 1:1 Taste Match %.
- **Profile → Share my tier list** → shares your `…/tier/<uid>` link.

## C. Shareable Lists
- **My Tiers → My Lists** → create "Top 5 Tacos" → open → **+** add a few → remove one
  → rename → toggle **Public** → **Share icon** (image) + **Link icon** (public link).
- List deep link:
  ```
  adb shell am start -a android.intent.action.VIEW -d "stier://list/<a-list-id>"
  ```
  opens it read-only.
- **Watch for:** saves actually persist (reopen the list) — this exercises the
  `lists` Firestore rules deployed on iOS.

## D. Visual polish
- Open **Community** (or the leaderboard) cold — you should see **pulsing skeleton
  rows** instead of a spinner while it loads.

## E. S-Tier Plus subscription (needs setup first)
Play Billing **cannot be fully tested on a sideloaded build**. To verify a real purchase:
1. **Play Console:** create an auto-renewing subscription **`stier_plus_monthly`**
   with a base plan + price, and add a **Terms/EULA link** to the listing.
2. Upload this build to **Internal testing** (bump `versionCode` → 13 first).
3. Add your account as a **license tester** (Play Console → Setup → License testing).
4. Install from the Internal-testing link, open **Profile → S-Tier Plus** → Subscribe
   (test card) → confirm **ads disappear on Map + Browse** and the card flips to
   "Plus member".
- **Without** the product created, the button correctly reads "Currently unavailable"
  and ads still show (the `isPlus = false` path works on any build).

## If something's off
Note which screen + what you saw; it's almost certainly a quick fix + re-push → CI.
The risky ones to scrutinize: the **list save/deep-link** flow and the **billing**
purchase→acknowledge→ads-off loop.
