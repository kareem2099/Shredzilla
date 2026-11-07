# DotShredzilla Google Play listing artwork

Version photographed: 1.5.1 (9). All UI panels come from the production Compose
screens rendered with synthetic local demonstration data. No account, training
history, credentials or personal device content is used in these assets.

- `screens/`: unmodified UI captures from the isolated `.storecapture` debug app.
- `captions.json`: English marketing captions; the native UI is English.
- `export.py`: reproducible vector layouts with the original captures embedded.
- `../../fastlane/metadata/android/`: listing descriptions and final RGB PNGs.
- `contact-sheet.png`: review sheet of phone screenshots and feature graphics.

## Capture

The screenshot cases are skipped in normal instrumentation runs.

```sh
./gradlew :app:assembleDebug :app:assembleDebugAndroidTest -PstoreListingCapture=true
adb install -r app/build/storeCapture/outputs/apk/debug/app-debug.apk
adb install -r app/build/storeCapture/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w -r -e class com.FreeRave.shredzilla.review.StoreListingScreenshotTest com.FreeRave.shredzilla.storecapture.test/com.FreeRave.shredzilla.review.StoreCaptureRunner
```

Retrieve `cache/store-{phone,tablet}-{page}.png` with `adb exec-out run-as
com.FreeRave.shredzilla.storecapture cat ...`. The capture flag uses a separate
package and build directory, removes network permissions and SDK startup
providers, and replaces the application with an instrumentation-only runner.
It does not sign in or overwrite the installed production app. Release identity
and configuration remain unchanged. Never build a store release with this flag.

## Render

Use system Python with PyGObject, Rsvg, Cairo and Noto Sans:

```sh
/usr/bin/python3 marketing/google-play/export_icon.py
/usr/bin/python3 marketing/google-play/export.py
/usr/bin/python3 marketing/google-play/render.py
```

Phone: six 1080×1920 screenshots in English. Tablet: four real responsive
840×1120 dp Compose captures, framed at 1080×1920, reused in the 7-inch and
10-inch slots. Feature graphic: 1024×500. PNGs are RGB without transparency.
The frame and text are drawn as SVG; screenshot content is not retouched.

Support: support@dotsuite.dev

- Play: https://play.google.com/store/apps/details?id=com.FreeRave.shredzilla
- Website: https://dotsuite.dev
- Privacy: https://dotsuite.dev/en/privacy#dotshredzilla
- Terms: https://dotsuite.dev/en/dotshredzilla/terms
- Deletion: https://dotsuite.dev/en/privacy#delete-dotshredzilla
- Source: https://github.com/kareem2099/Shredzilla

The store icon is exported from the current Android vector launcher layers by
`export_icon.py`, which also updates `app/src/main/ic_launcher-playstore.png`.

## Verification

The ten capture cases passed on a connected Android device. All 16 final PNGs
were checked for dimensions, RGB color type, file size and chunk integrity.
`checksums.json` identifies both raw captures and final artwork.
