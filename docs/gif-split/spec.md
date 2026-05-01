# GIF Split — Specification

## Overview

`GifSplitActivity` allows the user to extract a single frame from a GIF and save it as a PNG to the gallery.

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
| F8 | Clean up the temporary split output directory on both launch and destroy. |

## Non-Functional Requirements

| # | Requirement |
|---|-------------|
| NF1 | All file I/O and FFmpeg processing must execute off the main (UI) thread to avoid ANR. |
| NF2 | All View mutations must execute on the main thread. |
| NF3 | Controls (Save, Slider, ±) must be disabled while a background operation is in progress to prevent race conditions. |
| NF4 | Business logic and UX flow must remain identical to the original synchronous implementation. |

## Threading Model

```
Main Thread                    IO Thread (Dispatchers.IO)
──────────────────────         ─────────────────────────────────────────
setContentView / wire listeners
disable controls
launch coroutine ──────────────► resetDirectory(OUTPUT_SPLIT_DIR)
                                 FFmpegKit.execute(...)
                                 File.listFiles() → frameCount
                                 BitmapFactory.decodeFile × N → frames
                ◄────────────── return frames (or null)
show error + finish  ◄── null
setup slider, show first frame
enable controls
[user taps Save]
disable controls
launch coroutine ──────────────► copyFile(frame, createNewFile(...))
                ◄────────────── done
show toast + flash
enable controls
```
