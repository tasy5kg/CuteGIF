package me.tasy5kg.cutegif

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegKitConfig
import com.arthenica.ffmpegkit.FFprobeKit
import kotlin.concurrent.thread
import kotlin.math.ceil
import kotlin.math.min
import me.tasy5kg.cutegif.MyConstants.ADD_TEXT_RENDER_PNG_PATH
import me.tasy5kg.cutegif.MyConstants.FFMPEG_COMMAND_PREFIX_FOR_ALL_AN
import me.tasy5kg.cutegif.MyConstants.OUTPUT_GIF_TEMP_PATH
import me.tasy5kg.cutegif.MyConstants.PALETTE_PATH
import me.tasy5kg.cutegif.MyConstants.TASK_BUILDER_VIDEO_TO_GIF
import me.tasy5kg.cutegif.Toolbox.createFfSafForRead
import me.tasy5kg.cutegif.Toolbox.formatFileSize
import me.tasy5kg.cutegif.Toolbox.getExtra
import me.tasy5kg.cutegif.Toolbox.keepScreenOn
import me.tasy5kg.cutegif.Toolbox.logRed
import me.tasy5kg.cutegif.Toolbox.logRedElapsedTime
import me.tasy5kg.cutegif.Toolbox.onClick
import me.tasy5kg.cutegif.Toolbox.saveToPng
import me.tasy5kg.cutegif.Toolbox.toEmptyStringIf
import me.tasy5kg.cutegif.Toolbox.videoDuration
import me.tasy5kg.cutegif.databinding.ActivityVideoToGifPerformerBinding

class VideoToGifPerformerActivity : BaseActivity() {

  private val binding by lazy { ActivityVideoToGifPerformerBinding.inflate(layoutInflater) }
  private var taskThread: Thread? = null
  private var taskQuitOrFailed = false
  private val taskBuilderVideoToGif by lazy {
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
    with(taskBuilderVideoToGif) {
      val inputVideoUri = inputVideoUriWrapper.getUri()
      putProgress("正在读取视频", null, null)
      (textRender?.toBitmap(videoWH.first, videoWH.second) ?: Toolbox.generateTransparentBitmap(1, 1)).saveToPng(ADD_TEXT_RENDER_PNG_PATH)
      val trimTimeCommand = when {
        trimTime == null -> ""
        trimTime.first == 0 -> "-ss 0ms -to ${trimTime.second}ms "
        else -> "-ss ${videoKeyFramesTimestampList(inputVideoUri).findLast { it <= trimTime.first }}ms -to ${trimTime.second}ms "
      }
      val commandCreatePalette =
        "$FFMPEG_COMMAND_PREFIX_FOR_ALL_AN -skip_frame nokey $trimTimeCommand" +
            "-i ${inputVideoUri.createFfSafForRead()} " +
            "-i $ADD_TEXT_RENDER_PNG_PATH " +
            "-filter_complex overlay=0:0,${cropParams.toFFmpegCropCommand()}" +
            "${resolutionParams(cropParams, resolutionShortLength)}," +
            "palettegen=max_colors=${colorQuality}:stats_mode=diff -y $PALETTE_PATH"
      logRed("commandCreatePalette", commandCreatePalette)
      FFmpegKit.executeAsync(commandCreatePalette, { completeCallback ->
        when {
          completeCallback.returnCode.isValueSuccess -> performPart2()
          completeCallback.returnCode.isValueError -> quitOrFailed("出现错误")
        }
      }, { logCallback ->
        logRed("logcallback", logCallback.message.toString())
      }, { _ -> })
    }
  }

