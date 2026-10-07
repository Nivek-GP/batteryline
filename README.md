<div align="center">
  <img src="resources/batteryline.svg" width="100" alt="BatteryLine">
  <h1>BatteryLine</h1>
  <p>A thin battery line across the Android status bar, always on top, in a few pixels.</p>
  <img src="https://img.shields.io/badge/android-13%2B%20(API%2033)-green" alt="Android">
  <img src="https://img.shields.io/badge/language-Kotlin-purple" alt="Kotlin">
</div>

## About

BatteryLine draws a thin white line along the top edge of the screen. The full screen width (minus the edges you set) means a full battery, and the line gets shorter as the battery drains. It sits over every app, starts with the device and can be turned on or off from a Quick Settings tile.

It was built for a **HiBy M300** music player (Android 13, portrait) and an **Alldocube iPlay 60 Mini Turbo** tablet (Android 14, portrait and landscape), but it works on any Android 13+ device.

Dark monochrome UI with glass cards. It follows the system language: Spanish when the system is set to Spanish, English otherwise.

## Features

- **Always on top** — the line shows over every app, under the status bar icons, and the notification shade covers it when you pull it down
- **Thickness from 1 px** — capped at the status bar height so the line never leaves the bar, with an optional distance from the top
- **Smooth changes** — when the level changes by 1 %, the line slides to its new length instead of jumping
- **Custom full scale** — choose which level counts as a full line (e.g. 80 % if you limit charging to 80 %)
- **Fixed end** — the line can shrink toward the right, the left or the center
- **Low battery** — turns dark red below a level you choose (15 % by default)
- **Edges for rounded corners** — left and right margins, saved separately for portrait and landscape, plus **Auto-adjust**, which reads the corner radius the system reports
- **Live preview** — simulate any battery level while tuning the line
- **Starts on boot** and comes back after app updates
- **Quick Settings tile** — tap to turn the line on or off; long-press to open the settings
- **Light on battery** — no polling, timers or wakelocks: the line only redraws when the level changes, and only animates while the screen is on

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
4. **Edges** — tap **Auto-adjust to the corners**, or move the sliders until both ends of the line are fully visible. Turn on **Simulate battery level** at 100 % to see the whole line while you adjust. On a tablet, rotate it and repeat for the other orientation
5. **Battery optimization** — turn it off for BatteryLine so aggressive ROMs don't stop the line
6. **Add tile** — adds the on/off tile to Quick Settings

> **No permission screen?** Some music player ROMs don't include the *Display over other apps* screen. Grant it over adb instead:
>
> ```bash
> adb shell appops set dev.kevin.batteryline SYSTEM_ALERT_WINDOW allow
> ```

BatteryLine runs as a foreground service. It doesn't ask for notification permission, so on Android 13+ its notification stays out of the shade.

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
