package me.tasy5kg.cutegif

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegKitConfig
import java.io.File
import kotlin.concurrent.thread
import kotlin.math.min
import me.tasy5kg.cutegif.MyConstants.EXTRA_VIDEO_URI
import me.tasy5kg.cutegif.MyConstants.FFMPEG_COMMAND_PREFIX_FOR_ALL
import me.tasy5kg.cutegif.MyConstants.VIDEO_TO_GIF_VIDEO_FALLBACK_DIR
import me.tasy5kg.cutegif.Toolbox.createFfSafForRead
import me.tasy5kg.cutegif.Toolbox.fileName
import me.tasy5kg.cutegif.Toolbox.getExtra
import me.tasy5kg.cutegif.Toolbox.keepScreenOn
import me.tasy5kg.cutegif.Toolbox.logRed
import me.tasy5kg.cutegif.Toolbox.makeDirEmpty
import me.tasy5kg.cutegif.Toolbox.onClick
import me.tasy5kg.cutegif.Toolbox.removeFileNameExtension
import me.tasy5kg.cutegif.Toolbox.videoDuration
import me.tasy5kg.cutegif.databinding.ActivityVideoToGifVideoFallbackBinding

class VideoToGifVideoFallbackActivity : BaseActivity() {
  private val binding by lazy { ActivityVideoToGifVideoFallbackBinding.inflate(layoutInflater) }
  private val videoUri by lazy { intent.getExtra<Uri>(EXTRA_VIDEO_URI) }
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
    taskThread = thread { performFallback() }
  }

  private fun performFallback() {
    keepScreenOn(true)
    val duration = try {
      videoUri.videoDuration()
    } catch (e: Exception) {
      0
    }
    if (duration == 0) {
      quitOrFailed("无法读取视频")
      return
    }
    makeDirEmpty(VIDEO_TO_GIF_VIDEO_FALLBACK_DIR)
    val fallbackMp4Path = "$VIDEO_TO_GIF_VIDEO_FALLBACK_DIR${videoUri.fileName().removeFileNameExtension()}.mp4"
    val command = "$FFMPEG_COMMAND_PREFIX_FOR_ALL " +
        "-i ${videoUri.createFfSafForRead()} " +
        "-c:v libx264 -preset:v ultrafast -crf 17 -pix_fmt yuv420p -c:a aac -b:a 128k -y $fallbackMp4Path"
    logRed("command", command)
    logRed("fallbackMp4Path", fallbackMp4Path)
    FFmpegKit.executeAsync(command, {
      when {
        it.returnCode.isValueSuccess && !taskQuitOrFailed -> {
          VideoToGifActivity.start(this, Uri.fromFile(File(fallbackMp4Path)))
          finish()
        }

        it.returnCode.isValueError -> {
          runOnUiThread { Toolbox.toast("无法读取视频") }
          finish()
        }
      }
    },
      {
        logRed("logcallback", it.message.toString())
      }, {
        val progress = min(it.time * 100 / duration, 99)
        runOnUiThread {
          binding.mtvTitle.text = "正在转码视频（$progress%）"
          binding.linearProgressIndicator.setProgress(progress, true)
        }
      }
    )
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
    fun start(context: Context, videoUri: Uri) =
      context.startActivity(Intent(context, VideoToGifVideoFallbackActivity::class.java).apply {
        putExtra(EXTRA_VIDEO_URI, videoUri)
      })
  }
}