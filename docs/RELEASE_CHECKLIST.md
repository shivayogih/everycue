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
- Renewal Capture tests: labeled and heading-based fields, type inference, no-candidate handling, and mandatory review state
- Phase 1 attachment tests: MIME allowlist, 20 MiB bound, filename sanitization, and owned-path enforcement
- Backup archive tests: attachment byte round-trip, manifest compatibility, checksum/size validation, safe paths, duplicate rejection, and failed-restore cleanup
- Room 1→2→3 migrations retain Track/Renew data, add an optional encrypted barcode, preserve outcome category evidence, and add local coaching preferences
- Use Next tests: expired-item exclusion, deterministic ordering, stable ties, and structured reason codes
- Waste Coach tests: evidence thresholds, 30/90/365-day summaries, rising trends, deterministic reasons, and hide/dismiss filtering
- Trip Ready tests: missing dates, packing state, before/during/after trip boundaries, stable ordering, invalid counts, and 500 linked records
- Trip Ready links: exact Track/Renew identity, duplicate rejection, missing-trip rejection, and immediate optimistic link/unlink state

## Manual device gate

- Test light/dark/system theme and dynamic colors
- Deny and grant Android 13+ notification permission
- Verify daily reminder tap opens the matching Track or Renew detail
- Export a backup containing renewal images/documents, modify data, restore, and verify all domains/history/settings plus the exact attachment bytes and share/view behavior
- Attempt a corrupted/truncated archive restore and confirm current records and attachment files remain unchanged
- Import a legacy JSON backup with and without current renewal attachments; confirm safe imports preserve attachments and conflicting imports fail with a clear message
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
- Test Renewal Capture with camera and imported images for documents, insurance, warranties, memberships, subscriptions, and certificates; verify fields and dates remain editable, review is required, manual fallback works, and rotation does not reapply OCR over user edits
- Verify Use Next never displays an expired item, explains every ranking, preserves category filters across rotation, and opens the correct item detail
- Verify Waste Coach stays silent below thresholds, never estimates money without price data, updates after outcomes, and persists dismiss/mute/restore controls after restart
- Verify Trip Ready groups Critical / Needs attention / Ready, uses only explicitly linked records, recalculates after trip/packing/date changes, and opens the correct Track or Renew detail
- Link and unlink Trip Ready records, rotate and restart the app, and confirm selection state persists without stale or broken findings after source deletion
- Exercise Trip Ready search, linking, scrolling, and recalculation with more than 100 available records

## Play Console gate

- Use a protected upload key; never commit signing material
- Complete Data safety using `PRIVACY_POLICY.md` as the product baseline
- Add store listing graphics/screenshots and support contact
- Upload the signed AAB to internal testing, then closed testing
- Review Android vitals, pre-launch report, and accessibility report before production
- Compare signed AAB and Play Console download-size estimates with the recorded release baseline; investigate growth before promotion
- Register the Play app and add server-verified Play Integrity device/app/access-risk verdicts before accepting high-risk online actions
