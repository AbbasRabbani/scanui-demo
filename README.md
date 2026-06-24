# ScanUI Demo — Proof of Concept for eParT 3D-Scanning UI (Datenrucksack)

A small standalone Android project built to demonstrate the four
requirements listed in the eParT student-assistant (HiWi) posting at
Hochschule Fulda, ahead of applying:

| Posting requirement | Where it's implemented |
|---|---|
| Android-based Benutzeroberfläche | `MainActivity.kt` + `activity_main.xml` |
| Live-Visualisierung der 3D-Daten auf einem Tablet | `gl/PointCloudRenderer.kt` (OpenGL ES 2.0) |
| Visualisierung des Systemzustands | Status panel (top-left overlay): state, scan rate, battery, storage |
| Import/Export der aufgenommenen Daten | `io/ScanFileIO.kt` — exports/imports standard ASCII `.ply` point clouds |

## Why it's structured this way

The real backpack hardware obviously isn't available to me, so
`data/ScanDataSource.kt` defines a small interface with one method to
start/stop and one to pull the next batch of points + system status.
`SimulatedBackpackSource` implements that interface with a synthetic
rotating "room scan" so the rest of the app — rendering, status display,
file I/O — can be built and shown working end-to-end. Swapping in a real
driver for the actual scanner later only means writing a new class behind
the same interface; nothing else changes.

Point-cloud export uses the actual PLY format (not a custom one), so
exported scans open directly in tools like CloudCompare or MeshLab —
useful if eParT's pipeline already uses point-cloud tooling downstream.

Code is in Kotlin. My Android background is from a 2018 course in Java,
plus an ongoing Java-based app (Qarya) and a university chat-app project;
Kotlin is JVM-based and interoperates directly with Java, so this was a
quick adaptation rather than starting from scratch.

## Running it

This needs Android Studio (or `sdkmanager` + `gradlew`) with the Android
SDK installed, which isn't available in the sandbox this was written in,
so it hasn't been compiled to a signed APK here. To run:

1. Open the `scanui-demo/` folder in Android Studio.
2. Let it sync Gradle (uses AGP 8.1.2 / Kotlin 1.9.10 / SDK 34, min SDK 24).
3. Run on a tablet or emulator in landscape.
4. Tap **Start Scan** — the point cloud fills in live and the status panel
   updates (state, points captured, scan rate, battery, storage).
5. Tap **Export .ply** to write the captured cloud to app-external storage,
   then **Import .ply** to reload it back into the view.

## Known simplifications (intentional, for a proof of concept)

- No camera/orbit gestures yet — the view auto-rotates so the 3D shape is
  visible without needing touch controls.
- No real hardware I/O (USB/Bluetooth) — isolated behind `ScanDataSource`.
- No persistence beyond a single export/import cycle.

These are the natural "next steps" if this turns into the real project.
