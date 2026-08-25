# Release checklist — Online Wallpaper

## Completed in this repository

- English default UI resources and German resources under `values-de/`
- Localized refresh intervals and user-visible wallpaper errors
- In-app privacy-policy card linking to `https://bury.link/privacy/online-wallpaper`
- Privacy-policy source pages: `store-listing/privacy-policy.html` (German) and `privacy-policy-en.html` (English)
- Play Store copy draft: `store-listing/PLAY_STORE_LISTING.md`
- Optional release signing configuration via ignored `keystore.properties`
- `bundleRelease` build configuration verified

## Required manual steps before Play upload

1. **Publish the privacy policy**
   - Host the German policy at `https://bury.link/privacy/online-wallpaper`.
   - Make the English policy reachable from that page or use language negotiation.
   - Confirm the final URL in a browser; it was not reachable from this build environment.

2. **Create and back up the upload key**
   - Copy `keystore.properties.example` to ignored `keystore.properties` and set strong unique values.
   - Generate the keystore at the configured `storeFile` path, for example:
     `keytool -genkeypair -v -keystore release/online-wallpaper-upload.jks -alias online-wallpaper-upload -keyalg RSA -keysize 4096 -validity 10000`
   - Back up both the `.jks` and password-manager entry before uploading any bundle.

3. **Create a signed production AAB**
   - Run `./gradlew bundleRelease` after configuring `keystore.properties`.
   - Upload `app/build/outputs/bundle/release/app-release.aab` to an Internal testing track first.

4. **Prepare store assets**
   - Create the feature graphic and real screenshots listed in `store-listing/PLAY_STORE_LISTING.md`.
   - Do not use debug APK screenshots for the final listing without validating the release build.

5. **Complete Play Console declarations**
   - App access: no login required.
   - Data safety: review every library and the direct URL-download behaviour before submitting; do not claim a declaration without checking the live app.
   - Content rating, target audience, category, contact details and privacy-policy URL.
   - Run the Play pre-launch report from the Internal testing track.

6. **Test on devices**
   - Test manual setting, scheduled updates, HTTP LAN URL, HTTPS URL, a failed URL, and lock-screen behaviour on at least two physical devices.

## Release hygiene

- Increment `versionCode` for every Play upload.
- Keep the debug APK separate from the signed AAB.
- Commit and push source changes before manually uploading the first release.
