# GIF Split — API Documentation

---

## `GifSplitActivity` *(View Layer)*

Pure View. Observes `GifSplitViewModel` and delegates all user actions to it.

### Entry Point

```kotlin
GifSplitActivity.start(context: Context, gifPath: String)
```

| Parameter | Type | Description |
|---|---|---|
| `context` | `Context` | Calling context used to start the Activity. |
| `gifPath` | `String` | Absolute file-system path to the source GIF. |

### Intent Extras

| Key | Type | Description |
|---|---|---|
| `MyConstants.EXTRA_GIF_PATH` | `String` | Absolute path to the source GIF. |

### Internal Methods

| Method | Description |
|---|---|
| `render(UiState)` | Projects ViewModel state onto the View. Guarded by `framesSectionInitialized` to avoid re-initialization. |
| `handle(Event)` | Handles one-shot events: shows save toast/flash for `SaveSuccess`. |
| `setControlsEnabled(Boolean)` | Toggles Save, Slider, and ± buttons as a group. |

---

## `GifSplitViewModel` *(ViewModel Layer)*

Owns UI state and orchestrates frame loading and saving via `GifSplitRepository`.

### Construction

```kotlin
GifSplitViewModel.factory(gifPath: String): ViewModelProvider.Factory
```

Creates a factory that instantiates `GifSplitViewModel` with a default `GifSplitRepository`. Use this with `ViewModelProvider(activity, factory)`.

### Exposed State

```kotlin
val uiState: StateFlow<UiState>   // current screen state
val events: SharedFlow<Event>      // one-shot UI events
```

### `UiState` (sealed class, nested)

| State | Fields | Meaning |
|---|---|---|
| `Loading` | — | Frame extraction in progress. |
| `FramesReady` | `frames: List<Bitmap>`, `isSaving: Boolean` | Frames ready to display. `isSaving=true` while a save is in progress. |
| `Error` | — | FFmpeg produced no output. Activity should finish. |

### `Event` (sealed class, nested)

| Event | Meaning |
|---|---|
| `SaveSuccess` | Frame saved to gallery. Show toast and flash overlay. |

### Public Methods

| Method | Description |
|---|---|
| `saveFrame(frameIndex: Int)` | Saves the frame at `frameIndex` (1-based, matching slider value). No-op if state is not `FramesReady`. |

---

## `GifSplitRepository` *(Data Layer)*

Handles all file I/O and FFmpeg operations. All suspend functions dispatch on `ioDispatcher`.

### Constructor

```kotlin
GifSplitRepository(ioDispatcher: CoroutineDispatcher = Dispatchers.IO)
```

| Parameter | Default | Description |
|---|---|---|
| `ioDispatcher` | `Dispatchers.IO` | Dispatcher for all I/O work. Inject `UnconfinedTestDispatcher` in tests. |

### Methods

#### `suspend fun extractFrames(gifPath: String): List<Bitmap>?`

Extracts all frames from the GIF at `gifPath` into individual PNGs under `OUTPUT_SPLIT_DIR`, then decodes and returns them as a list.

| Returns | Condition |
|---|---|
| `List<Bitmap>` | Extraction succeeded; list is ordered by frame index (1-based). |
| `null` | `OUTPUT_SPLIT_DIR` is empty after FFmpeg execution (corrupt/unsupported GIF). |

#### `suspend fun saveFrame(gifPath: String, frameIndex: Int)`

Copies the PNG at `OUTPUT_SPLIT_DIR/<frameIndex>.png` to a new gallery entry.

| Parameter | Description |
|---|---|
| `gifPath` | Used as the filename prefix for the saved gallery entry. |
| `frameIndex` | 1-based index of the frame to save. |

#### `fun cleanup()`

Wipes and recreates `OUTPUT_SPLIT_DIR`. Called by `GifSplitViewModel.onCleared()` on Activity finish.
