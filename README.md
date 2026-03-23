# AppDistribution Android

Android client for browsing, downloading, and installing internal APK builds from the AppDistribution backend.

## Prerequisites

- Android Studio Iguana or newer
- JDK 17
- Android SDK 35

## Run

Open this directory as a standalone Gradle project in Android Studio, or use Gradle from the repo root:

```bash
./gradlew assembleDebug
```

The debug build points to `http://10.0.2.2:8080/`, so the backend must be reachable from the Android emulator.

## Firebase / Push Notifications

The repository intentionally contains a dummy `app/google-services.json` so the project can be opened publicly without exposing a real Firebase project.

- The file is sufficient for local builds.
- FCM push notifications will not work until you replace it with a real config from your own Firebase project.
- Backend push delivery also requires a valid Firebase service-account JSON on the server side.

## Publishing Notes

- `google-services.json` in this repo is a non-production placeholder.
- Do not commit a real Firebase config or release signing keystore to the public repository.