  private fun performPart2() {
    with(taskBuilderVideoToGif) {
      val inputVideoUri = inputVideoUriWrapper.getUri()
      val outputFramesEstimated = ceil(
        (if (trimTime == null) inputVideoUri.videoDuration()
        else (trimTime.second - trimTime.first)) * outputFps / outputSpeed / 1000f
      ).toInt()
      val commandVideoToGif =
        "$FFMPEG_COMMAND_PREFIX_FOR_ALL_AN " +
            (if (trimTime == null) "" else "-ss ${trimTime.first}ms -to ${trimTime.second}ms ") +
            "-i ${inputVideoUri.createFfSafForRead()} -i $ADD_TEXT_RENDER_PNG_PATH -i $PALETTE_PATH " +
            "-filter_complex \"[0:v] setpts=PTS/$outputSpeed,fps=fps=${outputFps}" +
            "${(",reverse").toEmptyStringIf { !reverse }} [0vPreprocessed];" +
            "[0vPreprocessed][1:v] overlay=0:0,${cropParams.toFFmpegCropCommand()}" +
            "${resolutionParams(cropParams, resolutionShortLength)} [videoWithText]; " +
            "[videoWithText][2:v] paletteuse=dither=bayer\" -final_delay $finalDelay -y $OUTPUT_GIF_TEMP_PATH"
      logRed("commandVideoToGif", commandVideoToGif)
      FFmpegKit.executeAsync(commandVideoToGif, { ffmpegVideoToGifSession ->
        val commandVideoToGifReturnCode = ffmpegVideoToGifSession.returnCode
        when {
          commandVideoToGifReturnCode.isValueSuccess -> performPart3()
          commandVideoToGifReturnCode.isValueError -> quitOrFailed("出现错误")
        }
      }, { log -> logRed("logcallback", log.message.toString()) }, {
        putProgress("正在生成 GIF", it.videoFrameNumber * 100 / outputFramesEstimated, null)
      })
    }
  }

  private fun performPart3() {
    with(taskBuilderVideoToGif) {
      val inputVideoUri = inputVideoUriWrapper.getUri()
      putProgress("正在压缩 GIF", null, null)
      logRedElapsedTime("gifsicleLossy") {
        when (Toolbox.gifsicleLossy(lossy, OUTPUT_GIF_TEMP_PATH, null, true)) {
          true -> {
            if (!taskQuitOrFailed) {
              val outputUri = Toolbox.createNewFile(inputVideoUri, "gif")
              Toolbox.copyFile(OUTPUT_GIF_TEMP_PATH, outputUri, true)
              finish()
              FileSavedActivity.start(this@VideoToGifPerformerActivity, outputUri)
            }
          }

          false -> {
            quitOrFailed("出现错误")
          }
        }
      }
    }
  }

  private fun resolutionParams(cropParams: CropParams, shortLength: Int): String {
    val short = cropParams.shortLength()
    val pixel = min(shortLength, short)
    return if (shortLength == 0 || shortLength >= short) {
      ""
    } else {
      ",scale=" +
          if ((cropParams.outW > cropParams.outH)) {
            "-2:$pixel"
          } else {
            "$pixel:-2"
          } + ":flags=lanczos"
    }
  }

  private fun putProgress(stateText: String, progress: Int?, fileSize: Long?) {
    runOnUiThread {
      val progressNoLargerThan99 = progress?.let { min(progress, 99) }
      binding.linearProgressIndicator.apply {
        when (progressNoLargerThan99) {
          null -> {
            isIndeterminate = true
          }

          else -> {
            isIndeterminate = false
            setProgress(min(progressNoLargerThan99, 99), true)
          }
        }
      }
      binding.mtvTitle.text =
        stateText + if (fileSize == null) "..." else "（${fileSize.formatFileSize()}）"
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
    // Slow operation: this function may takes at least 5s.

    private fun videoKeyFramesTimestampList(videoUri: Uri) =
      FFprobeKit.execute(
        "-loglevel error -skip_frame nokey -select_streams v:0 -show_entries frame=pts_time ${videoUri.createFfSafForRead()}"
      ).allLogsAsString
        .split("\n")
        .filter { it.startsWith("pts_time=") }
        .map { ((it.split('=')[1]).toFloat() * 1000f).toInt() }
  }
}