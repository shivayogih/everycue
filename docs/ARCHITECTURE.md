# EveryCue architecture

EveryCue uses clean dependency boundaries with an MVVM + MVI presentation flow.

## Dependency direction

```text
Compose UI -> Intent -> ViewModel -> domain store interface -> repository -> encryption -> Room/DataStore
                 ^          |
                 |          +-> immutable UiState
                 +------------- one-time Effect (navigation/message)
```

- **UI:** renders immutable state and emits typed intents. It does not read databases, DataStore, files, or WorkManager.
- **Presentation:** each feature ViewModel accepts a sealed intent contract, coordinates work, reduces observable state, and emits buffered one-time effects.
- **Domain boundary:** `TrackStore`, `PackStore`, and `RenewStore` describe persistence-independent operations and models.
- **Data:** repository implementations translate domain models to Room entities or versioned DataStore JSON.
- **App boundary:** dependency construction, WorkManager, notifications, widgets, document URIs, and cross-feature backup orchestration live in `:app`.
- **Navigation:** the app consumes effects and owns navigation, so ViewModels stay independent of Activity and Navigation 3 types.

## Phase 1 local intelligence foundations

- `:core:extraction` owns deterministic, framework-independent parsing contracts. Extracted values carry confidence, source context, and an explicit confirmation flag; extraction never persists a date or changes lifecycle state.
- `:core:attachments` owns bounded copies into app-private storage. It supports the narrow Phase 1 image/PDF/text MIME set, sanitizes metadata, rejects empty or oversized inputs, prevents path traversal, and removes only app-owned copies.
- `:core:vision` wraps permissionless Google Code Scanner and the Google Play services on-device Latin text recognizer. SDK objects stop at this boundary; feature ViewModels receive only barcode strings or OCR text and convert them to structured, reviewable Track or Renew drafts. Renewal Capture can suggest a type, title, provider, reference, start date, and due date from a camera or imported image, but it cannot save until the user reviews the suggestions.
- `:core:recommendation` owns deterministic recommendation policies. Use Next ranks active inventory from confirmed expiry dates, excludes expired items, and exposes a structured reason code for every result. Waste Coach aggregates only local outcome history, requires minimum evidence thresholds, exposes structured evidence/reason/suggestion codes, and supports durable dismiss/mute controls. Trip Ready evaluates packing progress plus only the Track and Renew records a user explicitly links to a trip; it groups structured findings as Critical, Needs attention, or Ready and never presents an expiry comparison as safety or legal advice.
- Remote generative AI is outside Phase 1. Manual Track, Pack, and Renew flows remain the fallback when an on-device model or Google Play services module is unavailable.

## State rules

1. A screen's durable rendering input comes from its feature `UiState`.
2. User actions enter through `onIntent`; public mutation methods are not exposed alongside it.
3. Navigation and snackbars are effects, not durable state, so rotation does not replay them.
4. Repositories expose `Flow` for observed data and suspend functions for commands.
5. Android framework dependencies do not cross into feature ViewModels or domain contracts.

## Offline and data ownership

Track and Renew use Room; Pack, Settings, and the local profile use Preferences DataStore. Sensitive organizer text and the Pack/profile JSON payloads are encrypted before persistence with AES-256-GCM. The non-exportable key is generated and retained by Android Keystore. Existing plaintext development data is read for compatibility and becomes encrypted when it is next saved; non-sensitive display and reminder settings remain plaintext. Daily reminders use unique periodic WorkManager work. Manual backups use a versioned EveryCue ZIP archive selected through Android's Storage Access Framework. Its JSON manifest and attachment files are validated for count, size, MIME type, ownership, safe paths, and SHA-256 integrity before restore mutates application state. Legacy JSON backups remain importable only when doing so cannot orphan current attachment data. The first-run profile is local identity data, not server authentication. No account, password, network client, ads, or analytics SDK is present.

Imported attachments are copied into an EveryCue-owned directory under app-private storage. Stored metadata uses a relative local reference rather than retaining a third-party document URI. Removing an attachment never deletes the user's source document or gallery image. Temporary captures are isolated under app cache and have an explicit cleanup path.

Trip Ready links are part of the encrypted Pack graph. The app boundary maps active Track and Renew records into small persistence-independent inputs; Pack never depends directly on another feature module. The evaluator uses confirmed dates and stable local rules, while navigation back to the owning record remains an app-level concern. Missing or deleted linked records are ignored, so they cannot create broken findings.

## Security boundary

- Release builds fail closed before repository initialization when local signals indicate an emulator, test-key build, known root binary, or Magisk artifact. Debug builds explicitly bypass this gate for emulator-based development and CI.
- Release windows prevent screenshots/non-secure displays, hide non-system overlays on Android 12+, and reject obscured touches. Widgets clear their counts and reminder workers stop before accessing data when the same device gate fails.
- Android OS backup/device transfer is disabled. User-controlled, versioned export through the Storage Access Framework is the supported portability mechanism.
- Sensitive local fields are authenticated and encrypted with AES-256-GCM using an Android Keystore key. Authentication failures fail closed instead of returning corrupted plaintext. User-requested EveryCue archives are intentionally portable and readable outside the app, so users must protect the destination file.
- Cleartext networking is denied by manifest and Network Security Configuration. Debug builds may trust user-installed certificates for local inspection, but cleartext remains disabled.
- Organizer records remain in Android app-private storage. The merged release manifest removes app-level internet and network-state permissions contributed by scanning dependencies; Google Play services owns dynamic scanner/OCR module delivery. CI fails if either permission returns. This local device gate is defense-in-depth and can be bypassed by a sufficiently capable attacker controlling the OS. Before a Play production rollout, use Play Integrity with server-side verdict verification for hardware-backed app/device integrity and app-access-risk decisions.
