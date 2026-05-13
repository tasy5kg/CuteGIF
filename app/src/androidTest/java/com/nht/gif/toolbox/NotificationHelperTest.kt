package com.nht.gif.toolbox

import android.Manifest
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationManagerCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.nht.gif.MyConstants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NotificationHelperTest {

    @get:Rule
    val grantPermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val notificationManager = NotificationManagerCompat.from(context)

    @Before
    fun setUp() {
        NotificationHelper.createChannel(context)
        notificationManager.cancelAll()
    }

    /** createChannel must register the channel so it is queryable by the system. */
    @Test
    fun createChannel_registersChannelWithCorrectId() {
        val channel = notificationManager.getNotificationChannel(MyConstants.NOTIFICATION_CHANNEL_FILE_SAVED)
        assertNotNull(channel)
        assertEquals(MyConstants.NOTIFICATION_CHANNEL_FILE_SAVED, channel!!.id)
    }

    /** Calling createChannel more than once must not throw or create duplicate channels. */
    @Test
    fun createChannel_isIdempotent() {
        NotificationHelper.createChannel(context)
        val channels = notificationManager.notificationChannels
            .filter { it.id == MyConstants.NOTIFICATION_CHANNEL_FILE_SAVED }
        assertEquals(1, channels.size)
    }

    /** buildShareIntent must produce an ACTION_SEND intent with the correct URI, type, and flags. */
    @Test
    fun buildShareIntent_hasCorrectActionTypeUriAndFlags() {
        val uri = Uri.parse("content://media/external/images/media/1")
        val intent = NotificationHelper.buildShareIntent(uri, "image/gif")
        assertEquals(Intent.ACTION_SEND, intent.action)
        assertEquals("image/gif", intent.type)
        @Suppress("DEPRECATION")
        assertEquals(uri, intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM))
        assertTrue(intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
    }

    /** showShareNotification must post no notification when permissionGranted = false. */
    @Test
    fun showShareNotification_doesNotPostWhenPermissionDenied() {
        val uri = Uri.parse("content://media/external/images/media/1")
        val before = notificationManager.activeNotifications.size

        NotificationHelper.showShareNotification(
            context, uri, "image/gif", "test.gif",
            permissionGranted = false
        )

        assertEquals(before, notificationManager.activeNotifications.size)
    }

    /** showShareNotification must post exactly one notification when permissionGranted = true. */
    @Test
    fun showShareNotification_postsNotificationWhenPermissionGranted() {
        val uri = Uri.parse("content://media/external/images/media/1")

        NotificationHelper.showShareNotification(
            context, uri, "image/gif", "test.gif",
            permissionGranted = true
        )

        assertTrue(notificationManager.activeNotifications.isNotEmpty())
    }
}
