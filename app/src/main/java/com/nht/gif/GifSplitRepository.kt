package com.nht.gif

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.arthenica.ffmpegkit.FFmpegKit
import com.nht.gif.MyConstants.OUTPUT_SPLIT_DIR
import com.nht.gif.toolbox.FileTools.copyFile
import com.nht.gif.toolbox.FileTools.createNewFile
import com.nht.gif.toolbox.FileTools.resetDirectory
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Handles all file I/O and FFmpeg operations for the GIF Split screen.
 * All suspend functions dispatch on [ioDispatcher]; inject [UnconfinedTestDispatcher] in tests.
 */
class GifSplitRepository(private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO) {

  /**
   * Extracts all frames from the GIF at [gifPath] into individual PNGs under [OUTPUT_SPLIT_DIR],
   * then decodes and returns them in frame order.
   * Returns null if FFmpeg produces no output (corrupt or unsupported GIF).
   */
  suspend fun extractFrames(gifPath: String): List<Bitmap>? = withContext(ioDispatcher) {
    resetDirectory(OUTPUT_SPLIT_DIR)
    FFmpegKit.execute("${MyConstants.FFMPEG_COMMAND_PREFIX_FOR_ALL_AN} -i \"$gifPath\" \"$OUTPUT_SPLIT_DIR%06d.png\"")
    val frameCount = File(OUTPUT_SPLIT_DIR).listFiles()?.size ?: return@withContext null
    (1..frameCount).mapNotNull {
      BitmapFactory.decodeFile(OUTPUT_SPLIT_DIR + String.format("%06d", it) + ".png")
    }
  }

  /**
   * Copies the PNG at [OUTPUT_SPLIT_DIR]/[frameIndex].png to a new gallery entry.
   * [frameIndex] is 1-based, matching the slider value.
   * Returns the [Uri] of the saved file so callers can post a share notification.
   */
  suspend fun saveFrame(gifPath: String, frameIndex: Int): Uri = withContext(ioDispatcher) {
    val outputUri = createNewFile(gifPath, "png")
    copyFile("$OUTPUT_SPLIT_DIR${String.format("%06d", frameIndex)}.png", outputUri)
    outputUri
  }

  /**
   * Wipes and recreates [OUTPUT_SPLIT_DIR]. Called from [GifSplitViewModel.onCleared].
   * Launches its own coroutine on [ioDispatcher] because [viewModelScope] is already canceled
   * by the time [ViewModel.onCleared] runs, making it unusable for dispatching this work.
   */
  fun cleanup() {
    CoroutineScope(ioDispatcher).launch { resetDirectory(OUTPUT_SPLIT_DIR) }
  }
}
