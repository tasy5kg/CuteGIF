# GIF Split — API Documentation

## `GifSplitActivity`

An `Activity` that extracts frames from a GIF file and allows the user to save a selected frame as a PNG.

### Entry Point

```kotlin
GifSplitActivity.start(context: Context, gifPath: String)
```

| Parameter | Type | Description |
|---|---|---|
| `context` | `Context` | The calling context used to start the Activity. |
| `gifPath` | `String` | Absolute file-system path to the source GIF. |

### Internal API

#### `onCreateIfEulaAccepted(savedInstanceState: Bundle?)`

Called by `BaseActivity.onCreate` after EULA acceptance is confirmed.

**Flow:**
1. Sets up close and slider ± click listeners synchronously.
2. Disables all interactive controls.
3. Launches a coroutine on `lifecycleScope`:
   - Runs frame extraction pipeline on `Dispatchers.IO`.
   - On success, populates the UI and registers the Save click handler on the main thread.
   - On failure (`frameCount == null`), shows an error toast and finishes the Activity.

#### `setControlsEnabled(enabled: Boolean)` *(private)*

Enables or disables the Save button, slider, and slider ± buttons as a group.

| Parameter | Type | Description |
|---|---|---|
| `enabled` | `Boolean` | `true` to enable all controls; `false` to disable them. |

#### `onDestroy()`

Cleans up the temporary split output directory (`OUTPUT_SPLIT_DIR`) on Activity destruction.

### Intent Extras

| Extra key | Type | Description |
|---|---|---|
| `MyConstants.EXTRA_GIF_PATH` | `String` | Absolute path to the source GIF file. |

### Side Effects

| Operation | Thread | Description |
|---|---|---|
| `resetDirectory(OUTPUT_SPLIT_DIR)` | IO | Wipes and recreates the temp frame directory on start and destroy. |
| `FFmpegKit.execute(...)` | IO | Decodes GIF into numbered PNG frames in `OUTPUT_SPLIT_DIR`. |
| `BitmapFactory.decodeFile(...)` | IO | Loads each frame PNG into memory as a `Bitmap`. |
| `copyFile(src, dest)` | IO | Copies the selected frame PNG to the gallery URI. |
| `createNewFile(prefix, "png")` | IO | Inserts a new MediaStore entry and returns its `Uri`. |
