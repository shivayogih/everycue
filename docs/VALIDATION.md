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
- User-authored templates and sharing remain roadmap work.

All current user-visible Pack, Track, Renew, onboarding, profile, settings, validation, sharing, template, notification, and security copy is sourced from Android string resources. Technical identifiers, serialized enum names, file MIME types, date patterns, and internal exception diagnostics remain code constants where localization does not apply.

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

The full Android project was validated locally with JDK 21, Android SDK Platform 37, AGP 9.3.2, Kotlin 2.4.10, Compose BOM 2026.08.00, and the checked-in Gradle 9.5.0 wrapper. The following gate completed successfully:

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug assembleRelease bundleRelease --no-daemon
```

This covers unit checks (including the release device-security evaluator), Android lint for every module, Room schema generation, debug APK assembly, and minified unsigned release APK/AAB packaging. GitHub Actions repeats the same gate on pull requests and pushes to `main`, and rejects a release APK or AAB above 12 MiB.

Final unsigned artifacts from the validated source state:

| Artifact | Bytes | SHA-256 |
|---|---:|---|
| Debug APK | 25,747,425 | `D3E308D43E6432FDB0406BC9733AE65140732C2DA9387EFB92DD13C9B8AB2A77` |
| Minified release APK | 3,164,602 | `A52FC6124759612F844E27A250CCC73612FA9004591210F1F4713B02A368088C` |
| Minified release AAB | 6,299,880 | `752FB8CD8E68287B494356F1B81428A50D72BB0386D5514D512E264BAE2C7D24` |

Compared with the pre-optimization validation artifacts, the debug APK is 15.4% smaller and the AAB is 52.9% smaller. Google Play's device-specific download is expected to be smaller than the universal bundle upload because the Play delivery pipeline serves split APKs.
