# ScarGuard

An Android app for at-home post-operation wound monitoring. It compares a "before" baseline
photo of an incision against daily follow-up photos to flag increasing redness/skin-tone change,
and pairs with an ESP32 sensor node over Bluetooth Low Energy (BLE) for continuous temperature
monitoring of the area -- both are early warning signs of infection.

**This is a home-monitoring aid, not a medical device.** It does not diagnose infection and does
not replace professional care. That disclaimer is also shown in the app's Settings screen.

## Design decision worth flagging

The prompt asked for "camera and temperature sensors" on the ESP32. In practice, transferring
camera-quality images over classic Bluetooth/BLE from an ESP32-CAM is slow, low resolution, and a
poor way to judge skin tone/redness. So this build uses:

- **The phone's own camera** (via CameraX) for the baseline and follow-up photos -- much higher
  quality, and it's the phone the person already has in hand.
- **The ESP32 + a temperature sensor** purely for continuous temperature monitoring, streamed to
  the app over BLE.
- A **command channel** back to the ESP32 so the app can ask it to blink an LED, e.g. as a visual
  cue while lining up the camera -- so the board still has a role in the capture flow, not just
  temperature.

If you specifically need the ESP32 to also capture images (e.g. with an ESP32-CAM you already
have), that's a separate, larger extension (JPEG chunking over BLE or switching the sensor node to
Wi-Fi/HTTP for image transfer) -- happy to build that out as a follow-up if you want it.

## Project layout

```
app/                          Android app (Kotlin, Jetpack Compose, Material 3)
  src/main/java/com/scarguard/app/
    ble/                       BLE GATT client (EspBleManager) talking to the ESP32
    camera/                    CameraX capture wrapper
    vision/                    RednessAnalyzer -- pure-Kotlin skin tone/redness comparison
    data/                      Room entities/DAOs, risk classification
    settings/                  DataStore-backed alert thresholds
    repository/                MonitoringRepository -- ties BLE + vision + DB + settings together
    notifications/             Notification channels + alert notifications
    service/                   MonitoringService -- foreground service, keeps BLE alive in background
    ui/                        Compose screens (home, connect, baseline, monitor, history, settings)
firmware/esp32_scar_monitor/  Reference Arduino sketch for the ESP32 sensor node
```

## How the monitoring works

1. **Baseline**: take one reference photo of the incision (ideally right after the operation).
   The app records the photo plus a redness/skin-tone score, and the sensor's current temperature
   if connected.
2. **Ongoing temperature**: once paired, the ESP32 streams a temperature reading every couple of
   seconds over BLE. The app keeps a live reading on the Home screen, logs a sample periodically
   for the trend chart, and raises a notification the moment the delta vs baseline crosses your
   Watch/Alert thresholds (Settings screen).
3. **Follow-up checks**: take a new photo any time, framed the same way as the baseline via an
   on-screen alignment guide. The app compares it against the baseline (hue/saturation-based
   redness score, plus overall color drift for bruising/discoloration), shows a redness heatmap,
   and combines that with the current temperature delta into one Normal / Watch / Alert result.

## Building the app

1. Open the `ScarGuard/` folder (this repo root) directly in Android Studio (Koala or newer
   recommended) -- it's a standard Gradle project, so "Open" and let it sync.
2. Android Studio will download the Gradle distribution, Android SDK platform 34, and all
   dependencies (Compose, CameraX, Room, Navigation) on first sync -- this needs an internet
   connection once.
3. Run on a real device (BLE + camera don't work in the emulator in any useful way). Minimum
   Android 8.0 (API 26).

No API keys or backend are required -- everything runs on-device.

## Flashing the ESP32

`firmware/esp32_scar_monitor/esp32_scar_monitor.ino` is a reference sketch for any ESP32 dev
board (no ESP32-CAM needed):

1. Install the `esp32` board package in the Arduino IDE (or PlatformIO) -- the sketch only uses
   the BLE library bundled with that core, no extra libraries required as written.
2. Default wiring is a simple NTC thermistor voltage divider on pin 34 (cheap, no extra library).
   The sketch has clearly-commented alternate code paths for a DS18B20 (contact digital sensor) or
   an MLX90614 (non-contact IR sensor) if you have one of those instead -- either is a better
   long-term choice than a bare NTC for skin temperature.
3. Flash it, open the Serial Monitor at 115200 baud to confirm readings, then open the app's
   Device tab, scan, and connect to "ScarGuard Sensor".

The BLE service/characteristic UUIDs are defined in both
[`BleConstants.kt`](app/src/main/java/com/scarguard/app/ble/BleConstants.kt) and the firmware --
keep them in sync if you change either side.

## Permissions the app requests

- **Camera** -- for baseline/follow-up photos.
- **Bluetooth** (Nearby devices on Android 12+, or Bluetooth + location on older versions) -- to
  scan for and connect to the ESP32. Android requires location permission for BLE scanning on
  versions before 12; the app does not use it for actual location data.
- **Notifications** -- to alert you when a check crosses into Watch/Alert.

## Known limitations / good next steps

- The redness analysis is a deliberately simple, explainable HSV-based heuristic (no ML model) --
  it's tuned to be useful, not diagnostic. A future version could add on-device ML (e.g. a small
  TFLite classifier) for more robust redness/wound-state detection.
- Photo alignment relies on an on-screen guide rather than true image registration; for best
  results take follow-ups in similar lighting and distance to the baseline.
- The temperature trend currently only starts once a baseline exists and the sensor has connected
  at least once.
