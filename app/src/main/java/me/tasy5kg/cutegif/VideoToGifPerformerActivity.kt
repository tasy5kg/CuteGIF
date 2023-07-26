package me.tasy5kg.cutegif

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegKitConfig
import com.arthenica.ffmpegkit.FFprobeKit
import me.tasy5kg.cutegif.MyApplication.Companion.appContext
import me.tasy5kg.cutegif.MyConstants.TASK_BUILDER_VIDEO_TO_GIF
import me.tasy5kg.cutegif.Toolbox.createFfSafForRead
import me.tasy5kg.cutegif.Toolbox.formatFileSize
import me.tasy5kg.cutegif.Toolbox.getExtra
import me.tasy5kg.cutegif.Toolbox.keepScreenOn
import me.tasy5kg.cutegif.Toolbox.logRed
import me.tasy5kg.cutegif.Toolbox.onClick
import me.tasy5kg.cutegif.Toolbox.saveToPng
import me.tasy5kg.cutegif.Toolbox.toEmptyStringIf
import me.tasy5kg.cutegif.Toolbox.videoDuration
import me.tasy5kg.cutegif.databinding.ActivityVideoToGifPerformerBinding
import kotlin.concurrent.thread
import kotlin.math.ceil
import kotlin.math.roundToInt

class VideoToGifPerformerActivity : BaseActivity() {

  private val binding by lazy { ActivityVideoToGifPerformerBinding.inflate(layoutInflater) }
  private val linearProgressIndicator by lazy { binding.linearProgressIndicator }
  private val mtvTitle by lazy { binding.mtvTitle }
  private val mbClose by lazy { binding.mbClose }
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
    mbClose.onClick {
      quitOrFailed("已取消")
    }
    taskThread = thread { performPart1() }
  }

  private fun performPart1() {
    with(taskBuilderVideoToGif) {
      val inputVideoUri = inputVideoUriWrapper.getUri()
      putProgress("正在读取视频", null, null)
      (textRender?.toBitmap(videoWH.first, videoWH.second) ?: Toolbox.generateTransparentBitmap(1, 1))
        .saveToPng(MyConstants.ADD_TEXT_RENDER_PNG_PATH)
      val commandCreatePalette =
        "${MyConstants.FFMPEG_COMMAND_PREFIX_FOR_ALL_AN} " +
            (if (trimTime != null) "-ss ${trimTime.first}ms -to ${trimTime.second}ms " else "") +
            "-i ${inputVideoUri.createFfSafForRead()} " +
            "-i ${MyConstants.ADD_TEXT_RENDER_PNG_PATH} " +
            "-filter_complex ${cropParams.toFFmpegCropCommand()}" +
            (",overlay=0:0") +
            "${resolutionParams(cropParams, resolutionShortLength)}," +
            "palettegen=max_colors=${colorQuality}:stats_mode=diff -y ${MyConstants.PALETTE_PATH}"
      logRed("commandCreatePalette", commandCreatePalette)
      FFmpegKit.executeAsync(commandCreatePalette) {
        when {
          it.returnCode.isValueSuccess -> performPart2()
          it.returnCode.isValueError -> quitOrFailed("出现错误")
        }
      }
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
        "${MyConstants.FFMPEG_COMMAND_PREFIX_FOR_ALL_AN} " +
            when (trimTime) {
              null -> ""
              else -> "-ss ${trimTime.first}ms -to ${trimTime.second}ms "
            } +
            "-i ${inputVideoUri.createFfSafForRead()} -i ${MyConstants.PALETTE_PATH} -i ${MyConstants.ADD_TEXT_RENDER_PNG_PATH} " +
            "-filter_complex \"[0:v] setpts=PTS/$outputSpeed,fps=fps=${outputFps}," +
            "${cropParams.toFFmpegCropCommand()}${(",reverse").toEmptyStringIf { !reverse }} [0vPreprocessed];" +
            "[0vPreprocessed][2:v] overlay=0:0${
              resolutionParams(cropParams, resolutionShortLength)
            } [videoWithText]; " +
            "[videoWithText][1:v] paletteuse=dither=bayer\" -final_delay ${MySettings.gifFinalDelay} -y ${MyConstants.OUTPUT_GIF_TEMP_PATH}"
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
      when (gifsicleLossy(lossy, MyConstants.OUTPUT_GIF_TEMP_PATH)) {
        true -> {
          if (!taskQuitOrFailed) {
            val outputUri = Toolbox.createNewFile(inputVideoUri, "gif")
            Toolbox.copyFile(MyConstants.OUTPUT_GIF_TEMP_PATH, outputUri, true)
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

  /**
  lossy should >= 0 .
  return true when succeed, false when failed.
  if outputGifPath is null, then output will overwrite input file.
   */
  private fun gifsicleLossy(
    lossy: Int,
    inputGifPath: String,
    outputGifPath: String? = null
  ): Boolean {
    val nativeLibraryDir = appContext.applicationInfo.nativeLibraryDir
    val gifsiclePath = "${nativeLibraryDir}/libgifsicle.so"
    val gifsicleEnvp = arrayOf("LD_LIBRARY_PATH=${nativeLibraryDir}")
    val gifsicleCmd = when (outputGifPath) {
      null -> "$gifsiclePath -b -O3 --lossy=$lossy $inputGifPath"
      else -> "$gifsiclePath -O3 --lossy=$lossy --output $outputGifPath $inputGifPath"
    }
    logRed("gifsicleCmd", gifsicleCmd)
    logRed("gifsicleEnvp", gifsicleEnvp.joinToString())
    return try {
      (Runtime.getRuntime().exec(gifsicleCmd, gifsicleEnvp).waitFor() == 0)
    } catch (e: Exception) {
      logRed("gifsicleLossy() failed", e.message)
      false
    }
  }

  private fun resolutionParams(cropParams: CropParams, shortLength: Int): String {
    val short = cropParams.shortLength()
    val pixel = Integer.min(shortLength, short)
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
      val progressNoLargerThan99 = progress?.let { Integer.min(progress, 99) }
      linearProgressIndicator.apply {
        when (progressNoLargerThan99) {
          null -> {
            isIndeterminate = true
          }

          else -> {
            isIndeterminate = false
            setProgress(Integer.min(progressNoLargerThan99, 99), true)
          }
        }
      }
      mtvTitle.text =
        stateText + if (fileSize == null) "..." else "（${fileSize.formatFileSize()}）"
    }
  }


  @Deprecated("Slow operation: this function may takes at least 5s.")
  private fun videoKeyFramesTimestampList(videoUri: Uri) =
    FFprobeKit.execute(
      "-loglevel error -skip_frame nokey -select_streams v:0 -show_entries frame=pts_time -of csv=p=0:sv=fail ${videoUri.createFfSafForRead()}"
    )
      .allLogsAsString
      .split("\n")
      .map {
        try {
          (it.toFloat() * 1000f).roundToInt() // 1 == 1ms
        } catch (e: NumberFormatException) {
          MyConstants.UNKNOWN_INT
        }
      }.filter { it != MyConstants.UNKNOWN_INT }

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