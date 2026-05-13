package com.nht.gif.toolbox

import android.Manifest
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.nht.gif.FileSavedActivity
import com.nht.gif.MyConstants
import com.nht.gif.MySettings
import com.nht.gif.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Helpers for creating the file-saved notification channel and posting share notifications.
 *
 * All methods are thread-safe. Channel creation and notification posting are fast system calls
 * that do not block the calling thread regardless of whether it is the main or a background thread.
 */
object NotificationHelper {

  /**
   * Registers the [MyConstants.NOTIFICATION_CHANNEL_FILE_SAVED] notification channel.
   * No-op below API 26. Safe to call multiple times — the system ignores duplicate registrations.
   */
  fun createChannel(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val channel = NotificationChannel(
      MyConstants.NOTIFICATION_CHANNEL_FILE_SAVED,
      context.getString(R.string.notification_channel_file_saved_name),
      NotificationManager.IMPORTANCE_DEFAULT
    )
    NotificationManagerCompat.from(context).createNotificationChannel(channel)
  }

  /**
   * Builds an [Intent.ACTION_SEND] intent for sharing [uri] with the given [mimeType].
   * Grants temporary read permission so the receiving app can access the MediaStore URI.
   */
  fun buildShareIntent(uri: Uri, mimeType: String): Intent = Intent().apply {
    action = Intent.ACTION_SEND
    putExtra(Intent.EXTRA_STREAM, uri)
    type = mimeType
    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
  }

  /**
   * Posts a file-saved notification with a Share action and a tap-to-open content intent.
   *
   * When the POST_NOTIFICATIONS permission is missing on API 33+ and [context] is an [Activity]:
   * - First / recoverable denial → triggers the system permission dialog.
   * - Permanent denial → shows a Snackbar directing the user to app notification settings.
   *
   * [permissionGranted] is exposed for testing only; production callers use the default.
   */
  fun showShareNotification(
    context: Context,
    uri: Uri,
    mimeType: String,
    fileName: String,
    permissionGranted: Boolean = isNotificationPermissionGranted(context),
  ) {
    Log.d("tuancoltech", "showShareNotification permissionGranted: $permissionGranted")
    if (permissionGranted) {
      postNotification(context, uri, mimeType, fileName)
      return
    }
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || context !is Activity) return
    CoroutineScope(Dispatchers.Main.immediate).launch {
      Log.d("tuancoltech", "showShareNotification isFInishing: ${context.isFinishing} isDestroyed: ${context.isDestroyed}")
      if (context.isFinishing || context.isDestroyed) return@launch
      handleMissingPermission(context)
    }
  }

  private fun handleMissingPermission(activity: Activity) {
    val shouldShowRationale = ActivityCompat.shouldShowRequestPermissionRationale(
      activity, Manifest.permission.POST_NOTIFICATIONS
    )

    Log.v("tuancoltech", "handleMissingPermission shouldShowRationale: $shouldShowRationale notificationPermissionRequested: ${MySettings.notificationPermissionRequested}")
    if (shouldShowRationale || !MySettings.notificationPermissionRequested) {
      MySettings.notificationPermissionRequested = true
      ActivityCompat.requestPermissions(
        activity,
        arrayOf(Manifest.permission.POST_NOTIFICATIONS),
        0
      )
    } else {
      Snackbar.make(
        activity.findViewById(android.R.id.content),
        R.string.notifications_blocked_snackbar,
        Snackbar.LENGTH_LONG
      ).setAction(R.string.settings) {
        activity.startActivity(
          Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, activity.packageName)
        )
      }.show()
    }
  }

  private fun postNotification(context: Context, uri: Uri, mimeType: String, fileName: String) {
    val notificationId = (MyConstants.NOTIFICATION_ID_FILE_SAVED_BASE + System.currentTimeMillis() % 1000).toInt()

    val openPendingIntent = PendingIntent.getActivity(
      context,
      notificationId,
      FileSavedActivity.createIntent(context, uri),
      PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    val sharePendingIntent = PendingIntent.getActivity(
      context,
      notificationId + 1,
      Intent.createChooser(buildShareIntent(uri, mimeType), null)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
      PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_ONE_SHOT
    )

    val notification = NotificationCompat.Builder(context, MyConstants.NOTIFICATION_CHANNEL_FILE_SAVED)
      .setSmallIcon(R.mipmap.ic_launcher)
      .setContentTitle(context.getString(R.string.notification_file_saved_title, fileName))
      .setContentText(context.getString(R.string.notification_file_saved_body))
      .setContentIntent(openPendingIntent)
      .addAction(0, context.getString(R.string.share), sharePendingIntent)
      .setAutoCancel(true)
      .build()

    NotificationManagerCompat.from(context).notify(notificationId, notification)
  }

  private fun isNotificationPermissionGranted(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
      ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
}
