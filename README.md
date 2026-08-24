# Online Wallpaper

An Android app that downloads an image from a URL you choose and sets it as your **lock screen
wallpaper**, on a schedule you choose. Your home screen wallpaper is never touched.

<img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.png" width="72" alt="App icon">

## What it does

A single settings screen lets you:

- **Image URL** — paste a direct link to a JPEG, PNG or WebP. Validated as an absolute `http(s)`
  URL before the schedule can be turned on.
- **Refresh interval** — 15 minutes, 30 minutes, 1 hour, 6 hours, 12 hours or 1 day.
- **Automatic refresh** — a toggle that starts and stops the periodic background work.
- **Set now** — fetches and applies the wallpaper immediately, whether or not the schedule is on.
- **Status** — last successful update, the most recent error (if any), the next scheduled run, and
  a thumbnail of the wallpaper that was last applied.

If the app is subject to battery optimization — or if the last successful update is much older than
the interval you picked — the screen shows a hint card with a shortcut to the system battery
settings. It disappears once the app is exempt; the app never demands the exemption.

## Building and installing

You need an Android SDK with **platform 35** and **build-tools 35.0.0**, and a **JDK 17**. Nothing
else: the Gradle wrapper is committed, so Gradle itself is downloaded on first run.

```bash
# Point Gradle at your SDK (or set the ANDROID_HOME environment variable)
echo "sdk.dir=/path/to/Android/sdk" > local.properties

./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Other useful tasks:

```bash
./gradlew testDebugUnitTest   # unit tests for the URL validation and downsampling logic
./gradlew lintDebug           # Android lint
```

Opening the project in Android Studio (Ladybug or newer) works too — it writes `local.properties`
for you.

## How the refresh works

```
Settings screen ──> DataStore (url, interval, enabled, last success, last error)
       │
       └─> WallpaperScheduler ──> WorkManager unique periodic work "wallpaper-refresh"
                                          │
                                          └─> WallpaperWorker
                                                 1. download the URL with OkHttp
                                                 2. decode, downsampled to screen size
                                                 3. WallpaperManager.setBitmap(…, FLAG_LOCK)
                                                 4. record success or error in DataStore
