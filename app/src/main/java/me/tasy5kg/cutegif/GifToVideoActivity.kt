package me.tasy5kg.cutegif

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegKitConfig
import me.tasy5kg.cutegif.MyConstants.EXTRA_GIF_PATH
import me.tasy5kg.cutegif.MyConstants.FFMPEG_COMMAND_PREFIX_FOR_ALL_AN
import me.tasy5kg.cutegif.Toolbox.createFfSafForWrite
import me.tasy5kg.cutegif.Toolbox.getExtra
import me.tasy5kg.cutegif.Toolbox.keepScreenOn
import me.tasy5kg.cutegif.Toolbox.logRed
import me.tasy5kg.cutegif.Toolbox.onClick
import me.tasy5kg.cutegif.Toolbox.pathToUri
import me.tasy5kg.cutegif.Toolbox.videoDuration
import me.tasy5kg.cutegif.databinding.ActivityGifToVideoBinding
import kotlin.concurrent.thread
import kotlin.math.min

class GifToVideoActivity : BaseActivity() {
  private val binding by lazy { ActivityGifToVideoBinding.inflate(layoutInflater) }
  private val inputGifPath by lazy { intent.getExtra<String>(MyConstants.EXTRA_GIF_PATH) }
  private var taskThread: Thread? = null
  private var taskQuitOrFailed = false

  override fun onCreateIfEulaAccepted(savedInstanceState: Bundle?) {
    setContentView(binding.root)
    setFinishOnTouchOutside(false)
    onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
      override fun handleOnBackPressed() {
        quitOrFailed("已取消")
      }
    })
    binding.mbClose.onClick { quitOrFailed("已取消") }
    taskThread = thread { performTranscode() }
  }

  private fun performTranscode() {
    keepScreenOn(true)
    val duration = try {
      pathToUri(inputGifPath).videoDuration()
    } catch (_: Exception) {
      0
    }
    if (duration == 0) {
      quitOrFailed("无法读取 GIF")
      return
    }
    val videoUri = Toolbox.createNewFile(pathToUri(inputGifPath), "mp4")
    val command =
      "$FFMPEG_COMMAND_PREFIX_FOR_ALL_AN -i $inputGifPath " +
          "-c:v libx264 -crf 23 -preset veryslow -pix_fmt yuv420p " +
          "-vf pad=\"width=ceil(iw/2)*2:height=ceil(ih/2)*2\" " + // reference: https://stackoverflow.com/a/53024964
          "-movflags +faststart " +
          videoUri.createFfSafForWrite()
    logRed("command", command)
    FFmpegKit.executeAsync(command, {
      when {
        it.returnCode.isValueSuccess && !taskQuitOrFailed -> {
          FileSavedActivity.start(this, videoUri)
          finish()
        }

        it.returnCode.isValueError -> {
          runOnUiThread { Toolbox.toast("GIF 转视频失败") }
          finish()
        }
      }
    },
      {
        logRed("logcallback", it.message.toString())
      }, {
        val progress = min(it.time * 100 / duration, 99)
        runOnUiThread {
          binding.mtvTitle.text = "正在转换为视频（$progress%）"
          binding.linearProgressIndicator.setProgress(progress, true)
        }
      })
  }

  private fun quitOrFailed(toastText: String?) {
    runOnUiThread {
      taskQuitOrFailed = true
      toastText?.let { Toolbox.toast(it) }
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
    fun start(context: Context, gifPath: String) =
      context.startActivity(Intent(context, GifToVideoActivity::class.java).apply {
        putExtra(EXTRA_GIF_PATH, gifPath)
      })
  }
}