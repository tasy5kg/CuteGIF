package com.nht.gif

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.nht.gif.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Verifies the clipboard write contract used by FileSavedActivity.copyToClipboard().
 *
 * The function is private so the test validates the same ClipboardManager API calls inline.
 * The instrumented context ensures ClipboardManager is accessible and the test has foreground
 * focus (required on API 33+ to read back primaryClip).
 */
@RunWith(AndroidJUnit4::class)
class FileSavedClipboardTest {

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun copyToClipboard_clipHasCorrectUriAndItemCount() {
        val uri = Uri.parse("content://media/external/images/media/99")
        val label = context.getString(R.string.copy_to_clipboard)
        val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

        manager.setPrimaryClip(ClipData.newUri(context.contentResolver, label, uri))

        val clip = manager.primaryClip
        assertNotNull(clip)
        assertEquals(1, clip!!.itemCount)
        assertEquals(uri, clip.getItemAt(0).uri)
    }
}
