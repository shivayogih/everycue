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
- Changed CI to run on `main` and pull requests with the checked-in Gradle wrapper.
- Added Track and Renew feature modules plus shared Room persistence.
- Made start-root back return control to the Activity instead of becoming a silent no-op.

### Remaining Pack technical debt

- Preferences DataStore stores the entire Pack graph as JSON. This is acceptable for a small offline list, but Room should be considered if search, large-list queries, migrations, custom templates, and relational history expand.
- UI text is still mostly hard-coded and must be moved into string resources before localization.
- User-authored templates and sharing remain roadmap work.
- Text extraction to resources is still required before localization begins.

## Track validation

The Track plan is coherent and supports a real consumer utility. Its strongest decisions are:

- Offline-first and no account/backend for V1
- Local-calendar expiry calculations
- Active inventory plus consumed/discarded/donated history
- Search, filters, categories, storage location, reminders, and insights
- Typed Navigation 3 keys and notification deep links

The 0.2 offline release candidate adds notifications, deep links, aggregate local insights, backup/restore, and widgets. Custom categories/locations and advanced time-series charts remain later milestones.

## Renew working specification

No separate Renew requirements or source were supplied. The foundation therefore uses this narrow V1 definition:

- Track a title, type, due date, warning window, issuer/provider, reference number, and notes.
- Classify items as upcoming, due soon, due today, or overdue using local calendar dates.
- Mark an item renewed with a new due date and retain previous/new dates in history.
- Support documents, insurance, warranties, memberships, subscriptions, certificates, and other dates.
- Keep all data on device in the first release.

This avoids expanding Renew into finance, payments, automatic subscription detection, or document storage.

## Verified build

The full Android project was validated locally with Android Studio's JDK 21 runtime, Android SDK Platform 36, AGP 9.3.2, Kotlin 2.4.10, and the checked-in Gradle 9.5.0 wrapper. The following gate completed successfully:

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug bundleRelease --no-daemon
```

This covers unit checks, Android lint for every module, Room schema generation, debug APK assembly, and a minified unsigned release AAB. GitHub Actions repeats the same gate on pull requests and pushes to `main`.

