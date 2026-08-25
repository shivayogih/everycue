# EveryCue source validation

## Inputs reviewed

- Live GitHub repository: `shivayogih/everycue`
- `FreshCue_Product_Architecture_PlayStore_Master_Plan(1).docx`
- `PackMate_Product_PlayStore_Master_Plan(1).docx`
- Two attached PackMate Navigation 3 source ZIPs
- `Scan App mobile design.pdf`

## Repository status at review time

The GitHub repository existed and was public, but contained no project files. No existing EveryCue implementation could therefore be validated from the repository itself.

## Attached Pack source

The two Pack source ZIPs were byte-identical. The project contained a useful standalone Pack implementation:

- Trip CRUD foundation
- Packing item add/toggle/delete
- Templates
- Preferences DataStore persistence
- Material 3 and dark/dynamic theme support
- Navigation 3 multiple stacks and dialog destinations
- GitHub Actions baseline

### Corrections made in this foundation

- Changed application identity to `com.everycue.app`.
- Migrated Pack into a feature module under the EveryCue top-level navigation.
- Removed Pack's own Settings bottom tab; EveryCue owns one global Settings destination.
- Added a Templates action inside Pack.
- Replaced timestamp-derived IDs with random positive `Long` IDs.
- Added a local JSON backup key and decode fallback.
- Changed CI to run on `main`, `dev`, and pull requests with the checked-in Gradle wrapper.
- Added Track and Renew feature modules plus shared Room persistence.
- Made start-root back return control to the Activity instead of becoming a silent no-op.

### Remaining Pack technical debt

- Preferences DataStore stores the entire Pack graph as JSON. This is acceptable for a small offline list, but Room should be considered if search, large-list queries, migrations, custom templates, and relational history expand.
- User-authored templates and sharing remain roadmap work.

All current user-visible Pack, Track, Renew, onboarding, profile, settings, validation, sharing, template, notification, and security copy is sourced from Android string resources. Technical identifiers, serialized enum names, file MIME types, date patterns, and internal exception diagnostics remain code constants where localization does not apply.

## Track validation

The Track plan is coherent and supports a real consumer utility. Its strongest decisions are:

- Offline-first and no account/backend for V1
- Local-calendar expiry calculations
- Active inventory plus consumed/discarded/donated history
- Search, filters, categories, storage location, reminders, and insights
- Typed Navigation 3 keys and notification deep links

The current offline Phase 1 candidate adds notifications, deep links, aggregate local insights, attachment-safe backup/restore, widgets, Smart Add, Use Next, Waste Coach, Renewal Capture, and Trip Ready. Custom categories/locations and advanced time-series charts remain later milestones.

## Renew working specification

No separate Renew requirements or source were supplied. The foundation therefore uses this narrow V1 definition:

- Track a title, type, due date, warning window, issuer/provider, reference number, and notes.
- Classify items as upcoming, due soon, due today, or overdue using local calendar dates.
- Mark an item renewed with a new due date and retain previous/new dates in history.
- Support documents, insurance, warranties, memberships, subscriptions, certificates, and other dates.
- Keep all data on device in the first release.

This avoids expanding Renew into finance, payments, automatic subscription detection, or document storage.

## Verified build

The full Android project was validated locally with JDK 21, Android SDK Platform 37, AGP 9.3.2, Kotlin 2.4.10, Compose BOM 2026.08.00, and the checked-in Gradle 9.5.0 wrapper. The following gate completed successfully:

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug assembleRelease bundleRelease --no-daemon
```

This covers unit checks (including the release device-security evaluator and deterministic Trip Ready rules across 500 linked records), Android lint for every module, Room schema generation, debug APK assembly, and minified unsigned release APK/AAB packaging. GitHub Actions repeats the same gate on pull requests and protected integration branches, rejects a release APK or AAB above 12 MiB, and verifies that the merged release manifest disables backup and cleartext traffic and contains no app-level internet or network-state permission.

Trip Ready was exercised on an Android emulator with an actual Pack trip and Renew record. The flow retained its explicit link through rotation and a process restart, recalculated after the trip date changed, and opened the correct owning Renewal detail. The UI uses lazy keyed lists and set-based filtering for large record collections.

The production privacy/security qualification also verified the following on an Android 36 Google Play emulator:

- The installed debug package declares neither `android.permission.INTERNET` nor `android.permission.ACCESS_NETWORK_STATE`.
- Renewal Capture still processed the local OCR fixture and proposed the expected title, insurance type, start date, and due date through the Google Play services text-recognition module.
- A locally debug-signed copy of the minified release APK stopped at the device-protection screen and did not expose organizer or profile UI on the emulator.
- The merged release manifest has `allowBackup="false"`, `usesCleartextTraffic="false"`, a non-exported FileProvider, and no app-level internet or network-state permission.

The debug signature used for the emulator-only release check is not a production signing identity and must never be distributed. Rooted physical-device coverage, first-install model delivery, Play Integrity integration, and Play Console Data safety review remain release-candidate gates.

Final unsigned artifacts from the validated source state:

| Artifact | Bytes | SHA-256 |
|---|---:|---|
| Debug APK | 25,925,857 | `ABC2A375E59686BBE268B104FCB5A2FD78A15785EBE936AAB9A85AD880FA90BF` |
| Minified release APK | 4,097,041 | `7B12F28AA5F4ED57DD65043890E5108BAF03331876FFBC0E388E1B4A70AF7D1D` |
| Minified release AAB | 7,958,293 | `F1423E5C03A567C72638CE9F1651830A28C19935060F9007824CC421CBD5507A` |

The release APK is 3.91 MiB and the AAB is 7.59 MiB, both below the enforced 12 MiB ceiling. Google Play's device-specific download is expected to be smaller than the universal bundle upload because the Play delivery pipeline serves split APKs.
