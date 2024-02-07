package me.tasy5kg.cutegif

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegKitConfig
import me.tasy5kg.cutegif.MyConstants.OUTPUT_GIF_TEMP_PATH
import me.tasy5kg.cutegif.MyConstants.TASK_BUILDER_VIDEO_TO_GIF
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
import kotlin.math.min

class VideoToGifPerformerActivity : BaseActivity() {
  private val binding by lazy { ActivityVideoToGifPerformerBinding.inflate(layoutInflater) }
  private var taskThread: Thread? = null
  private var taskQuitOrFailed = false
  private val taskBuilder by lazy {
    intent.getExtra<TaskBuilderVideoToGif>(
      TASK_BUILDER_VIDEO_TO_GIF
    )
  }

  override fun onCreateIfEulaAccepted(savedInstanceState: Bundle?) {
    setContentView(binding.root)
    setFinishOnTouchOutside(false)
    onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
      override fun handleOnBackPressed() {
        quitOrFailed("已取消")
      }
    })
    binding.mbClose.onClick {
      quitOrFailed("已取消")
    }
    taskThread = thread { performPart1() }
  }

  private fun performPart1() {
    putProgress("正在读取视频", null, null)
    val command = taskBuilder.commandCreatePalette
    logRed("commandCreatePalette", command)
    FFmpegKit.executeAsync(command, { completeCallback ->
      when {
        completeCallback.returnCode.isValueSuccess -> performPart2()
        completeCallback.returnCode.isValueError -> quitOrFailed("出现错误")
      }
    }, { logCallback ->
      logRed("logcallback", logCallback.message.toString())
    }, { _ -> })
  }

  private fun performPart2() {
    val command = taskBuilder.commandVideoToGif
    logRed("commandVideoToGif", command)
    FFmpegKit.executeAsync(command, { completeCallback ->
      val returnCode = completeCallback.returnCode
      when {
        returnCode.isValueSuccess -> performPart3()
        returnCode.isValueError -> quitOrFailed("出现错误")
      }
    }, { log -> logRed("logcallback", log.message.toString()) }, {
      putProgress(
        "正在生成 GIF",
        it.videoFrameNumber * 100 / taskBuilder.outputFramesEstimated,
        if (taskBuilder.lossy == null) it.size else null
      )
    })
  }

  private fun performPart3() {
    with(taskBuilder) {
      putProgress("正在保存 GIF", null, null)
      lossy?.let { MediaTools.gifsicleLossy(it, OUTPUT_GIF_TEMP_PATH, null, true) }
      if (!taskQuitOrFailed) {
        val outputUri = createNewFile(FileTools.FileName(inputVideoPath).nameWithoutExtension, "gif")
        copyFile(OUTPUT_GIF_TEMP_PATH, outputUri, true)
        finish()
        FileSavedActivity.start(this@VideoToGifPerformerActivity, outputUri)
      }
    }
  }


  private fun putProgress(stateText: String, progress: Int?, fileSize: Long?) {
    val progressNoLargerThan99 = progress?.constraintBy(0..99)
    runOnUiThread {
      binding.linearProgressIndicator.apply {
        if (progressNoLargerThan99 == null) {
          isIndeterminate = true
        } else {
          isIndeterminate = false
          setProgress(min(progressNoLargerThan99, 99), true)
        }
      }
      binding.mtvTitle.text = stateText + if (fileSize == null) "..." else "（${fileSize.formattedFileSize()}）"
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
        putExtra(TASK_BUILDER_VIDEO_TO_GIF, taskBuilderVideoToGif)
      })
  }
}