```

- **Scheduling.** The periodic work is enqueued under the unique name `wallpaper-refresh` with
  `ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE`, so changing the interval or re-enabling the
  toggle restarts the schedule cleanly rather than leaving the old period running. Turning the
  toggle off cancels the unique work. The work requires `NetworkType.CONNECTED`.
- **"Set now"** enqueues a separate one-shot unique work (`wallpaper-refresh-now`) with no network
  constraint, so an offline device reports the error immediately instead of queueing silently.
- **Downloading.** OkHttp streams the response into a file in `cacheDir` so the image is never held
  in memory twice. The file is deleted as soon as the wallpaper is applied.
- **Decoding.** `BitmapFactory` is run twice — once with `inJustDecodeBounds` to read the
  dimensions, then with an `inSampleSize` that is the largest power of two keeping the image at or
  above `WallpaperManager.getDesiredMinimumWidth()/Height()`. If a decode still hits
  `OutOfMemoryError`, the sample size is doubled and retried up to three times.
- **Applying.** `WallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_LOCK)` —
  lock screen only. `FLAG_SYSTEM` is never passed.
- **Failure handling.** Failures are classified. *Transient* ones (no connectivity, timeout, HTTP
  408/429/5xx, out of memory) return `Result.retry()`, which WorkManager retries with an
  exponential backoff starting at one minute, up to three attempts per run. *Permanent* ones
  (malformed URL, HTTP 404, a response that is not a decodable image) return `Result.failure()`
  immediately — retrying cannot help, and the next scheduled run will try again anyway. Either way
  the message is written to DataStore and shown on the settings screen. The worker never crashes.
- **Recovery.** On launch, if the schedule is enabled but WorkManager has no live work for it (for
  example after the app was force-stopped), it is re-enqueued.

## Permissions

| Permission | Why |
| --- | --- |
| `INTERNET` | Download the image. |
| `ACCESS_NETWORK_STATE` | WorkManager's `NetworkType.CONNECTED` constraint. |
| `SET_WALLPAPER` | Required by `WallpaperManager.setBitmap()`. |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Only to offer the exemption dialog from the hint card. |

All four are install-time permissions, so there is no runtime prompt. No storage permission is
needed: the only file the app keeps is a small preview thumbnail in the internal cache directory.

The WorkManager library merges three more into the final manifest — `WAKE_LOCK`,
`RECEIVE_BOOT_COMPLETED` and `FOREGROUND_SERVICE` — so that scheduled work survives a reboot. This
app itself starts no foreground service and posts no notifications.

## Known limitations

- **15 minutes is the floor.** WorkManager clamps periodic work to a minimum period of 15 minutes,
  so that is the shortest interval offered.
- **The interval is a target, not a guarantee.** Android batches deferrable background work and
  Doze can delay it considerably, especially on OEM builds with aggressive battery management. A
  run can arrive late, and on a device that is asleep and offline it can be skipped until
  connectivity returns. This is why the app surfaces the battery-optimization hint.
- **Lock screen only.** By design. There is no option to set the home screen wallpaper.
- **Not a live wallpaper.** The app sets a static bitmap on a schedule; it does not implement
  `WallpaperService`.
- **Some devices do not support a separate lock screen wallpaper.** Where
  `WallpaperManager.isWallpaperSupported()` or `isSetWallpaperAllowed()` returns false — including
  devices under a restrictive device-policy manager — the app reports this as an error instead of
  failing silently.
- **Redirects and authentication.** OkHttp follows redirects, but URLs that need cookies, headers
  or a login will not work. Use a direct image link.
- **No cache validation.** Every run downloads the image in full; there is no `ETag`/
  `If-Modified-Since` handling, so a URL that always returns the same picture still costs a
  download each cycle.

## Decisions worth flagging

- **`SET_WALLPAPER` is required, contrary to the spec.** The brief said no permission beyond
  `INTERNET` was needed. That is not correct — `WallpaperManager.setBitmap()` throws
  `SecurityException` without `android.permission.SET_WALLPAPER`. It is declared in the manifest.
  Android lint flags this too, so the omission would have failed the build.
- **`CANCEL_AND_REENQUEUE` over `UPDATE`.** The spec allowed either. Cancel-and-re-enqueue makes
  "next scheduled run" on the status screen mean what the user just chose, instead of continuing
  the period that was started under the old interval.
- **The thumbnail preview is included.** It was optional in the brief. It is a ≤512 px JPEG in
  `cacheDir`, written only after a wallpaper is applied successfully, and it never blocks or fails
  an update.
- **No notifications and no foreground service.** As the brief preferred, this is plain deferrable
  background work, which also means no `POST_NOTIFICATIONS` handling is needed on Android 13+.
- **The URL is persisted on a 400 ms debounce** as you type, so there is no separate save button
  and nothing is lost if the app is backgrounded mid-edit.
- **Dependency versions are pinned to a set that is verified to build.** Lint will report newer
  releases of AGP, Compose and AndroidX; upgrading (especially to AGP 9.x) has not been verified
  here, so the pinned versions are deliberate.
- **The launcher icon is generated, not hand-drawn.** `tools/generate_launcher_icons.py` renders
  the legacy PNG mipmaps with no third-party dependencies; the adaptive and monochrome icons are
  vector drawables using the same shapes. Re-run the script only if the artwork changes.

## Project layout

```
app/src/main/java/link/bury/onlinewallpaper/
├── data/          settings model, DataStore repository, interval enum, URL validation
├── wallpaper/     download, downsampled decode, thumbnail cache, applying to FLAG_LOCK
├── work/          the WorkManager worker and the scheduler that owns the unique work
└── ui/            MainActivity, the Compose settings screen, its ViewModel and theme
```
