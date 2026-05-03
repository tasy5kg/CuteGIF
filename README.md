> #### English

# Easy GIF

Easy GIF is a simple and easy-to-use video to GIF tool for Android.

Latest version: `2026.4.1`

---

## Features

- Video to GIF conversion with fully customizable export settings
- Export as **animated GIF** or **animated WebP**
- Video editing: crop, trim, speed control, reverse playback
- Add text overlays with custom font, style, color, and rotation
- Background removal (chroma key / colour key)
- Video stabilization
- Real-time **file size estimation** before exporting — shows both GIF and WebP sizes side-by-side
- GIF split: extract and save individual frames from any GIF
- GIF to video conversion
- Motion Photo (`.mvimg`) to GIF conversion
- Resolution, frame rate, color quality, and clarity controls
- Preview before export

---

## Screenshots

<img src="assets/en/img1.webp" width="240"/> <img src="assets/en/img2.webp" width="240"/> <img src="assets/en/img3.webp" width="240"/> <img src="assets/en/img6.webp" width="240"/>

---

## Praise wall

<img src="assets/en/img11.webp" width="480"/>

(The text in this image was translated from Simplified Chinese by AI.)

---

## For Developers

### Architecture

Easy GIF follows the **MVVM** pattern for all new features.

```
app/
├── data/               # FileSizeEstimator interface & implementation
├── model/              # OutputFormat, WebpQuality, EstimationState
├── toolbox/            # Pure utility helpers: FileTools, MediaTools, Toolbox
├── ui/
│   └── videotogif/     # VideoToGifExportOptionsViewModel
└── *.kt                # Activities, Fragments, TaskBuilders
```

- UI state is exposed as `StateFlow`, consumed via `lifecycleScope` in Activities/Fragments.
- Background work (FFmpeg encoding, file I/O, size estimation) runs on `Dispatchers.IO` via Kotlin Coroutines.
- `TaskBuilderVideoToGif` is a serialisable data class that carries all export parameters and builds the FFmpeg command strings.
- Existing Activity-based code is not subject to MVVM refactoring unless explicitly required.

### Tech Stack

| Library | Version | Purpose |
|---|---|---|
| Kotlin | 2.3.20 | Language |
| FFmpegKit | local `.aar` | Video/image encoding, frame extraction |
| Gifsicle | native `.so` | GIF lossy compression & optimisation |
| Glide | 4.16.0 | GIF & image preview loading |
| Coroutines | 1.9.0 | Async & background work |
| Lifecycle / ViewModel | 2.8.7 | MVVM state management |
| Material Components | 1.14.0-beta01 | UI components & theming |
| Android Image Cropper | 4.5.0 | Crop UI |
| MMKV | 2.4.0 | Key-value preferences |

### Requirements

| | |
|---|---|
| Min SDK | 29 (Android 10) |
| Target SDK | 35 (Android 15) |
| Compile SDK | 36 |
| Java | 17 |

### Build

```bash
./gradlew assembleDebug
```

> FFmpegKit and Gifsicle are bundled as local `.aar` and `.so` files — no additional setup required.

---

## Copyright

沈科光 ([tasy5kg@qq.com](mailto:tasy5kg@qq.com)). 2022–2024.

This project is licensed under the [GPL-3.0 license](/COPYING).

Modified by Tuan Nguyen ([tuancoltech@gmail.com](mailto:tuancoltech@gmail.com)) on 2026.
