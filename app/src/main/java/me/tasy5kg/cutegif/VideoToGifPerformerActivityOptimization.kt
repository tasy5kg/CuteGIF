package me.tasy5kg.cutegif

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegKitConfig
import me.tasy5kg.cutegif.MyConstants.OUTPUT_GIF_TEMP_PATH
import me.tasy5kg.cutegif.MyConstants.TASK_BUILDER_VIDEO_TO_GIF
import me.tasy5kg.cutegif.MyConstants.VIDEO_TO_GIF_EXTRACTED_FRAMES_PATH
import me.tasy5kg.cutegif.databinding.ActivityVideoToGifPerformerBinding
import me.tasy5kg.cutegif.toolbox.FileTools
import me.tasy5kg.cutegif.toolbox.FileTools.copyFile
import me.tasy5kg.cutegif.toolbox.FileTools.createNewFile
import me.tasy5kg.cutegif.toolbox.FileTools.formattedFileSize
import me.tasy5kg.cutegif.toolbox.MediaTools
import me.tasy5kg.cutegif.toolbox.Toolbox
import me.tasy5kg.cutegif.toolbox.Toolbox.constraintBy
import me.tasy5kg.cutegif.toolbox.Toolbox.getExtra
import me.tasy5kg.cutegif.toolbox.Toolbox.keepScreenOn
import me.tasy5kg.cutegif.toolbox.Toolbox.logRed
import me.tasy5kg.cutegif.toolbox.Toolbox.onClick
import kotlin.concurrent.thread
import kotlin.math.max

class VideoToGifPerformerActivityOptimization : BaseActivity() {
  private val binding by lazy { ActivityVideoToGifPerformerBinding.inflate(layoutInflater) }
  private var taskThread: Thread? = null
  private var taskQuitOrFailed = false
  private val taskBuilder by lazy { intent.getExtra<TaskBuilderVideoToGif>(TASK_BUILDER_VIDEO_TO_GIF) }
  private var previousUpdatedFileSize = 0L

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
    taskThread = thread { performPart1() }
  }

  private fun performPart1() {
    putProgress(0, getString(R.string.exporting_gif_))
    FileTools.makeDirEmpty(VIDEO_TO_GIF_EXTRACTED_FRAMES_PATH)
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
      putProgress(
        (statistics.videoFrameNumber * 80 / taskBuilder.getOutputFramesEstimated()).constraintBy(0..80), getString(R.string.exporting_gif_)
      )
    })
  }

  private fun performPart2() {
    val command = taskBuilder.getCommandCreatePalette()
    logRed("commandCreatePalette", command)
    FFmpegKit.executeAsync(command, { completeCallback ->
      when {
        completeCallback.returnCode.isValueSuccess -> performPart3()
        completeCallback.returnCode.isValueError -> quitOrFailed(getString(R.string.an_error_occurred))
      }
    }, { log -> logRed("logcallback", log.message.toString()) }, {})
  }

  private fun performPart3() {
    putProgress(90, getString(R.string.exporting_gif_))
    val command = taskBuilder.getCommandVideoToGif()
    logRed("commandVideoToGif", command)
    FFmpegKit.executeAsync(command, { completeCallback ->
      when {
        completeCallback.returnCode.isValueSuccess -> performPart4()
        completeCallback.returnCode.isValueError -> quitOrFailed(getString(R.string.an_error_occurred))
      }
    }, { log -> logRed("logcallback", log.message.toString()) }, { statistics ->
      previousUpdatedFileSize = max(previousUpdatedFileSize, statistics.size)
      putProgress(
        (90 + statistics.videoFrameNumber * 10 / taskBuilder.getOutputFramesEstimated()).constraintBy(90..100),
        getString(R.string.exporting_gif_) + getString(R.string.____brackets____, statistics.size.formattedFileSize())
      )
    })
  }

  private fun performPart4() {
    with(taskBuilder) {
      lossy?.let {
        putProgress(null, getString(R.string.compressing_gif_raw_size, previousUpdatedFileSize.formattedFileSize()))
        MediaTools.gifsicleLossy(it, OUTPUT_GIF_TEMP_PATH, null, true)
      }
      if (!taskQuitOrFailed) {
        val outputUri = createNewFile(FileTools.FileName(inputVideoPath).nameWithoutExtension, "gif")
        copyFile(OUTPUT_GIF_TEMP_PATH, outputUri, true)
        finish()
        FileSavedActivity.start(this@VideoToGifPerformerActivityOptimization, outputUri)
      }
    }
  }

  private fun putProgress(progress: Int?, text: String) {
    runOnUiThread {
      binding.linearProgressIndicator.apply {
        if (progress == null) {
          isIndeterminate = true
        } else {
          isIndeterminate = false
          setProgress(progress.constraintBy(0..100), true)
        }
      }
      binding.mtvTitle.text = text
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
    fun start(context: Context, taskBuilderVideoToGif: TaskBuilderVideoToGif) =
      context.startActivity(Intent(context, VideoToGifPerformerActivityOptimization::class.java).apply {
        putExtra(TASK_BUILDER_VIDEO_TO_GIF, taskBuilderVideoToGif)
      })
  }
}