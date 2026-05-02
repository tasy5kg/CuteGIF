# GIF Split — Specification

## Overview

`GifSplitActivity` lets the user extract a single frame from a GIF and save it as a PNG to the gallery. The screen follows the MVVM pattern: `GifSplitActivity` is the pure View layer, `GifSplitViewModel` owns UI state and orchestrates business logic, and `GifSplitRepository` handles all file I/O and FFmpeg operations.

## Functional Requirements

| # | Requirement |
|---|-------------|
| F1 | Accept an input GIF path via `Intent` extra `EXTRA_GIF_PATH`. |
| F2 | Decode the GIF into individual PNG frames using FFmpeg on launch. |
| F3 | Display the first frame immediately after decoding completes. |
| F4 | For multi-frame GIFs, show a slider that lets the user navigate frames (haptic feedback on change). |
| F5 | For single-frame GIFs, hide the frame selector controls. |
| F6 | Allow the user to save the currently selected frame as a PNG to the gallery. |
| F7 | Show a brief flash overlay after a successful save. |
| F8 | Clean up the temporary split output directory on both launch and ViewModel destruction. |

## Non-Functional Requirements

| # | Requirement |
|---|-------------|
| NF1 | All file I/O and FFmpeg processing must execute off the main (UI) thread to avoid ANR. |
| NF2 | All View mutations must execute on the main thread. |
| NF3 | Controls (Save, Slider, ±) must be disabled while any background operation is in progress. |
| NF4 | Business logic and UX flow must remain identical to the original implementation. |
| NF5 | Frame data must survive device rotation (ViewModel retention) — no re-extraction on config change. |

## Threading Model

```
GifSplitActivity (Main Thread)        GifSplitRepository (IO Thread via ioDispatcher)
──────────────────────────────        ────────────────────────────────────────────────
observe uiState → render UI
[Loading state]
GifSplitViewModel.init ──────────────► extractFrames():
                                         resetDirectory(OUTPUT_SPLIT_DIR)
                                         FFmpegKit.execute(...)
                                         File.listFiles() → frameCount
                                         BitmapFactory.decodeFile × N → frames
                       ◄────────────── return List<Bitmap>? (or null)
[FramesReady / Error state]
render frames, enable controls
[user taps Save]
viewModel.saveFrame(index) ───────────► saveFrame(gifPath, index):
                                         copyFile(frame, createNewFile(...))
                       ◄────────────── done
[isSaving = false state + SaveSuccess event]
toast + flash overlay, enable controls
```
