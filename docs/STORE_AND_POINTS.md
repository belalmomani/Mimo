# Mimo Store & Points — developer guide

## Where things are
| Concern | File |
|---|---|
| Points ledger (only code that changes the balance) | `domain/PointsManager.kt` |
| Unlock by points, ownership, active look | `domain/SkinUnlockManager.kt` |
| Rewarded-ad rewards, daily limit, interval, no double pay | `domain/RewardedAdsManager.kt` |
| **All store prices / ads per look** | `domain/SkinCatalog.kt` |
| **Points per ad, daily limit, interval, welcome bonus, on/off** | `domain/RewardConfig.kt` |
| Point packs (Starter/Plus/Premium/Ultimate) | `domain/RewardConfig.kt` → `PointsPacks` |
| Room storage (atomic transactions) | `data/db/*` |
| Start.io adapter | `ads/StartIoRewardedProvider.kt` |
| Billing (placeholder, not connected) | `billing/BillingRepository.kt` |

## Change a look's price / number of ads
Edit its row in `SkinCatalog.ALL`: `AD_3, ads = 3` → `AD_5, ads = 5`; `POINTS, points = 60` → any price.
`UnlockType` and the numbers must agree (`AD_n` ⇒ `ads = n`, `POINTS` ⇒ `points > 0`, `MIXED` ⇒ both) — a wrong row fails at app start with a clear message.

## Add a new look
1. Put the transparent PNG in `app/src/main/res/drawable-nodpi/face_21.png`.
2. Add `"face_21" to R.drawable.face_21` in `face/SkinArt.kt`.
3. Add a row to `SkinCatalog.ALL`.
`tools/slice_sheet.py <sheet.png> <outdir>` re-cuts a 5×4 sheet and writes `face_meta.json` (eye line / split for blinking).

## Change points per ad
`RewardConfig(pointsPerAd = 10)` (instantiated in `Graph.kt`). Same place: `dailyPointAdLimit`, `minSecondsBetweenPointAds`, `rewardedAdsEnabled`, `welcomeBonusPoints`.
Ads watched *for a specific look* give progress (1/3, 2/3, 3/3), not points, and are not subject to the point-ad daily limit.

## Start.io
* Dependency: `com.startapp:inapp-sdk:5.3.1` from Maven Central (version taken from the vendor's integration page).
* App id: `STARTIO_APP_ID` in `gradle.properties` (or `-PSTARTIO_APP_ID=...`); `ADS_ENABLED=false` builds without running any ad code.
* Initialised in code (`StartAppSDK.init(context, id, false)`); the auto-init provider is removed as the vendor guide requires; `android.permission.AD_ID` is declared.
* Rewarded flow follows the vendor sample: `StartAppAd` + `AdMode.REWARDED_VIDEO` + `VideoListener.onVideoCompleted`.
* **Not yet compiled or run against the real SDK** (see "Verification status"). Test with test ads/your device before publishing.
* Certificate fingerprints / Google Play ads policies / consent (UMP/GDPR) must be handled in the Start.io dashboard and in your Play listing.

## Google Play Billing (to do)
Nothing is faked. To enable real purchases: create consumable products in Play Console, add `com.android.billingclient:billing` (8.x+),
implement `BillingRepository`, credit points with `PointsManager.credit(..., txId = PointsManager.purchaseTxId(token))`,
verify purchases server-side, handle pending/cancelled/refunded purchases and restore. Local points bought with money should be validated on a server.

## Anti-tampering — honest limits
Balance and ownership are stored locally (Room). The ledger prevents double payment, negative balance and half-finished charges, but a
rooted/modified device can edit its own database, and `onVideoCompleted` is a client-side signal. Do not treat local points as secure
currency; for real-money points use server-side verification.

## Verification status
* `tools/run_domain_tests.sh` compiles the pure-Kotlin domain layer and runs 30 JUnit tests (works offline).
* The Android layer (Room, Compose UI, overlay service, Start.io adapter) has **not been compiled** in the authoring environment
  (no Android SDK / Maven access). CI (`.github/workflows/build.yml`) builds it; expect to fix small compile errors on the first run.
* Voice & dialogue (added later): `domain/dialogue/*` (pure logic, 15 tests: quiet hours, modes, rate limits, no repeats, calls/focus),
  `speech/SpeechController.kt` (Android TextToSpeech; reports a missing voice instead of failing), `ui/SettingsScreen.kt`.
  The talking mouth is an APPROXIMATE pulse while the voice is active, not lip-sync. Call detection uses AudioManager mode only (best effort).
* Habits & reminders (added later): `domain/reminders/*` (pure logic, 17 tests: daily/weekly/once, done-today skip, snooze, time zones),
  Room v2 (`MIGRATION_1_2` adds the tables; points/ownership untouched), `reminders/*` (one AlarmManager alarm for the earliest reminder via
  `setAndAllowWhileIdle`, notification with Done/Snooze, re-armed after reboot / app update / time change), tab "Habits".
  Alerts can be a few minutes late in Doze / battery saver (exact alarms were intentionally not requested). The Room migration was not run on a device.
* Prayer times (Habits tab > Prayer): computed on the device from a built-in city list (no GPS), methods MWL/Egyptian/Karachi/Umm al-Qura/ISNA, Asr standard or Hanafi, per-prayer minute adjustment. Times may differ by minutes from an official timetable; uncomputable times show N/A and are never guessed. Reminders use one inexact alarm (may be delayed in Doze). Pressing "I prayed" is optional; silence is never treated as confirmation. Data stays local and can be deleted.
* My own voice (Settings > My own voice): add up to 10 audio clips (20 s, 5 MB each), stored privately on the device. Adding clips never deletes the built-in voice. Source choice: built-in only (default), my clips only (falls back to built-in if none/failing), or clip then built-in voice. A clip cannot say new text, so the text still shows in the bubble. Not compiled/run here.
* Not implemented yet: in-app language switch, location auto-detect, usage stats, activity, microphone/voice input, AI layer, Context Engine.
