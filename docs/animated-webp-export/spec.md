# Animated WebP Export — Feature Specification

**Version:** 1.1
**Date:** 2026-05-02
**Scope:** Video → GIF flow only (first iteration)
**Status:** Final

---

## 1. Overview

Add Animated WebP as an additional export format in the Video → GIF conversion flow. The user can choose between GIF and Animated WebP before exporting. Both formats are produced from the same editing pipeline (trim, crop, speed, text, color key). The UI shows the estimated file size for each format so the user can make an informed choice.

---

## 2. Goals

- Let users export as Animated WebP alongside the existing GIF format.
- Show estimated file size for both GIF and WebP so users understand the trade-off.
- Support both lossy and lossless WebP quality modes.
- Reuse the existing FFmpeg pipeline; add only a new encoding step at the end.

## 3. Non-Goals (this iteration)

- WebP export in GIF Split, GIF → Video, or Motion Photo flows.
- Animated WebP preview in the export options dialog (static single-frame preview is sufficient).
- WebP-specific editing controls (filters, palette options are GIF-only concepts).

---

## 4. User Stories

### US-1 — Format Selection
> As a user, I want to choose between GIF and Animated WebP before exporting, so I can pick the format that best fits my use case.

**Acceptance criteria:**
- The export options dialog shows a format toggle: **GIF** | **WebP**.
- GIF is selected by default.
- Switching format updates the visible quality controls (GIF controls hide, WebP controls show, and vice versa).

---

### US-2 — WebP Quality Presets
> As a user, I want to choose a WebP quality preset, so I can balance file size against visual quality without understanding codec internals.

**Acceptance criteria:**
- Four presets are available: **Small**, **Medium**, **High**, **Lossless**.
- Default preset is **Medium**.
- Selecting **Lossless** disables the lossy quality parameter and uses lossless WebP encoding.
- Selecting **Lossless** shows an inline warning: _"Lossless files may be larger than GIF."_
- Preset labels map to FFmpeg parameters as follows:

| Preset | FFmpeg flags | Notes |
|---|---|---|
| Small | `-quality 50 -compression_level 6` | Lossy, smallest file |
| Medium | `-quality 75 -compression_level 6` | Lossy, balanced (default) |
| High | `-quality 90 -compression_level 6` | Lossy, near-lossless visually |
| Lossless | `-lossless 1 -compression_level 6` | Lossless, largest file |

---

### US-3 — File Size Comparison
> As a user, I want to see the estimated file size for both GIF and WebP, so I can understand the size difference before committing to an export.

**Acceptance criteria:**
- After the user configures export settings, the dialog shows two size estimates side by side:
  - `GIF: ~X MB` (based on current GIF settings)
  - `WebP: ~Y MB` (based on current WebP preset)
- Estimates are computed from a short sample encode (first 1 second of the clip, or the full clip if shorter), then extrapolated to the full duration.
- Estimates are shown as approximate values (prefix `~`).
- Size is displayed in KB if under 1 MB, otherwise in MB (e.g. `~850 KB`, `~2.3 MB`).
- Estimates update when the user changes resolution, FPS, quality, or format settings.
- If estimation is in progress, show a loading indicator in place of the size value.

---

### US-4 — Export and Save
> As a user, when I tap Save with WebP selected, I want the output file saved as `.webp` to the gallery, so I can share it like any other media file.

**Acceptance criteria:**
- Output file extension is `.webp`.
- File is saved to the same output directory as GIF exports.
- The FileSavedActivity screen shows the correct format label ("Animated WebP" instead of "GIF").
- The share intent uses MIME type `image/webp`.

---

## 5. UI Changes

### 5.1 Export Options Dialog (`dialog_fragment_video_to_gif_export_options.xml`)

**New elements:**

1. **Format toggle** (MaterialButtonToggleGroup): `GIF | WebP`
   — Placed at the top of the export options, above resolution controls.

2. **WebP quality section** (shown only when WebP is selected):
   — Label: "WebP Quality"
   — MaterialButtonToggleGroup: `Small | Medium | High | Lossless`
   — Default: Medium

