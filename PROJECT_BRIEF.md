# Online Wallpaper — Android App Spec

## Goal
A native Android (Kotlin) app called **"Online Wallpaper"**. It downloads an image from a
user-provided URL and sets it as the **lock screen wallpaper** using Android's
`WallpaperManager` system, on a periodic schedule chosen by the user.

## Package / naming
- Package id: `link.bury.onlinewallpaper`
- App display name: "Online Wallpaper"
- Min SDK: 24 (Android 7.0), Target/Compile SDK: latest stable (35)
- Kotlin + Jetpack Compose for the single settings/status screen (or plain Views if simpler —
  your call, but keep it minimal, one screen).

## Core Features

### 1. Settings screen (single Activity)
Inputs:
- **Image URL** (text field, validated as a URL, persisted)
- **Refresh interval** (dropdown/picker: e.g. 15 min, 30 min, 1h, 6h, 12h, 1 day — respect
  WorkManager's practical minimum of 15 minutes for periodic work)
- **Enable/Disable** toggle to start/stop the periodic refresh
- **"Set now" button** to trigger an immediate fetch + set, independent of the schedule
- Status area showing: last successful update timestamp, last error (if any), next scheduled run

Persist settings with `DataStore` (Preferences) or `SharedPreferences`.

### 2. Wallpaper refresh worker
- Use **WorkManager** `PeriodicWorkRequest` with the configured interval.
- Worker responsibilities each run:
  1. Download the image from the stored URL (use `HttpURLConnection` or OkHttp — OkHttp preferred,
     add as a dependency).
  2. Decode to `Bitmap` (handle large images: downsample via `BitmapFactory.Options.inSampleSize`
     to roughly the device's screen resolution to avoid OOM).
  3. Call `WallpaperManager.getInstance(context).setBitmap(bitmap, null, true, WallpaperManager.FLAG_LOCK)`
     to set **lock screen only** (do not touch `FLAG_SYSTEM`/home screen).
  4. On success: update last-success timestamp in persisted state.
  5. On failure (network error, bad image, decode failure): update last-error message in persisted
     state, use WorkManager's retry/backoff (`Result.retry()` with exponential backoff policy),
     but don't crash.
- Re-enqueue the periodic work whenever the user changes the interval or toggles enable/disable
  (cancel + re-enqueue via a unique work name, `ExistingPeriodicWorkPolicy.UPDATE` or
  `.CANCEL_AND_REENQUEUE`).
- Handle `WorkManager` constraints: require network connectivity (`NetworkType.CONNECTED`).

### 3. Permissions / manifest
- `INTERNET` permission.
- `ACCESS_NETWORK_STATE` (for constraints).
- No storage permission needed if we don't persist the image to disk (fetch fresh each cycle is
  fine; optionally cache the last successful bitmap in internal cache dir so status screen can
  show a thumbnail preview, using `context.cacheDir`, no external storage).
- Declare `WallpaperManager` usage doesn't need a special permission beyond internet.

### 4. Robustness / UX details
- Show a small thumbnail preview of the last-applied wallpaper on the settings screen (optional
  nice-to-have, skip if it complicates things too much).
- Validate URL format before allowing the user to save/enable.
- If WorkManager reports the periodic worker hasn't run recently (e.g. due to Doze/battery
  optimization), surface a hint to the user to disable battery optimization for the app
  (`Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)`), but don't force it.
- Handle Android 13+ notification permission gracefully IF you add a foreground/notification for
  the worker — prefer NOT using a foreground service; plain WorkManager background work should be
  used since this isn't time-critical, exact work.

## Project structure
Standard Gradle Android app project (single module `app`), Kotlin DSL (`build.gradle.kts`),
Gradle wrapper included so it builds with `./gradlew assembleDebug` with no other setup beyond
an Android SDK.

## Deliverables
1. Full working Gradle Android project in this repo, buildable with `./gradlew assembleDebug`.
2. `README.md` explaining: what the app does, how to build/install it, how the refresh mechanism
   works, and known limitations (e.g. minimum 15-min WorkManager interval, lock-screen-only scope).
3. Sensible `.gitignore` for Android/Gradle projects (build/, .gradle/, local.properties, etc).
4. Commit all work with clear commit message(s) and push to the `main` branch of this repo
   (origin is already set to https://github.com/bury-link/online-wallpaper-app.git).

## Explicitly out of scope
- Home screen wallpaper (lock screen only).
- True Android Live Wallpaper Service (`WallpaperService`) — not needed, static bitmap + periodic
  WorkManager refresh only.
- iOS — Android only.
- CI/CD, Play Store listing, signing config — not needed for this task.
