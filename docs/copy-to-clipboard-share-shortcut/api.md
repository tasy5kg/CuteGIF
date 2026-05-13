# API Documentation — Copy to Clipboard + Persistent Share Shortcut

---

## 1. `NotificationHelper` (`com.nht.gif.toolbox.NotificationHelper`)

Stateless helper object for creating the file-saved notification channel and posting share notifications.

---

### `createChannel(context: Context)`

Registers the `cute_gif_file_saved` notification channel on API 26+. No-op on lower APIs and safe to call multiple times (idempotent).

Must be called once before any `showShareNotification()` call. Recommended call site: `Application.onCreate()` or `MainActivity.onCreate()`.

**Parameters**

| Name | Type | Description |
|------|------|-------------|
| `context` | `Context` | Any context; used to obtain `NotificationManager`. |

**Threading:** Main thread. Execution is fast (system call); no IO.

---

### `showShareNotification(context: Context, uri: Uri, mimeType: String, fileName: String)`

Posts a persistent notification to the notification shade after a file is saved. Does nothing if `POST_NOTIFICATIONS` permission is missing on API 33+.

**Parameters**

| Name | Type | Description |
|------|------|-------------|
| `context` | `Context` | Activity or application context. |
| `uri` | `Uri` | MediaStore `content://` URI of the saved file. |
| `mimeType` | `String` | MIME type of the file, e.g. `"image/gif"`, `"image/webp"`, `"video/mp4"`. |
| `fileName` | `String` | Display name shown in the notification title, e.g. `"output_20260510.gif"`. |

**Notification behaviour**

- Title: `"<fileName> saved"`
- Body: localised format string with MIME type.
- Content tap: opens `FileSavedActivity` with `uri`.
- Share action: opens system chooser via `Intent.ACTION_SEND`.
- Auto-cancels on tap.

**Threading:** Thread-safe. `NotificationManagerCompat.notify()` handles internal synchronisation.

---

### `buildShareIntent(uri: Uri, mimeType: String): Intent`

Builds an `Intent.ACTION_SEND` intent for the given file. Used internally by `showShareNotification` and exposed for reuse.

**Parameters**

| Name | Type | Description |
|------|------|-------------|
| `uri` | `Uri` | MediaStore URI of the file to share. |
| `mimeType` | `String` | MIME type of the file. |

**Returns:** `Intent` configured with `ACTION_SEND`, `EXTRA_STREAM = uri`, type, and `FLAG_GRANT_READ_URI_PERMISSION`.

---

## 2. `FileSavedActivity` — new extension

### `copyToClipboard(uri: Uri)` (private fun)

Copies the provided URI into the system clipboard as a plain URI clip.

- On API < 33: shows a Snackbar with `R.string.copied_to_clipboard`.
- On API ≥ 33: relies on the system clipboard confirmation UI (no additional feedback emitted).

**Parameters**

| Name | Type | Description |
|------|------|-------------|
| `uri` | `Uri` | URI to place in clipboard. MediaStore URIs are directly supported. |

**Threading:** Must be called on the main thread (accesses `ClipboardManager`, a UI service).

---

## 3. String Resources

New strings added to `values/strings.xml` (and `values-zh/strings.xml`):

| Key | English value | Purpose |
|-----|--------------|---------|
| `copy_to_clipboard` | `"Copy"` | Copy button label |
| `copied_to_clipboard` | `"Copied to clipboard"` | Snackbar text (API < 33) |
| `notification_channel_file_saved_name` | `"File saved"` | Notification channel display name |
| `notification_file_saved_title` | `"%1$s saved"` | Notification title (`%1$s` = file name) |
| `notification_file_saved_body` | `"Tap to open or share"` | Notification body text |
| `notification_action_share` | `"Share"` | Notification action button label |

---

## 4. Constants (additions to `MyConstants.kt`)

| Constant | Value | Purpose |
|----------|-------|---------|
| `NOTIFICATION_CHANNEL_FILE_SAVED` | `"cute_gif_file_saved"` | Channel ID |
| `NOTIFICATION_ID_FILE_SAVED_BASE` | `1000` | Base notification ID; final ID = base + (System.currentTimeMillis() % 1000).toInt() for uniqueness per save |