3. **Size estimate row** (always visible):
   — Two chips or text views side by side: `GIF ~X.X MB` and `WebP ~Y.Y MB`
   — The currently selected format is visually highlighted.
   — Shows a circular progress indicator while estimating.

4. **GIF-specific controls** (Color Quality, Image Quality):
   — Remain visible only when GIF format is selected.
   — Hidden when WebP is selected (WebP has its own quality preset instead).

### 5.2 FileSavedActivity

- Display format name dynamically: "GIF saved" vs "Animated WebP saved".
- Share intent MIME type set based on output format.

---

## 6. Data Model Changes

### 6.1 New type: `OutputFormat`

```kotlin
enum class OutputFormat { GIF, ANIMATED_WEBP }
```

### 6.2 `TaskBuilderVideoToGif` additions

New fields:

| Field | Type | Description |
|---|---|---|
| `outputFormat` | `OutputFormat` | GIF or ANIMATED_WEBP. Default: GIF. |
| `webpQuality` | `WebpQuality?` | Non-null only when outputFormat == ANIMATED_WEBP. |

New type:

```kotlin
enum class WebpQuality(val ffmpegQuality: Int?, val lossless: Boolean) {
    SMALL(50, false),
    MEDIUM(75, false),
    HIGH(90, false),
    LOSSLESS(null, true)
}
```

### 6.3 `TaskBuilderVideoToGif` — new command

A new method `getCommandVideoToWebp()` produces the FFmpeg command to encode the extracted BMP frames into an Animated WebP file, reusing the same frame extraction output as the GIF pipeline.

```
ffmpeg -framerate {fps} -i {frames}%06d.bmp
  [-quality {q} | -lossless 1]
  -compression_level 6
  -loop 0
  -y {output}.webp
```

The existing `getCommandExtractFrame()` is unchanged — frame extraction is format-agnostic.

---

## 7. Encoding Pipeline

The WebP export reuses the existing 3-step GIF pipeline up to frame extraction, then diverges:

```
[Existing]                          [New branch]
getCommandExtractFrame()            getCommandExtractFrame()   (same)
    ↓                                   ↓
getCommandCreatePalette()           getCommandVideoToWebp()    (replaces palette + gif steps)
    ↓
getCommandVideoToGif()
```

WebP does not require a palette generation step. This makes the WebP pipeline **faster** than GIF for the same settings.

---

## 8. File Size Estimation

Estimation runs as a background coroutine triggered when the user opens the export dialog or changes any setting that affects output size.

**Algorithm:**
1. Take the first `min(duration, 1000ms)` of the clip using the current settings.
2. Encode a sample GIF and a sample WebP (at current presets) to a temp directory.
3. Measure the sample file sizes.
4. Extrapolate: `estimatedSize = sampleSize * (fullDuration / sampleDuration)`.
5. Publish results to the UI via a `StateFlow`.

**Constraints:**
- Estimation runs on `Dispatchers.IO`.
- A new estimation job cancels any in-progress estimation job (debounced by 300ms).
- Temp files are cleaned up after estimation completes or the dialog is dismissed.

---

## 9. Out-of-Scope Decisions (Deferred)

| Topic | Decision |
|---|---|
| Animated WebP preview in dialog | Deferred — static single-frame preview is sufficient for v1 |
| WebP in other flows (GIF Split, GIF→Video) | Deferred to a follow-up iteration |
| Sharing directly as WhatsApp sticker | Deferred — tracked separately in sticker export feature |
| Per-frame delay control for WebP | Deferred |

---

## 10. Resolved Decisions

| # | Question | Decision |
|---|---|---|
| OQ-1 | Should the size estimate show MB or KB depending on size magnitude? | Yes — show KB if under 1 MB, MB otherwise (e.g. `~850 KB`, `~2.3 MB`). |
| OQ-2 | Should Lossless WebP warn the user that file size may be larger than GIF? | Yes — show inline warning when Lossless preset is selected. |
