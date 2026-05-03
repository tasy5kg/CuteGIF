package com.nht.gif

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegKitConfig
import com.nht.gif.MyConstants.EXTRA_TASK_BUILDER_VIDEO_TO_GIF
import com.nht.gif.MyConstants.OUTPUT_GIF_TEMP_PATH
import com.nht.gif.MyConstants.OUTPUT_WEBP_TEMP_PATH
import com.nht.gif.MyConstants.VIDEO_TO_GIF_EXTRACTED_FRAMES_PATH
import com.nht.gif.model.OutputFormat
import com.nht.gif.databinding.ActivityVideoToGifPerformerBinding
import com.nht.gif.toolbox.FileTools
import com.nht.gif.toolbox.FileTools.copyFile
import com.nht.gif.toolbox.FileTools.createNewFile
import com.nht.gif.toolbox.FileTools.formattedFileSize
import com.nht.gif.toolbox.MediaTools
import com.nht.gif.toolbox.Toolbox
import com.nht.gif.toolbox.Toolbox.constraintBy
import com.nht.gif.toolbox.Toolbox.getExtra
import com.nht.gif.toolbox.Toolbox.keepScreenOn
import com.nht.gif.toolbox.Toolbox.logRed
import com.nht.gif.toolbox.Toolbox.onClick
import kotlin.concurrent.thread
import kotlin.math.max

class VideoToGifPerformerActivity : BaseActivity() {
  private val binding by lazy { ActivityVideoToGifPerformerBinding.inflate(layoutInflater) }
  private var taskThread: Thread? = null
  private var taskQuitOrFailed = false
  private val taskBuilder by lazy { intent.getExtra<TaskBuilderVideoToGif>(EXTRA_TASK_BUILDER_VIDEO_TO_GIF) }
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
    val label = if (taskBuilder.outputFormat == OutputFormat.ANIMATED_WEBP)
      getString(R.string.exporting_webp_) else getString(R.string.exporting_gif_)
    putProgress(0, label)
    FileTools.resetDirectory(VIDEO_TO_GIF_EXTRACTED_FRAMES_PATH)
    val command = taskBuilder.getCommandExtractFrame()
    logRed("CommandExtractFrame", command)
    FFmpegKit.executeAsync(command, { completeCallback ->
      when {
        completeCallback.returnCode.isValueSuccess ->
          if (taskBuilder.outputFormat == OutputFormat.ANIMATED_WEBP) performWebpEncoding()
          else performPart2()
        completeCallback.returnCode.isValueError -> quitOrFailed(getString(R.string.an_error_occurred))
      }
    }, { logCallback ->
      logRed("logcallback", logCallback.message.toString())
    }, { statistics ->
      putProgress(
        (statistics.videoFrameNumber * 40 / taskBuilder.getOutputFramesEstimated()).constraintBy(0..40), label
      )
    })
  }

  private fun performWebpEncoding() {
    putProgress(null, getString(R.string.exporting_webp_))
    val command = taskBuilder.getCommandVideoToWebp()
    logRed("commandVideoToWebp", command)
    FFmpegKit.executeAsync(command, { completeCallback ->
      when {
        completeCallback.returnCode.isValueSuccess -> performWebpSave()
        completeCallback.returnCode.isValueError -> quitOrFailed(getString(R.string.an_error_occurred))
      }
    }, { log -> logRed("logcallback", log.message.toString()) }, {})
  }

  private fun performWebpSave() {
    if (!taskQuitOrFailed) {
      val outputUri = createNewFile(FileTools.FileName(taskBuilder.inputVideoPath).nameWithoutExtension, "webp")
      copyFile(OUTPUT_WEBP_TEMP_PATH, outputUri, true)
      finish()
      FileSavedActivity.start(this@VideoToGifPerformerActivity, outputUri)
    }
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
    putProgress(60, getString(R.string.exporting_gif_))
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
        (60 + statistics.videoFrameNumber * 40 / taskBuilder.getOutputFramesEstimated()).constraintBy(60..100),
        getString(R.string.exporting_gif_) + getString(R.string.____brackets____, statistics.size.formattedFileSize())
      )
    })
  }

  private fun performPart4() {
    with(taskBuilder) {
      lossy?.let {
        putProgress(null, getString(R.string.compressing_gif_raw_size, previousUpdatedFileSize.formattedFileSize()))
        logRed("gifsicleLossy", "start rtime")
        MediaTools.gifsicleLossy(it, OUTPUT_GIF_TEMP_PATH, null, true)
        logRed("gifsicleLossy", "end rtime")
      }
      if (!taskQuitOrFailed) {
        val outputUri = createNewFile(FileTools.FileName(inputVideoPath).nameWithoutExtension, "gif")
        copyFile(OUTPUT_GIF_TEMP_PATH, outputUri, true)
        finish()
        FileSavedActivity.start(this@VideoToGifPerformerActivity, outputUri)
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
      context.startActivity(Intent(context, VideoToGifPerformerActivity::class.java).apply {
        putExtra(EXTRA_TASK_BUILDER_VIDEO_TO_GIF, taskBuilderVideoToGif)
      })
  }
}