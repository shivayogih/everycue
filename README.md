# EveryCue

**Track it. Pack it. Renew it.**

EveryCue is an offline-first Android utility that combines three focused tools in one application:

- **Track** — household inventory, expiry status, use/discard/donate outcomes, and basic usage insights.
- **Pack** — trips, packing checklists, progress, and reusable travel templates.
- **Renew** — passports, licences, insurance, warranties, memberships, subscriptions, certificates, and other due dates.

The project is structured as a real multi-feature Jetpack Compose application and as a focused Navigation 3 practice codebase. The consumer product—not Navigation 3—is the user-facing value proposition.

## Current milestone: 0.1.0 foundation

Implemented in this source package:

| Area | Status |
|---|---|
| App identity | `EveryCue`, application ID `com.everycue.app` |
| Top-level navigation | Independent retained stacks for Track, Pack, Renew, and Settings |
| Track | Add/edit/detail/delete, expiry classification, inventory search/category filter, outcomes, history, basic insights |
| Pack | Create/delete trips, add/delete/toggle items, progress, templates, local persistence |
| Renew | Add/edit/detail/delete, due classification, mark renewed, search/type filter, renewal history |
| Persistence | Room for Track/Renew; Preferences DataStore for Pack |
| Privacy posture | Offline-first, no login, backend, ads, analytics, or cloud sync |
| Unit checks | Expiry boundaries, renewal boundaries, and packing progress |

Not yet implemented:

- WorkManager reminders and notification deep links
- Global settings persistence and category/location customization
- Advanced analytics and charts
- Import/export, widgets, cloud sync, family sharing, billing, and analytics SDKs
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

The source keeps feature routes, screens, repositories, and entry builders together for the first vertical slice. When the route surface grows, each feature can be split into `api` and `impl` submodules without changing the product model.

## Technology baseline

- Kotlin 2.4.10
- Jetpack Compose + Material 3
- Navigation 3 1.1.6
- Room 2.8.4
- Preferences DataStore 1.2.1
- Coroutines, Flow, and StateFlow
- AGP 9.3.2 / Gradle 9.5.0
- minSdk 23 / compileSdk and targetSdk 37
- JDK 21

## Build

This foundation includes the standard Gradle 9.5.0 wrapper and a GitHub Actions build that runs the project checks and uploads the debug APK.

Local options:

1. Install JDK 21 and Android SDK Platform 37, or open the project in a compatible Android Studio.
2. Run the checked-in wrapper:

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug
```

The wrapper-generation helper under `scripts/` is only needed when intentionally regenerating wrapper files.

## Release sequence

1. **0.1 Foundation** — current modules, local data, Navigation 3, functional vertical slices.
2. **0.2 Quality pass** — compile/CI fixes, string resources, error states, accessibility, UI tests, Room migration tests.
3. **0.3 Reminders** — notification permission, WorkManager scheduling, reminder settings, notification deep links.
4. **0.4 Insights** — time-based consumption/waste reports and renewal activity summaries.
5. **0.9 Release candidate** — privacy policy, Play assets, signed AAB, closed testing, accessibility/device QA.

## Source lineage

The Pack feature was migrated from the attached PackMate Navigation 3 project. Track was built from the attached expiry-tracker product plan. Renew is a narrow first specification created for this EveryCue foundation because no separate Renew requirements/code were supplied.

See `docs/VALIDATION.md` and `docs/SCAN_DESIGN_REVIEW.md`.
