# How Far To...

Android app that shows how far away a searched location is from your current position.

## Building

This is a standard Gradle project (Android Studio or `./gradlew`).

The Google Maps API key is kept out of version control. Before building, create
`app/src/main/res/values/keys.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="maps_api_key">YOUR_MAPS_API_KEY_HERE</string>
</resources>
```

Note: the app now uses the Maps SDK for Android (v2+). Old Maps v1 API keys do not
work — create an API key with "Maps SDK for Android" enabled in the
[Google Cloud console](https://console.cloud.google.com/google/maps-apis).

Build a release bundle for Play with:

```
./gradlew :app:bundleRelease
```
