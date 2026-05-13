# Specification — Copy to Clipboard + Persistent Share Shortcut

> Feature from: `docs/feature-brainstorm.md` § "Copy to Clipboard + Persistent Share Shortcut"
> Status: Planned

---

## 1. Overview

Two complementary improvements to reduce post-export friction:

1. **Copy to Clipboard** — a "Copy" button on the save/result screen (`FileSavedActivity`) that copies the exported file (GIF, WebP, or MP4) into the system clipboard so the user can paste it directly into WhatsApp, Telegram, iMessage (cross-device), or any other app without opening the gallery.

2. **Persistent Share Notification** — immediately after any file save completes, show an Android notification that persists in the notification shade. The notification carries a one-tap Share action and an Open action so the user can act on the file from any screen without navigating back through the app.

---

## 2. Scope

### In scope
- Copy button in `FileSavedActivity` for GIF, WebP, and MP4 outputs.
- Persistent notification triggered by every successful save: VideoToGif conversion, GIF-to-Video conversion, GIF frame split.
- Share action on notification (re-uses `Intent.ACTION_SEND` with correct MIME type).
- Open action on notification (launches `FileSavedActivity` with the saved Uri).
- Runtime `POST_NOTIFICATIONS` permission request on Android 13+ (API 33).

### Out of scope
- Clipboard paste / read from clipboard.
- Batch export / multiple simultaneous notifications.
- Notification history or badge count.

---

## 3. Acceptance Criteria

### 3.1 Copy to Clipboard

| ID | Criterion |
|----|-----------|
| AC-C1 | A "Copy" button is visible on `FileSavedActivity` for all output types (GIF, WebP, MP4). |
| AC-C2 | Tapping "Copy" places the file Uri into the system clipboard with the correct MIME label. |
| AC-C3 | On Android < 13, a Snackbar "Copied!" is shown as visual feedback. |
| AC-C4 | On Android ≥ 13, no duplicate feedback is shown (the system shows its own clipboard toast). |
| AC-C5 | The copied Uri is directly pasteable in WhatsApp (tested manually). |

### 3.2 Persistent Share Notification

| ID | Criterion |
|----|-----------|
| AC-N1 | A notification appears in the shade after every successful file save. |
| AC-N2 | The notification displays the file name and type (e.g., "output.gif saved"). |
| AC-N3 | Tapping the notification body opens `FileSavedActivity` with the saved file. |
| AC-N4 | Tapping the "Share" action button opens the system share sheet for the file. |
| AC-N5 | On Android ≥ 13 (API 33), the app requests `POST_NOTIFICATIONS` at runtime before showing the first notification. If permission is denied, no notification is shown (silent failure, no crash). |
| AC-N6 | On Android ≥ 8 (API 26), a notification channel "File saved" (`cute_gif_file_saved`) is registered. |
| AC-N7 | The notification is auto-cancelled when the user taps either action. |

---

## 4. Platform Constraints

| Constraint | Detail |
|-----------|--------|
| Clipboard — content URI | `ClipboardManager.setPrimaryClip()` with a MediaStore `content://` URI works without a custom `FileProvider` since MediaStore URIs are world-readable. |
| Notification channel | Required from API 26. Must be created before posting any notification; safe to call repeatedly (no-op if channel already exists). |
| `POST_NOTIFICATIONS` | Required from API 33. Must request at runtime; missing permission must not crash — just suppress the notification. |
| PendingIntent flags | Use `PendingIntent.FLAG_IMMUTABLE` (required on API 31+). |
| Share PendingIntent | The `Intent.ACTION_SEND` PendingIntent must use `FLAG_ACTIVITY_NEW_TASK` so it can be launched from a notification context. |

---

## 5. User Stories

- **US-C1** As a user, after exporting a GIF, I want to copy it to clipboard so I can paste it into a chat without leaving my current workflow.
- **US-N1** As a user, after a conversion finishes in the background, I want a notification so I can share the file immediately without navigating back into the app.
