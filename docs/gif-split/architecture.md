# GIF Split — Architecture Design

## Component Overview

```
GifSplitActivity
├── onCreateIfEulaAccepted()
│   ├── wire close / slider ± click listeners immediately (main thread)
│   ├── disable all controls
│   └── lifecycleScope.launch {
│       ├── withContext(Dispatchers.IO): frame extraction pipeline
│       │   ├── resetDirectory(OUTPUT_SPLIT_DIR)
│       │   ├── FFmpegKit.execute(...)         ← was: blocking on main thread
│       │   ├── File.listFiles()               ← was: blocking on main thread
│       │   └── BitmapFactory.decodeFile × N  ← was: blocking on main thread
│       └── [main thread resumed]
│           ├── guard: null frames → toast + finish
│           ├── setup slider (multi-frame) or hide selector (single-frame)
│           ├── aciv.setImageBitmap(frames[0])
│           ├── enable controls
│           └── wire mbSave.onClick {
│               └── lifecycleScope.launch {
│                   ├── disable controls
│                   ├── capture slider value on main thread
│                   ├── withContext(Dispatchers.IO): copyFile + createNewFile
│                   └── [main thread resumed]
│                       ├── toast(saved)
│                       ├── show/hide flash overlay
│                       └── enable controls
│               }
│           }
└── onDestroy()
    └── resetDirectory(OUTPUT_SPLIT_DIR)   [kept on main thread — tiny, destroy-time only]
```

## Design Decisions

### Coroutines over Thread/Executor
`AGENTS.md` mandates Kotlin Coroutines for all background work. `lifecycleScope` ties coroutine lifetime to the Activity lifecycle, cancelling automatically on `onDestroy`. No manual cancellation logic is required.

### Two-phase `mbSave.onClick` registration
The `mbSave` click listener is registered inside the loading coroutine (after frames are ready) rather than upfront. Combined with `isEnabled = false` during loading, this ensures the save path is never triggered against an empty or partial frame list.

### `slider.value` captured before IO switch
`binding.slider.value` is read on the main thread before `withContext(Dispatchers.IO)` to avoid accessing a `View` property from a background thread.

### Controls disabled during operations
`setControlsEnabled(Boolean)` disables/enables `mbSave`, `mbSliderMinus`, `mbSliderPlus`, and `slider` as a group. This prevents overlapping operations without adding complex state machines.

## Dependencies

| Dependency | Purpose |
|---|---|
| `kotlinx-coroutines-android` | `Dispatchers.Main` and `Dispatchers.IO`; `lifecycleScope` coroutine execution |
| `androidx.lifecycle:lifecycle-runtime-ktx` | `lifecycleScope` extension on `AppCompatActivity` (transitively available via `appcompat`) |
