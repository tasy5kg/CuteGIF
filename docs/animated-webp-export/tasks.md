# Animated WebP Export — Task Breakdown

**Spec:** [spec.md](spec.md)
**Last updated:** 2026-05-02

---

## How to use this file

- Check off each task `[x]` as it is completed.
- When **every task** in a User Story section is checked, update that story's **Status** badge to `✅ Complete`.
- Status values: `⬜ Not Started` · `🔄 In Progress` · `✅ Complete`

---

## US-1 — Format Selection

**Spec ref:** [spec.md § US-1](spec.md#us-1--format-selection)
**Status:** ✅ Complete

### Data Model

- [x] **T1.1** Define `OutputFormat` enum with values `GIF` and `ANIMATED_WEBP`.
- [x] **T1.2** Add `outputFormat: OutputFormat` field to `TaskBuilderVideoToGif` with default value `GIF`.

### UI Layout

- [x] **T1.3** Add `MaterialButtonToggleGroup` (buttons: **GIF** | **WebP**) at the top of `dialog_fragment_video_to_gif_export_options.xml`, above resolution controls.

### Logic & Wiring

- [x] **T1.4** Add `outputFormat: OutputFormat` to the export options ViewModel state (default: `GIF`).
- [x] **T1.5** Wire the format toggle's selection callback to update `outputFormat` in the ViewModel.
- [x] **T1.6** Observe `outputFormat` in the dialog fragment: hide GIF-specific controls (Color Quality, Image Quality) and show the WebP quality section when `ANIMATED_WEBP` is selected.
- [x] **T1.7** Observe `outputFormat` in the dialog fragment: show GIF-specific controls and hide the WebP quality section when `GIF` is selected (initial/default state).

### Tests

- [x] **T1.8** Unit test — `outputFormat` defaults to `GIF` on ViewModel creation.
- [x] **T1.9** Unit test — setting `outputFormat` to `ANIMATED_WEBP` transitions state so that WebP quality is visible and GIF controls are hidden.
- [x] **T1.10** Unit test — setting `outputFormat` back to `GIF` transitions state so that GIF controls are visible and WebP quality is hidden.

---

## US-2 — WebP Quality Presets

**Spec ref:** [spec.md § US-2](spec.md#us-2--webp-quality-presets)
**Status:** ✅ Complete

### Data Model

- [x] **T2.1** Define `WebpQuality` enum with fields `ffmpegQuality: Int?` and `lossless: Boolean`, and four values: `SMALL(50, false)`, `MEDIUM(75, false)`, `HIGH(90, false)`, `LOSSLESS(null, true)`.
- [x] **T2.2** Add `webpQuality: WebpQuality?` field to `TaskBuilderVideoToGif` — `null` when `outputFormat == GIF`, `MEDIUM` when `outputFormat == ANIMATED_WEBP`.

### UI Layout

- [x] **T2.3** Add WebP quality section to `dialog_fragment_video_to_gif_export_options.xml`: a "WebP Quality" label and a `MaterialButtonToggleGroup` with buttons **Small** | **Medium** | **High** | **Lossless**. Section is hidden by default (shown only when WebP is selected — see T1.6/T1.7).
- [x] **T2.4** Add a lossless warning `TextView` ("Lossless files may be larger than GIF.") below the quality toggle in the layout. Hidden by default.

### Logic & Wiring

- [x] **T2.5** Add `webpQuality: WebpQuality` to the export options ViewModel state with default `MEDIUM`.
- [x] **T2.6** Wire the quality preset toggle's selection callback to update `webpQuality` in the ViewModel.
- [x] **T2.7** When `outputFormat` switches to `ANIMATED_WEBP`, set `webpQuality` to `MEDIUM` and reflect the default selection in the toggle UI.
- [x] **T2.8** Observe `webpQuality` in the dialog fragment: show the lossless warning text when `LOSSLESS` is selected; hide it for all other presets.

### Tests

- [x] **T2.9** Unit test — `SMALL` preset: `ffmpegQuality == 50`, `lossless == false`.
- [x] **T2.10** Unit test — `MEDIUM` preset: `ffmpegQuality == 75`, `lossless == false`.
- [x] **T2.11** Unit test — `HIGH` preset: `ffmpegQuality == 90`, `lossless == false`.
- [x] **T2.12** Unit test — `LOSSLESS` preset: `ffmpegQuality == null`, `lossless == true`.
- [x] **T2.13** Unit test — `webpQuality` defaults to `MEDIUM` when `outputFormat` switches to `ANIMATED_WEBP`.
- [x] **T2.14** Unit test — ViewModel state exposes lossless warning as visible only when `webpQuality == LOSSLESS`.

---

## US-3 — File Size Comparison

**Spec ref:** [spec.md § US-3](spec.md#us-3--file-size-comparison)
**Status:** ✅ Complete

### Data Model

- [x] **T3.1** Define `EstimationState` sealed class: `Loading`, `Ready(gifSizeBytes: Long, webpSizeBytes: Long)`, `Error`.
- [x] **T3.2** Implement `formatEstimatedSize(bytes: Long): String` helper: returns `~X KB` when `bytes < 1_048_576`, otherwise `~X.X MB` (1 decimal place).

### Core Logic

- [x] **T3.3** Implement `FileSizeEstimator`: takes current export settings, encodes a GIF and WebP sample using `min(duration, 1000ms)` of the clip at current settings into a temp directory, measures both output file sizes, and extrapolates each: `estimatedSize = sampleSize × (fullDuration / sampleDuration)`.
- [x] **T3.4** Implement temp file cleanup in `FileSizeEstimator`: delete temp directory after estimation completes (success or error).

### ViewModel

- [x] **T3.5** Add `estimationState: StateFlow<EstimationState>` to the export options ViewModel (initial value: `Loading`).
- [x] **T3.6** Implement estimation trigger in ViewModel: launch estimation coroutine on `Dispatchers.IO` using `FileSizeEstimator`; update `estimationState` with the result.
- [x] **T3.7** Apply 300ms debounce to the estimation trigger — cancel any in-progress estimation job before launching a new one.
- [x] **T3.8** Re-trigger estimation whenever `outputFormat`, `webpQuality`, resolution, or FPS changes in ViewModel state.
- [x] **T3.9** Cancel in-progress estimation job and clean up temp files when the dialog is dismissed (ViewModel cleared or explicit cancel hook).

### UI Layout

- [x] **T3.10** Add a size estimate row to `dialog_fragment_video_to_gif_export_options.xml`: flat `ConstraintLayout` with `CircularProgressIndicator` (loading) and two `TextView`s for GIF/WebP sizes (ready/error).

### UI Wiring

- [x] **T3.11** Observe `estimationState` in the dialog fragment: show `CircularProgressIndicator` on `Loading`; show formatted size strings on `Ready`; show a fallback label (e.g. "—") on `Error`.
- [x] **T3.12** Visually highlight the size label of the currently selected format (driven by `outputFormat` state).

### Tests

- [x] **T3.13** Unit test — `formatEstimatedSize` with value below 1 MB returns `~X KB` format.
- [x] **T3.14** Unit test — `formatEstimatedSize` with value ≥ 1 MB returns `~X.X MB` format.
- [x] **T3.15** Unit test — `formatEstimatedSize` output always starts with `~`.
- [x] **T3.16** Unit test — extrapolation formula: given `sampleSize`, `sampleDuration`, `fullDuration`, returns `sampleSize × (fullDuration / sampleDuration)`.
- [x] **T3.17** Unit test — ViewModel transitions `estimationState` from `Loading` to `Ready` on successful estimation.
- [x] **T3.18** Unit test — ViewModel transitions `estimationState` from `Loading` to `Error` when `FileSizeEstimator` throws.
- [x] **T3.19** Unit test — changing a setting within 300ms cancels the previous estimation job and does not emit a stale `Ready` result.

---

## US-4 — Export and Save

**Spec ref:** [spec.md § US-4](spec.md#us-4--export-and-save)
**Status:** ✅ Complete

### FFmpeg Command

- [x] **T4.1** Implement `getCommandVideoToWebp()` in `TaskBuilderVideoToGif`: builds the FFmpeg command using `framerate`, `framesPath`, and `WebpQuality` — emitting `-quality {q} -compression_level 6` for lossy presets and `-lossless 1 -compression_level 6` for `LOSSLESS` — with `-loop 0 -y {output}.webp`.

### Export Pipeline

- [x] **T4.2** Update the export execution path in `VideoToGifActivity` (or its ViewModel): when `outputFormat == ANIMATED_WEBP`, call `getCommandExtractFrame()` then `getCommandVideoToWebp()`, skipping the palette and GIF encoding steps.
- [x] **T4.3** When `outputFormat == ANIMATED_WEBP`, set the output file extension to `.webp`.

### File Saving

- [x] **T4.4** Update `FileTools.createNewFile()` (or its call site) to handle the `"webp"` extension: insert the MediaStore entry with `image/webp` MIME type and save to the same output directory as GIF exports.

### FileSavedActivity

- [x] **T4.5** Pass `outputFormat` to `FileSavedActivity` via Intent extra.
- [x] **T4.6** Update `FileSavedActivity` to display **"Animated WebP saved"** when `outputFormat == ANIMATED_WEBP`, and the existing **"GIF saved"** label otherwise.
- [x] **T4.7** Update the share Intent in `FileSavedActivity` to use MIME type `image/webp` when `outputFormat == ANIMATED_WEBP`, and `image/gif` otherwise.

### Tests

- [x] **T4.8** Unit test — `getCommandVideoToWebp()` with `SMALL` preset contains `-quality 50 -compression_level 6`.
- [x] **T4.9** Unit test — `getCommandVideoToWebp()` with `MEDIUM` preset contains `-quality 75 -compression_level 6`.
- [x] **T4.10** Unit test — `getCommandVideoToWebp()` with `HIGH` preset contains `-quality 90 -compression_level 6`.
- [x] **T4.11** Unit test — `getCommandVideoToWebp()` with `LOSSLESS` preset contains `-lossless 1 -compression_level 6` and does not contain `-quality`.
- [x] **T4.12** Unit test — `getCommandVideoToWebp()` output always ends with `-loop 0 -y {path}.webp`.
- [x] **T4.13** Unit test — export pipeline invokes `getCommandVideoToWebp()` (not GIF commands) when `outputFormat == ANIMATED_WEBP`. *(N/A — Activity-layer code is exempt from TDD per AGENTS.md)*
- [x] **T4.14** Unit test — export pipeline invokes `getCommandCreatePalette()` and `getCommandVideoToGif()` (not WebP command) when `outputFormat == GIF`. *(N/A — Activity-layer code is exempt from TDD per AGENTS.md)*
