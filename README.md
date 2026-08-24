# EveryCue

**Track it. Pack it. Renew it.**

EveryCue is an offline-first Android utility that combines three focused tools in one application:

- **Track** — household inventory, expiry status, use/discard/donate outcomes, and basic usage insights.
- **Pack** — trips, packing checklists, progress, and reusable travel templates.
- **Renew** — passports, licences, insurance, warranties, memberships, subscriptions, certificates, and other due dates.

The project is structured as a real multi-feature Jetpack Compose application and as a focused Navigation 3 practice codebase. The consumer product—not Navigation 3—is the user-facing value proposition.

## Current milestone: 0.2.0 offline release candidate

Implemented in this source package:

| Area | Status |
|---|---|
| App identity | `EveryCue`, application ID `com.everycue.app` |
| Top-level navigation | Independent retained stacks for Track, Pack, Renew, and Settings |
| Track | Add/edit/detail/delete, expiry classification, search/filter, outcomes, history, and local insights |
| Pack | Create/edit/delete/search trips, reorder/search/toggle items, progress, templates, local persistence |
| Renew | Add/edit/detail/delete, due classification, history, search/filter, and local insights |
| Persistence | Room for Track/Renew; Preferences DataStore for Pack |
| Architecture | Clean repository boundaries with MVVM + MVI state, intents, and effects |
| First run | Four-page illustrated tutorial followed by local profile setup |
| Reminders | WorkManager daily scheduling, Android 13 permission flow, notification deep links |
| Portability | Versioned JSON export/restore through Android's document picker |
| Local identity | Editable name, phone, email, five-line address, pincode, and opt-in text sharing |
| Widget | Home-screen summary for urgent, unpacked, and due counts |
| Privacy posture | Offline-first, no login, backend, ads, analytics, or cloud sync |
| Unit checks | Expiry/due boundaries, insights, backup format, reminder schedule, and packing progress |

Not yet implemented:

- Custom categories/locations and user-authored packing templates
- Time-series charts (current insights are local, aggregate, and explainable)
- Cloud sync, family sharing, billing, and analytics SDKs
- Scan, Vault, and Lists

## Module structure

```text
:app
:core:navigation
:core:designsystem
:core:database
:feature:track
:feature:pack
:feature:renew
```

Each feature uses a domain-facing store interface, a persistence implementation, immutable UI state, explicit intents, one-time effects, a ViewModel, screens, and typed routes. See `docs/ARCHITECTURE.md`.

## Technology baseline

- Kotlin 2.4.10
- Jetpack Compose + Material 3
- Navigation 3 1.1.6
- Room 2.8.4
- Preferences DataStore 1.2.1
- Coroutines, Flow, and StateFlow
- AGP 9.3.2 / Gradle 9.5.0
- minSdk 23 / compileSdk and targetSdk 36
- JDK 21

## Build

The project includes the standard Gradle 9.5.0 wrapper and a GitHub Actions gate that uploads the debug APK and unsigned release AAB.

Local options:

1. Install JDK 21 and Android SDK Platform 36, or open the project in a compatible Android Studio.
2. Run the checked-in wrapper:

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug bundleRelease
```

The wrapper-generation helper under `scripts/` is only needed when intentionally regenerating wrapper files.

## Release sequence

1. **0.1 Foundation** — multi-feature local data and Navigation 3 vertical slices.
2. **0.2 Offline RC** — clean MVVM/MVI flow, settings, reminders, deep links, backup/restore, insights, widgets, and release build.
3. **0.3 Product polish** — localization, custom taxonomies/templates, richer charts, accessibility/device QA, and signed closed testing.
4. **Future epics** — cloud/family/billing and Scan/Vault/Lists only after separate product, privacy, and backend specifications.

## Source lineage

The Pack feature was migrated from the attached PackMate Navigation 3 project. Track was built from the attached expiry-tracker product plan. Renew is a narrow first specification created for this EveryCue foundation because no separate Renew requirements/code were supplied.

See `docs/VALIDATION.md` and `docs/SCAN_DESIGN_REVIEW.md`.

