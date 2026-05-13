# Architecture Design — Copy to Clipboard + Persistent Share Shortcut

---

## 1. Component Map

```
┌─────────────────────────────────────────────────────────┐
│  Existing save callers                                  │
│  ┌─────────────────────────┐                           │
│  │ VideoToGifPerformerActivity │ ─── (1) save done ──► │
│  └─────────────────────────┘                           │
│  ┌──────────────────────┐                              │
│  │ GifToVideoActivity   │ ─── (1) save done ──────────►│
│  └──────────────────────┘                              │──► NotificationHelper
│  ┌──────────────────────┐                              │        │
│  │ GifSplitActivity     │ ─── (1) save done ──────────►│        │ (2) show
│  └──────────────────────┘                              │        ▼
└─────────────────────────────────────────────────────────┘  Android Notification Shade
                                                                │
                                                  (3) tap body / Share action
                                                                │
                                                     FileSavedActivity
                                                     ┌──────────────────────┐
                                                     │  [Share]  [Copy] [Delete] [Done] │
                                                     └──────────────────────┘
                                                           │
                                              (4) Copy tap → ClipboardManager
```

---

## 2. New Components

### `NotificationHelper` (`toolbox/NotificationHelper.kt`)

- **Type:** `object` (singleton, stateless helper)
- **Responsibilities:**
  - Create and register the notification channel (`createChannel(context)`)
  - Build and post a share notification (`showShareNotification(context, uri, mimeType, fileName)`)
  - Build the share `PendingIntent` and the open-file `PendingIntent`
- **Threading:** All calls are lightweight (no IO). Safe to call from any thread, but must not block the main thread with channel creation; channel registration is synchronous but fast.
- **Why an `object`:** Stateless; no need for DI plumbing for a simple system API wrapper. Consistent with `FileTools` / `Toolbox` patterns already in the codebase.

---

## 3. Modified Components

### `FileSavedActivity`

- **Change:** Add a "Copy" `MaterialButton` to the layout.
- **Logic added:** `copyToClipboard(uri: Uri)` — calls `ClipboardManager.setPrimaryClip()` with a `ClipData` built from the MediaStore URI; shows a Snackbar on API < 33.
- **No ViewModel change needed** — clipboard copy is a pure UI side-effect with no persistent state.

### `VideoToGifPerformerActivity`

- **Change:** After creating the output Uri and launching `FileSavedActivity`, call `NotificationHelper.showShareNotification(...)`.
- **Touched functions:** `performWebpSave()` (line 87–91) and `performPart4()` (line 132–135).

### `GifToVideoActivity`

- **Change:** After `createNewFile(...)` + launching `FileSavedActivity`, call `NotificationHelper.showShareNotification(...)`.
- **Touched function:** save result handler (line ~53–61).

### `GifSplitActivity`

- **Change:** Observe `SaveSuccess` event from `GifSplitViewModel`; after existing gallery-open behavior, call `NotificationHelper.showShareNotification(...)`.

### `AndroidManifest.xml`

- **Change:** Add `<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />` guarded with `android:minSdkVersion="33"`.

### `MyApplication`

- **Change:** Call `NotificationHelper.createChannel(this)` in the existing `MyApplication.onCreate()` to register the channel at startup before any save action can occur.

---

## 4. Threading Model

| Operation | Thread | Rationale |
|-----------|--------|-----------|
| `NotificationHelper.createChannel()` | Main (on startup) | Channel registration is fast, UI-safe |
| `NotificationHelper.showShareNotification()` | Main or IO (post-save) | `NotificationManagerCompat.notify()` is thread-safe; no IO involved |
| `copyToClipboard()` | Main | `ClipboardManager` is a UI service; must be called on main thread |
| Build `PendingIntent` | Main | Lightweight; no IO |

---

## 5. Permission Handling

```
API < 33  → no runtime permission needed; just register channel and post
API ≥ 33  → check POST_NOTIFICATIONS at runtime before first showShareNotification()
            if denied → suppress silently; no crash, no retry loop
            where to request → VideoToGifExportOptionsDialogFragment (before conversion starts)
                               or lazily inside NotificationHelper.showShareNotification()
```

Decision: **Lazy request inside `showShareNotification()`**, triggered at the point of first save.

Three-state flow (API 33+ only):
1. **Not yet asked** (`shouldShowRationale = false`, `MySettings.notificationPermissionRequested = false`) → show system dialog, persist `notificationPermissionRequested = true`.
2. **Denied once, recoverable** (`shouldShowRationale = true`) → show system dialog again.
3. **Permanently denied** (`shouldShowRationale = false`, `notificationPermissionRequested = true`) → show Snackbar with a "Settings" action that deep-links to `Settings.ACTION_APP_NOTIFICATION_SETTINGS`.

`context is Activity` gates the entire flow — non-Activity contexts (tests, application context) are silently ignored. All Snackbar and `requestPermissions` calls are dispatched on `Dispatchers.Main.immediate` via a fire-and-forget `CoroutineScope`, so callers on background threads (FFmpeg callbacks) are safe.

---

## 6. Notification Design

- **Channel ID:** `cute_gif_file_saved`
- **Channel name:** `"File saved"` (localised string)
- **Priority:** `PRIORITY_DEFAULT`
- **Style:** `NotificationCompat.BigTextStyle` — title: `"{filename} saved"`, body: file type + size.
- **Actions:**
  - Action 0 (content tap): `PendingIntent` → `FileSavedActivity.start(context, uri)` (existing factory method)
  - Action 1 (button "Share"): `PendingIntent` → `Intent.ACTION_SEND` chooser
- **Auto-cancel:** `true`
- **Small icon:** existing app icon drawable

---

## 7. Risks & Mitigations

| Risk | Mitigation |
|------|-----------|
| Clipboard sharing on API < 24 may not support URI clips | All targets are API 24+; no mitigation needed |
| `POST_NOTIFICATIONS` denied leaves user with no feedback after background conversion | Acceptable UX (existing behavior); Snackbar/Toast in `FileSavedActivity` still works |
| Multiple rapid saves produce multiple notifications | Each call to `showShareNotification` uses a unique notification ID derived from the save timestamp; acceptable (1 notification per save) |
| Opening `FileSavedActivity` from notification while app is backgrounded | Use `FLAG_ACTIVITY_NEW_TASK`; existing `singleTask` launch mode handles re-entry cleanly |
