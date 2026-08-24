# Release checklist

## Automated gate

- JDK 21, Android API 36, checked-in Gradle wrapper
- `testDebugUnitTest`
- `lintDebug`
- `assembleDebug`
- `bundleRelease` with minification and resource shrinking
- CI artifacts: debug APK and unsigned release AAB

## Manual device gate

- Test light/dark/system theme and dynamic colors
- Deny and grant Android 13+ notification permission
- Verify daily reminder tap opens the matching Track or Renew detail
- Export a backup, modify data, restore, and verify all domains/history/settings
- Add and resize the widget; verify counts update after mutations
- Exercise TalkBack labels, large font, landscape, and smallest supported screen
- Test fresh install and upgrade from 0.1.0 without clearing app data

## Play Console gate

- Use a protected upload key; never commit signing material
- Complete Data safety using `PRIVACY_POLICY.md` as the product baseline
- Add store listing graphics/screenshots and support contact
- Upload the signed AAB to internal testing, then closed testing
- Review Android vitals, pre-launch report, and accessibility report before production

