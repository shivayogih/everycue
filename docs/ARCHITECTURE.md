# EveryCue architecture

EveryCue uses clean dependency boundaries with an MVVM + MVI presentation flow.

## Dependency direction

```text
Compose UI -> Intent -> ViewModel -> domain store interface -> repository -> Room/DataStore
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

## State rules

1. A screen's durable rendering input comes from its feature `UiState`.
2. User actions enter through `onIntent`; public mutation methods are not exposed alongside it.
3. Navigation and snackbars are effects, not durable state, so rotation does not replay them.
4. Repositories expose `Flow` for observed data and suspend functions for commands.
5. Android framework dependencies do not cross into feature ViewModels or domain contracts.

## Offline and data ownership

Track and Renew use Room; Pack, Settings, and the local profile use Preferences DataStore. Daily reminders use unique periodic WorkManager work. Manual backups are versioned JSON selected through Android's Storage Access Framework. The first-run profile is local identity data, not server authentication. No account, password, network client, ads, or analytics SDK is present.

