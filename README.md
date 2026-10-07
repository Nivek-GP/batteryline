<div align="center">
  <img src="resources/batteryline.svg" width="100" alt="BatteryLine">
  <h1>BatteryLine</h1>
  <p>A thin battery line across the Android status bar, always on top, in a few pixels.</p>
  <img src="https://img.shields.io/badge/android-13%2B%20(API%2033)-green" alt="Android">
  <img src="https://img.shields.io/badge/language-Kotlin-purple" alt="Kotlin">
</div>

## About

BatteryLine turns your battery level into a thin line across the top of the screen, getting shorter as the battery drains.

It started with a music player: on a small screen, a tiny battery icon is easy to miss and hard to read. A line that spans the whole screen is the opposite: quiet enough to forget, impossible to misread. It now runs on any device with Android 13 or later, in English and Spanish.

## Features

- **Battery at a glance** — see how much charge is left without reading a number, in any app
- **Barely there** — as thin as a single pixel, it blends into the status bar instead of covering your screen
- **Fits your screen** — matches rounded corners, phones and tablets, portrait and landscape
- **Smooth, never jumpy** — the line glides as the battery drains
- **Warns you in time** — turns red when the battery gets low
- **Made for charge limits** — if you stop charging at 80 %, the line can treat 80 % as full
- **Set and forget** — starts with your device; a Quick Settings tile turns it on or off
- **Light on battery** — it stays idle and only updates when the battery level changes

## Download

Go to the [Releases page](https://github.com/Nivek-GP/batteryline/releases) and download `BatteryLine-x.x.x.apk`.

## Installation

1. On your device, enable **Developer options** and **USB debugging**
2. Connect it and check that it shows up with `adb devices`
3. Install the app:

```bash
adb install -r BatteryLine-x.x.x.apk
```

> You can also copy the APK to the device and open it from a file manager, after allowing it to install unknown apps.

## Setup

1. Open **BatteryLine** and tap **Allow display over other apps**, then enable it for BatteryLine
2. Turn on **Show line**
3. **Line** — set the thickness (1 px sits right on the top edge), opacity and fixed end
4. **Edges** — tap **Auto-adjust to the corners**, or use the sliders and their −/+ buttons until both ends of the line are fully visible. Turn on **Simulate battery level** at 100 % to see the whole line while you adjust.
5. **Battery optimization** — turn it off for BatteryLine so aggressive ROMs don't stop the line
6. **Add tile** — adds the on/off tile to Quick Settings

> **No permission screen?** Some music player ROMs don't include the *Display over other apps* screen. Grant it over adb instead:
>
> ```bash
> adb shell appops set dev.kevin.batteryline SYSTEM_ALERT_WINDOW allow
> ```

BatteryLine runs as a foreground service. It doesn't ask for notification permission, so on Android 13+ its notification stays out of the shade.

BatteryLine doesn't show up in Recents, so it can't be swiped away by accident (some ROMs force-stop apps swiped from Recents, which would also remove the line). To change settings, open it from the app drawer or long-press its Quick Settings tile.

## Testing without draining the battery

```bash
adb shell dumpsys battery set level 69   # fake a level
adb shell dumpsys battery set level 10   # low battery: the line turns red
adb shell dumpsys battery reset          # back to the real level
```

## Building from source

Requires [Android Studio](https://developer.android.com/studio), or just its bundled JDK and the Android SDK.

Stack: Kotlin, Gradle 9.8, AGP 9.4 (built-in Kotlin), `compileSdk` 37, `minSdk`/`targetSdk` 33.

```bash
export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"  # Android Studio's JDK on Windows
./gradlew assembleRelease  # APK in app/build/outputs/apk/release/
./gradlew assembleDebug    # debug build
```

> Release APKs are signed with the debug key, which is enough for sideloading on your own device.

## Related

- [App Launch Tools](https://github.com/Nivek-GP/app-launch-tools) — ShortcutLauncher and QuickLaunch, same look, same devices
