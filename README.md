# Lock In

*(Note: the project folder, package name `com.lockit.app`, and internal code
still say "LockIt" — only the display name shown on your phone has been
changed to "Lock In". Renaming the package is a bigger refactor; happy to do
it if you want the code to match too, just ask.)*

A free, fully on-device Android app: lock apps behind a to-do list, unlock them
proportionally by task weightage, verify tasks with on-device ML (no API key,
no server, no ongoing cost), and use weekly emergency tokens to skip the lock.

## What's actually working in this code

- **Todo list**: short-term / long-term, one-time / recurring, per-task
  weightage 0–100% (`AddTaskScreen.kt`, `Task.kt`)
- **Unlock math**: weightage → % of daily app time unlocked, full completion →
  full unlock (`UnlockCalculator.kt` — this is the core logic you described,
  read it first)
- **Locked apps list** with a daily minutes budget (`LockedAppsScreen.kt`)
- **Weekly emergency tokens** (10/week, resets automatically via
  `WeeklyTokenResetWorker.kt`, no server needed)
- **On-device photo verification** using Google ML Kit Image Labeling —
  free, no API key, works offline (`MLKitVerifier.kt`)
- **App-lock overlay** via `AccessibilityService` that detects when a locked
  app opens and shows a full-screen lock screen instead
  (`AppLockAccessibilityService.kt`, `LockScreenActivity.kt`)
- Red / green / black / white / grey theme (`Theme.kt`, `colors.xml`)

## What you need to finish/wire up yourself in Android Studio

This is real, compilable Kotlin — but a few pieces are intentionally left as
clearly-marked stubs because they need a real device/Play Services environment
to test properly, not a sandbox:

1. **App picker** (`LockedAppsScreen.kt` → `AppPickerDialog`): currently a
   manual text-entry dialog. Swap it for a `LazyColumn` populated from
   `packageManager.getInstalledApplications(PackageManager.GET_META_DATA)`
   filtered to apps with a launcher intent. This is ~15 lines, I kept it
   manual-entry so the file compiles without needing a live device to test
   the package list against.
2. **Camera capture screen**: `MLKitVerifier.verify()` is ready to call, but
   there's no CameraX preview screen wired to it yet. Add a
   `CameraX PreviewView` + capture button, pass the resulting `Bitmap` to
   `MLKitVerifier.verify(bitmap, expectedLabels)`, then call
   `viewModel.markComplete(task)` if `passed == true`.
3. **Timer / Location / Health Connect verification**: `VerificationMethod`
   enum and UI selector are in place; the actual timer countdown screen, GPS
   distance check, and Health Connect step-count read need their own small
   screens — same pattern as the camera one above.
4. **Live unlock-minutes tracking**: `LockScreenActivity` currently reads
   task counts as placeholders in a couple of spots (commented clearly in the
   code) — wire it to `TodoViewModel`'s real `Flow`s once you're running on
   a device so it recalculates live instead of once on screen-open.

## How to build and install (no coding needed, just clicking)

1. Install **Android Studio** (free): https://developer.android.com/studio
2. Open Android Studio → **Open** → select this `LockIt` folder
3. Let Gradle sync (first time takes a few minutes, downloads dependencies)
4. Plug your phone in via USB, enable **Developer Options → USB Debugging**
   on the phone (Settings → About Phone → tap Build Number 7 times, then
   Settings → Developer Options)
5. Click the green **Run ▶** button in Android Studio, select your phone
6. First launch, the app will ask you to:
   - Enable it as an **Accessibility Service** (Settings → Accessibility →
     Lock In → turn on) — this is what lets it detect locked apps opening
   - Grant **"Display over other apps"** permission — this is what lets it
     show the lock screen
   - Grant Camera permission if you plan to use camera-scan verification

## Running it via GitHub

There are two ways to use GitHub here, depending on whether you want to build
locally in Android Studio or let GitHub build the APK for you in the cloud.

### Option A — Push to GitHub, then open in Android Studio (same result as before, just version-controlled)

1. Create a new empty repo on github.com (e.g. `lockit-app`), don't add a
   README/gitignore there — you already have those.
2. On your machine, in this unzipped `LockIt` folder, run:
   ```
   git init
   git add .
   git commit -m "Initial Lock In app"
   git branch -M main
   git remote add origin https://github.com/YOUR_USERNAME/lockit-app.git
   git push -u origin main
   ```
3. In Android Studio: **File → New → Project from Version Control**, paste
   your repo URL, and open it. From here it's identical to opening the
   folder directly — sync, plug in your phone, hit Run ▶.

### Option B — Let GitHub build the APK for you (no Android Studio needed at all)

This repo includes `.github/workflows/build.yml`, which tells GitHub Actions
to compile a debug APK automatically every time you push.

1. Do steps 1–2 from Option A above (create repo, push code).
2. Go to your repo on github.com → the **Actions** tab. You should see a
   "Build Debug APK" workflow run automatically (triggered by your push).
   If it doesn't appear, click **Actions → Build Debug APK → Run workflow**
   to trigger it manually.
3. Wait 2–4 minutes for it to finish (green checkmark).
4. Click into the completed run → scroll to **Artifacts** at the bottom →
   download **LockIn-debug-apk** (this downloads a `.zip` containing
   `app-debug.apk`).
5. Unzip it on your phone (or transfer the `.apk` to your phone via USB,
   Google Drive, email — whatever's easiest).
6. On your phone, tap the `.apk` file to install it. You'll need to allow
   **"Install unknown apps"** for whichever app you used to open the file
   (Android will prompt you and link straight to the setting).
7. Once installed, open Lock In and grant the Accessibility Service and
   "Display over other apps" permissions as described above.

This means every time you (or I) edit the code and push, GitHub rebuilds a
fresh installable APK for you automatically — no local Android Studio setup
required. This costs nothing: GitHub Actions is free for this kind of usage.

## Cost

$0. Everything — database, ML Kit, WorkManager, Accessibility Service — runs
entirely on your phone. No backend, no API key, no subscription. GitHub
Actions (Option B) is also free at this scale.
