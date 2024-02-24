package me.tasy5kg.cutegif

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegKitConfig
import me.tasy5kg.cutegif.MyConstants.OUTPUT_GIF_TEMP_PATH
import me.tasy5kg.cutegif.MyConstants.TASK_BUILDER_VIDEO_TO_GIF
import me.tasy5kg.cutegif.MyConstants.VIDEO_TO_GIF_SKI_EXTRACTED_FRAMES_PATH
import me.tasy5kg.cutegif.databinding.ActivityVideoToGifPerformerBinding
import me.tasy5kg.cutegif.toolbox.FileTools
import me.tasy5kg.cutegif.toolbox.FileTools.copyFile
import me.tasy5kg.cutegif.toolbox.FileTools.createNewFile
import me.tasy5kg.cutegif.toolbox.FileTools.makeDirEmpty
import me.tasy5kg.cutegif.toolbox.MediaTools
import me.tasy5kg.cutegif.toolbox.Toolbox
import me.tasy5kg.cutegif.toolbox.Toolbox.constraintBy
import me.tasy5kg.cutegif.toolbox.Toolbox.getExtra
import me.tasy5kg.cutegif.toolbox.Toolbox.keepScreenOn
import me.tasy5kg.cutegif.toolbox.Toolbox.logRed
import me.tasy5kg.cutegif.toolbox.Toolbox.onClick
import java.io.File
import kotlin.concurrent.thread

class VideoToGifPerformerSkiActivity : BaseActivity() {
  private val binding by lazy { ActivityVideoToGifPerformerBinding.inflate(layoutInflater) }
  private var taskThread: Thread? = null
  private var taskQuitOrFailed = false
  private val taskBuilder by lazy {
    intent.getExtra<TaskBuilderVideoToGifSki>(
      TASK_BUILDER_VIDEO_TO_GIF
    )
  }

  override fun onCreateIfEulaAccepted(savedInstanceState: Bundle?) {
    setContentView(binding.root)
    setFinishOnTouchOutside(false)
    onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
      override fun handleOnBackPressed() {
        quitOrFailed(getString(R.string.cancelled))
      }
    })
    binding.mbClose.onClick {
      quitOrFailed(getString(R.string.cancelled))
    }
    binding.linearProgressIndicator.isIndeterminate = false
    taskThread = thread { performPart1() }
  }

  private fun performPart1() {
    putProgress(getString(R.string.loading_video), 0, null)
    makeDirEmpty(VIDEO_TO_GIF_SKI_EXTRACTED_FRAMES_PATH)
    val command = taskBuilder.getCommandExtractFrame()
    logRed("CommandExtractFrame", command)
    FFmpegKit.executeAsync(command, { completeCallback ->
      when {
        completeCallback.returnCode.isValueSuccess -> performPart2()
        completeCallback.returnCode.isValueError -> quitOrFailed(getString(R.string.an_error_occurred))
      }
    }, { logCallback ->
      logRed("logcallback", logCallback.message.toString())
    }, { statistics ->
      putProgress(getString(R.string.loading_video), statistics.videoFrameNumber * 33 / taskBuilder.getOutputFramesEstimated(), null)
    })
  }

  private fun performPart2() {
    val pngPaths = try {
      File(VIDEO_TO_GIF_SKI_EXTRACTED_FRAMES_PATH).listFiles()!!.sorted().joinToString(" ") { it.path }
    } catch (e: Exception) {
      quitOrFailed(getString(R.string.an_error_occurred))
      return
    }
    val result = with(taskBuilder) {
      MediaTools.gifski(
        pngPaths = pngPaths,
        fps = outputFps,
        quality = gifQuality,
        width = width,
        height = height,
        outputPath = OUTPUT_GIF_TEMP_PATH,
        extra = false,
        statistics = {
          if (it.first != null && it.second != null) {
            putProgress(getString(R.string.exporting_gif), 33 + it.first!! * 67 / it.second!!, it.third)
          }
        }
      )
    }
    if (result) {
      if (!taskQuitOrFailed) {
        val outputUri = createNewFile(FileTools.FileName(taskBuilder.inputVideoPath).nameWithoutExtension, "gif")
        copyFile(OUTPUT_GIF_TEMP_PATH, outputUri, true)
        finish()
        FileSavedActivity.start(this@VideoToGifPerformerSkiActivity, outputUri)
      }
    } else {
      quitOrFailed(getString(R.string.an_error_occurred))
      return
    }
  }

  private fun putProgress(stateText: String, progress: Int, fileSizeString: String?) {
    val progressLimited = progress.constraintBy(0..100)
    runOnUiThread {
      binding.linearProgressIndicator.setProgress(progressLimited, true)
      @SuppressLint("SetTextI18n")
      binding.mtvTitle.text = stateText + (fileSizeString?.let { "（预计$it）" } ?: "…")
    }
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

    fun start(context: Context, taskBuilderVideoToGifSki: TaskBuilderVideoToGifSki) =
      context.startActivity(Intent(context, VideoToGifPerformerSkiActivity::class.java).apply {
        putExtra(TASK_BUILDER_VIDEO_TO_GIF, taskBuilderVideoToGifSki)
      })
  }
}