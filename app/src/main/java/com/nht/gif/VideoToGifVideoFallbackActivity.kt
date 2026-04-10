package com.nht.gif

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegKitConfig
import com.nht.gif.MyConstants.EXTRA_VIDEO_PATH
import com.nht.gif.MyConstants.FFMPEG_COMMAND_PREFIX_FOR_ALL
import com.nht.gif.MyConstants.INPUT_FILE_DIR
import com.nht.gif.databinding.ActivityVideoToGifVideoFallbackBinding
import com.nht.gif.toolbox.FileTools.resetDirectory
import com.nht.gif.toolbox.MediaTools.getVideoDurationMsByFFmpeg
import com.nht.gif.toolbox.Toolbox.getExtra
import com.nht.gif.toolbox.Toolbox.keepScreenOn
import com.nht.gif.toolbox.Toolbox.logRed
import com.nht.gif.toolbox.Toolbox.onClick
import com.nht.gif.toolbox.Toolbox.toast
import kotlin.concurrent.thread
import kotlin.math.min
import kotlin.math.roundToInt

class VideoToGifVideoFallbackActivity : BaseActivity() {
  private val binding by lazy { ActivityVideoToGifVideoFallbackBinding.inflate(layoutInflater) }
  private val inputVideoPath by lazy { intent.getExtra<String>(EXTRA_VIDEO_PATH) }
  private var taskThread: Thread? = null
  private var taskQuitOrFailed = false

  override fun onCreateIfEulaAccepted(savedInstanceState: Bundle?) {
    setContentView(binding.root)
    setFinishOnTouchOutside(false)
    onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
      override fun handleOnBackPressed() {
        quitOrFailed(getString(R.string.cancelled))
      }
    })
    binding.mbClose.onClick { quitOrFailed(getString(R.string.cancelled)) }
    taskThread = thread { performFallback() }
  }

  private fun performFallback() {
    keepScreenOn(true)
    val duration = getVideoDurationMsByFFmpeg(inputVideoPath)
    val fallbackMp4Path = "${inputVideoPath}_fallback.mp4"
    val command =
      "$FFMPEG_COMMAND_PREFIX_FOR_ALL -i \"$inputVideoPath\" -c:v libx264 -preset:v veryfast -crf 17 -pix_fmt yuv420p -c:a aac -b:a 128k -y \"$fallbackMp4Path\""
    logRed("command", command)
    logRed("fallbackMp4Path", fallbackMp4Path)
    FFmpegKit.executeAsync(command, {
      when {
        it.returnCode.isValueSuccess && !taskQuitOrFailed -> {
          VideoToGifActivity.start(this, fallbackMp4Path)
          finish()
        }

        it.returnCode.isValueError -> {
          runOnUiThread { toast(R.string.unable_to_read_video) }
          finish()
          resetDirectory(INPUT_FILE_DIR)
        }
      }
    }, {
      logRed("logcallback", it.message.toString())
    }, {
      if (duration != null) {
        val progress = min((it.time * 100 / duration).roundToInt(), 99)
        runOnUiThread {
          binding.mtvTitle.text = getString(R.string.transcoding_video__d_, progress)
          binding.linearProgressIndicator.isIndeterminate = false
          binding.linearProgressIndicator.setProgress(progress, true)
        }
      }
    })
  }

  private fun quitOrFailed(toastText: String?) {
    runOnUiThread {
      taskQuitOrFailed = true
      toastText?.let { toast(it) }
      FFmpegKit.cancel()
      FFmpegKitConfig.clearSessions()
      taskThread?.interrupt()
      finish()
    }
  }

  override fun onDestroy() {
    keepScreenOn(false)
    super.onDestroy()
  }

  companion object {
    fun start(context: Context, inputVideoPath: String) = context.startActivity(
      Intent(context, VideoToGifVideoFallbackActivity::class.java).putExtra(
        EXTRA_VIDEO_PATH, inputVideoPath
      )
    )
  }
}