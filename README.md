# GPS Receiver

Viewer app — enters a pairing code and shows the other phone's live location
on a dark, satellite-tile map (osmdroid + Esri World Imagery).

## One-time setup (before first build)

1. Use the **same Firebase project** as GPS Sender.
2. In Project Settings, add another Android app with package name `com.holomatos.gpsreceiver`.
3. Download the generated `google-services.json` and place it at `app/google-services.json` in this repo.
4. Commit and push — GitHub Actions will build the APK automatically.

## How it works

1. Enter the same pairing code used on the GPS Sender phone.
2. Tap "TRACK".
3. The app listens to `locations/{code}` in Firebase and moves a marker on
   the satellite map in real time as updates arrive.

## Build

GitHub Actions builds a debug APK on every push to `main`. Grab it from the
Actions tab → latest run → Artifacts.

## Next improvements (not yet implemented)

- Show last-seen timestamp / staleness warning
- Multiple simultaneous tracked codes
- Firebase Realtime Database security rules (currently wide open in test mode)
