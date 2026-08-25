# Release checklist

## Automated gate

- JDK 21, Android API 37, checked-in Gradle wrapper
- `testDebugUnitTest`
- `lintDebug`
- `assembleDebug` and minified `assembleRelease`
- `bundleRelease` with minification and resource shrinking
- CI artifacts: debug APK, unsigned release APK, and unsigned release AAB
- CI size ceiling: release APK and AAB must each remain at or below 12 MiB
- Phase 1 extraction tests: invalid dates, multiple labeled dates, and month/year normalization
- Phase 1 attachment tests: MIME allowlist, 20 MiB bound, filename sanitization, and owned-path enforcement
- Room 1→2 migration retains Track/Renew data and adds an optional encrypted barcode field

## Manual device gate

- Test light/dark/system theme and dynamic colors
- Deny and grant Android 13+ notification permission
- Verify daily reminder tap opens the matching Track or Renew detail
- Export a backup, modify data, restore, and verify all domains/history/settings
- Add and resize the widget; verify counts update after mutations
- Exercise TalkBack labels, large font, landscape, and smallest supported screen
- Test fresh install and upgrade from 0.1.0 without clearing app data
- Test release blocking on a rooted physical device and at least two emulator families; confirm debug builds remain available
- Confirm release screenshots, overlays, obscured touches, cleartext traffic, OS backup, and widget/reminder data access are blocked
- Inspect a release install's local files and confirm sensitive Room/DataStore values are ciphertext; verify an existing plaintext development install migrates records as they are saved
- Confirm a tampered encrypted payload fails closed and does not render corrupted data
- Deny/cancel every scanner and system-picker flow; confirm manual Track/Renew entry remains usable
- Inspect logs and crash breadcrumbs after OCR failure; confirm no OCR text, document reference, or sensitive number appears
- Confirm discarded camera captures are removed and attachment removal leaves the original gallery/document file intact
- Test Smart Add before and after Google Play services model download; verify cancellation and model failure return to manual entry

## Play Console gate

- Use a protected upload key; never commit signing material
- Complete Data safety using `PRIVACY_POLICY.md` as the product baseline
- Add store listing graphics/screenshots and support contact
- Upload the signed AAB to internal testing, then closed testing
- Review Android vitals, pre-launch report, and accessibility report before production
- Compare signed AAB and Play Console download-size estimates with the recorded release baseline; investigate growth before promotion
- Register the Play app and add server-verified Play Integrity device/app/access-risk verdicts before accepting high-risk online actions
