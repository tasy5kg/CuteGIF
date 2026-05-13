# Tasks — Copy to Clipboard + Persistent Share Shortcut

> Spec: `spec.md` | Architecture: `architecture.md` | API: `api.md`
> Feature branch: implement from `main`

---

## Infrastructure

- [x] **INFRA-1** — Add `<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />` (with `tools:targetApi="33"`) to `AndroidManifest.xml`.
- [x] **INFRA-2** — Add `NOTIFICATION_CHANNEL_FILE_SAVED` and `NOTIFICATION_ID_FILE_SAVED_BASE` constants to `MyConstants.kt`.
- [x] **INFRA-3** — Create `toolbox/NotificationHelper.kt` as an `object` with `createChannel(context)` and `buildShareIntent(uri, mimeType)`. Channel name string `notification_channel_file_saved_name` added to both `values/` and `values-zh/`.
- [x] **INFRA-4** — `NotificationHelper.createChannel(this)` wired into existing `MyApplication.onCreate()`.

---

## Copy to Clipboard

- [x] **CLIP-1** — `copy_to_clipboard` and `copied_to_clipboard` strings already present in both `values/` and `values-zh/strings.xml` — no action needed.
- [x] **CLIP-2** — "Copy" `MaterialButton` (`@id/mbCopy`) added to `activity_file_saved.xml` between Share and Done, styled as `Widget.Material3.Button.OutlinedButton`.
- [x] **CLIP-3** — `copyToClipboard(uri)` implemented in `FileSavedActivity` using `ClipData.newUri` + `ClipboardManager.setPrimaryClip`. Toast shown on API < 33; silent on API ≥ 33.
- [x] **CLIP-4** — `mbCopy` wired to `copyToClipboard(fileUri)` in `onCreateIfEulaAccepted`.

---

## Share Notification

- [x] **NOTIF-1** — Notification strings added to both locales: `notification_file_saved_title`, `notification_file_saved_body`. `notification_action_share` reuses existing `R.string.share`. `MIME_TYPE_IMAGE_PNG` constant added to `MyConstants`.
- [x] **NOTIF-2** — `NotificationHelper.buildShareIntent(uri, mimeType)` implemented (in INFRA-3). `FileSavedActivity.createIntent()` companion method added for PendingIntent construction.
- [x] **NOTIF-3** — `NotificationHelper.showShareNotification(context, uri, mimeType, fileName, permissionGranted)` implemented with permission guard, content PendingIntent, share action PendingIntent, and auto-cancel notification.
- [x] **NOTIF-4** — `showShareNotification` called in `FileSavedActivity.onCreateIfEulaAccepted()`. Moved here from the converter activities (`VideoToGifPerformerActivity`, `GifToVideoActivity`) where `finish()` set `isFinishing=true` before the `Dispatchers.Main.immediate` coroutine ran, making the permission dialog and Snackbar unreachable.
- [x] **NOTIF-5** — (Covered by NOTIF-4; `GifToVideoActivity` no longer calls `showShareNotification` directly.)
- [x] **NOTIF-6** — `GifSplitRepository.saveFrame()` now returns `Uri`. `GifSplitViewModel.Event.SaveSuccess` updated to `data class SaveSuccess(val uri: Uri)`. `GifSplitActivity` calls `showShareNotification` on `SaveSuccess`.

---

## Tests

> **Note:** `NotificationHelper` wraps Android system services so all its tests live in `androidTest/` (instrumented). Two channel tests already written at `androidTest/com/nht/gif/toolbox/NotificationHelperTest.kt`.

- [x] **TEST-INFRA** — `createChannel_registersChannelWithCorrectId` and `createChannel_isIdempotent` written in `NotificationHelperTest` (androidTest).
- [x] **TEST-1** — `buildShareIntent_hasCorrectActionTypeUriAndFlags` added to `NotificationHelperTest`.
- [x] **TEST-2** — `showShareNotification_doesNotPostWhenPermissionDenied` added to `NotificationHelperTest`.
- [x] **TEST-3** — `showShareNotification_postsNotificationWhenPermissionGranted` added to `NotificationHelperTest`.
- [x] **TEST-4** — `FileSavedClipboardTest.copyToClipboard_clipHasCorrectUriAndItemCount` written in `androidTest/`.

---

## Done Criteria

All checkboxes above are ticked, all new unit tests pass, and no existing tests are broken.
