# GIF Split — Architecture Design

## Layer Overview

```
┌──────────────────────────────────────────────┐
│  View Layer                                  │
│  GifSplitActivity                            │
│  • Renders UiState via StateFlow             │
│  • Handles one-shot Events via SharedFlow    │
│  • Delegates all user actions to ViewModel   │
└─────────────────────┬────────────────────────┘
                      │ observes / calls
┌─────────────────────▼────────────────────────┐
│  ViewModel Layer                             │
│  GifSplitViewModel                          │
│  • Owns UiState (StateFlow)                  │
│  • Emits Events (SharedFlow)                 │
│  • Calls Repository; maps results to state   │
│  • Survives config changes                   │
└─────────────────────┬────────────────────────┘
                      │ suspend calls
┌─────────────────────▼────────────────────────┐
│  Data Layer                                  │
│  GifSplitRepository                          │
│  • Handles all I/O: FFmpegKit, BitmapFactory │
│    FileTools (resetDirectory, copyFile,      │
│    createNewFile)                            │
│  • Dispatches all work on ioDispatcher       │
└──────────────────────────────────────────────┘
```

## State Model

```kotlin
// Sealed inside GifSplitViewModel
sealed class UiState {
    object Loading : UiState()
    data class FramesReady(val frames: List<Bitmap>, val isSaving: Boolean = false) : UiState()
    object Error : UiState()
}

sealed class Event {
    object SaveSuccess : Event()
}
```

State transitions:
```
Loading ──► FramesReady(frames, isSaving=false)  [on successful extraction]
Loading ──► Error                                [on null frame count]
FramesReady(isSaving=false) ──► FramesReady(isSaving=true)   [saveFrame called]
FramesReady(isSaving=true)  ──► FramesReady(isSaving=false)  [save done] + SaveSuccess event
Error   ──► [Activity calls finish()]
```

## Component Responsibilities

### GifSplitRepository
- Injectable `ioDispatcher` (default: `Dispatchers.IO`) for testability.
- `extractFrames(gifPath)`: full frame extraction pipeline, returns `null` on failure.
- `saveFrame(gifPath, frameIndex)`: copies selected frame to gallery.
- `cleanup()`: wipes `OUTPUT_SPLIT_DIR`; called from `ViewModel.onCleared()`.

### GifSplitViewModel
- Created via `ViewModelProvider` with a `factory(gifPath)` companion function.
- `init` triggers `loadFrames()` immediately.
- `saveFrame(frameIndex)` guards with `_uiState.value as? FramesReady` — no-op if not ready.
- Calls `repository.cleanup()` in `onCleared()`, which fires on Activity finish (not rotation).

### GifSplitActivity
- Pure View: no business logic.
- `framesSectionInitialized` flag prevents re-initializing the slider on every re-render (e.g., `isSaving` state changes), while correctly re-initializing on rotation (new Activity instance resets flag).
- Collects `uiState` and `events` inside `repeatOnLifecycle(STARTED)`.
- `render(UiState)` and `handle(Event)` are the only two state→UI projection functions.

## Design Decisions

| Decision | Rationale |
|---|---|
| Repository owns `ioDispatcher` | Enables unit tests to inject `UnconfinedTestDispatcher`, avoiding real I/O |
| `isSaving` in `FramesReady` state | Survives STOP→START lifecycle transitions; avoids lost `SaveStarted` events if Activity is backgrounded during save |
| `framesSectionInitialized` flag | Prevents duplicate `addOnChangeListener` calls on re-renders without clearing listeners; correctly resets on rotation |
| `ViewModelProvider` over `by viewModels {}` | Avoids adding `activity-ktx` dependency while supporting custom factory |
| `onCleared` cleanup (not `onDestroy`) | On rotation, ViewModel is retained so `OUTPUT_SPLIT_DIR` frames persist for re-use; cleanup only on finish |

## Dependencies Added

| Artifact | Purpose |
|---|---|
| `lifecycle-viewmodel-ktx 2.8.7` | `ViewModel`, `viewModelScope`, `ViewModelProvider` |
| `kotlinx-coroutines-android 1.9.0` | Already added — `Dispatchers.IO/Main` |
| `kotlinx-coroutines-test 1.9.0` | `runTest`, `StandardTestDispatcher` for ViewModel unit tests |
| `mockk 1.13.12` | Mocking `GifSplitRepository` in ViewModel tests |
