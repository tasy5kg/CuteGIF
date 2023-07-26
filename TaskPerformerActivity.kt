package me.tasy5kg.cutegif

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegKitConfig
import com.arthenica.ffmpegkit.FFprobeKit
import java.io.Serializable
import java.lang.Integer.min
import kotlin.concurrent.thread
import kotlin.math.ceil
import kotlin.math.roundToInt
import me.tasy5kg.cutegif.MyApplication.Companion.appContext
import me.tasy5kg.cutegif.MyConstants.ADD_TEXT_RENDER_PNG_PATH
import me.tasy5kg.cutegif.MyConstants.EXTRA_TASK_BUILDER
import me.tasy5kg.cutegif.MyConstants.FFMPEG_COMMAND_PREFIX_FOR_ALL
import me.tasy5kg.cutegif.MyConstants.OUTPUT_GIF_TEMP_PATH
import me.tasy5kg.cutegif.MyConstants.PALETTE_PATH
import me.tasy5kg.cutegif.MyConstants.UNKNOWN_INT
import me.tasy5kg.cutegif.Toolbox.copyFile
import me.tasy5kg.cutegif.Toolbox.createFfSafForRead
import me.tasy5kg.cutegif.Toolbox.createNewFile
import me.tasy5kg.cutegif.Toolbox.formatFileSize
import me.tasy5kg.cutegif.Toolbox.getExtra
import me.tasy5kg.cutegif.Toolbox.keepScreenOn
import me.tasy5kg.cutegif.Toolbox.logRed
import me.tasy5kg.cutegif.Toolbox.saveToPng
import me.tasy5kg.cutegif.Toolbox.toEmptyStringIf
import me.tasy5kg.cutegif.Toolbox.toast
import me.tasy5kg.cutegif.Toolbox.videoDuration
import me.tasy5kg.cutegif.databinding.ActivityTaskPerformerBinding

class TaskPerformerActivity : BaseActivity() {
  private val binding by lazy { ActivityTaskPerformerBinding.inflate(layoutInflater) }
  private val linearProgressIndicator by lazy { binding.linearProgressIndicator }
  private val mtvTitle by lazy { binding.mtvTitle }
  private val mbClose by lazy { binding.mbClose }
  private var taskThread: Thread? = null
  private var taskQuitOrFailed = false

  private val requestPermissionLauncher =
    registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
      toast(
        if (isGranted) getString(R.string.permission_granted_please_continue) else getString(
          R.string.unable_to_save_your_file_without_storage_permission
        )
      )
    }

  override fun onCreateIfEulaAccepted(savedInstanceState: Bundle?) {
    setContentView(binding.root)
    setFinishOnTouchOutside(false)
    if ((Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
          && ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
      ) != PERMISSION_GRANTED)
    ) {
      requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
      quitOrFailed(null)
      return
    }
    executeTask(intent.getExtra(EXTRA_TASK_BUILDER))
    onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
      override fun handleOnBackPressed() {
        quitOrFailed("已取消")
      }
    })
    mbClose.setOnClickListener {
      quitOrFailed("已取消")
    }
  }

  private fun executeTask(taskBuilderBase: TaskBase) {
    taskThread = thread {
      keepScreenOn(true)
      if (taskBuilderBase is VideoToGifTaskBuilderBase2) executeTaskVideoToGif(taskBuilderBase)
      else throw NotImplementedError(taskBuilderBase.toString())
    }
  } catch (e: Exception)
  {
    e.printStackTrace()
    quitOrFailed("出现错误")
  }
}

private fun executeTaskVideoToGif(taskBuilderVideoToGif: TaskBuilderVideoToGif) =
  executeTaskVideoToGifPart1Of3(taskBuilderVideoToGif)

private fun putProgress(stateText: String, progress: Int?, fileSize: Long?) {
  runOnUiThread {
    val progressNoLargerThan99 = progress?.let { min(progress, 99) }
    linearProgressIndicator.apply {
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
    mtvTitle.text =
      stateText + if (fileSize == null) "..." else "（${fileSize.formatFileSize()}）"
  }
}

private fun quitOrFailed(toastText: String?) {
  runOnUiThread {
    taskQuitOrFailed = true
    keepScreenOn(false)
    toastText?.let { toast(it) }
    FFmpegKit.cancel()
    FFmpegKitConfig.clearSessions()
    taskThread?.interrupt()
    finish()
  }
}

private fun executeTaskVideoToGifPart1Of3(taskBuilderVideoToGif: TaskPerformerActivity2.Companion.TaskBuilderVideoToGif2) {
  with(taskBuilderVideoToGif) {
    val inputVideoUri = inputVideoUriWrapper.getUri()
    putProgress("[1/3] 正在读取文件", null, null)
    (textRender?.toBitmap(videoWH.first, videoWH.second)
      ?: Toolbox.generate1x1TransparentBitmap().saveToPng(ADD_TEXT_RENDER_PNG_PATH))
    val trimAndSkipFrameCommandInCreatePalette =
      ("-skip_frame nokey ")
    // make sure at least 1 key frame in trimTimeKeyFrameStart..trimTimeKeyFrameEnd
    val videoKeyFramesTimestampList = videoKeyFramesTimestampList(inputVideoUri)
    val trimTimeKeyFrameStart =
      videoKeyFramesTimestampList.filter { it < trimTime.first }.maxOrNull() ?: 0
    val trimTimeKeyFrameEnd =
      videoKeyFramesTimestampList.filter { it > trimTime.second }.minOrNull()
        ?: inputVideoUri.videoDuration()
    "-ss ${trimTimeKeyFrameStart}ms -to ${trimTimeKeyFrameEnd}ms "
  }
  val commandCreatePalette =
    "$FFMPEG_COMMAND_PREFIX_FOR_ALL " +
        trimAndSkipFrameCommandInCreatePalette +
        "-i ${inputVideoUri.createFfSafForRead()} " +
        "-i $ADD_TEXT_RENDER_PNG_PATH " +
        "-filter_complex ${cropParams.toFFmpegCropCommand()}${transposeParams(rotation)}" +
        (",overlay=0:0") +
        "${resolutionParams(cropParams, resolutionShortLength, rotation)}," +
        "palettegen=max_colors=${colorQuality}:stats_mode=diff -y $PALETTE_PATH"
  logRed("commandCreatePalette", commandCreatePalette)
  val commandCreatePaletteReturnCode = FFmpegKit.execute(commandCreatePalette).returnCode
  when {
    commandCreatePaletteReturnCode.isValueSuccess -> executeTaskVideoToGifPart2Of3(
      taskBuilderVideoToGif
    )

    commandCreatePaletteReturnCode.isValueError -> quitOrFailed("出现错误")
  }
}
}

private fun executeTaskVideoToGifPart2Of3(taskBuilderVideoToGif: TaskBuilderVideoToGif) {
  with(taskBuilderVideoToGif) {
    val inputVideoUri = inputVideoUriWrapper.getUri()
    val outputFramesEstimated =
      ceil((if (trimTime == null) inputVideoUri.videoDuration() else (trimTime.second - trimTime.first)) * outputFps / outputSpeed / 1000f).toInt()
    val commandVideoToGif =
      "$FFMPEG_COMMAND_PREFIX_FOR_ALL " +
          (if (trimTime == null) "" else "-ss ${trimTime.first}ms -to ${trimTime.second}ms ") +
          "-i ${inputVideoUri.createFfSafForRead()} -i $PALETTE_PATH -i $ADD_TEXT_RENDER_PNG_PATH " +
          "-filter_complex \"[0:v] setpts=PTS/$outputSpeed,fps=fps=${outputFps}," +
          "${cropParams.toFFmpegCropCommand()}${transposeParams(rotation)}${(",reverse").toEmptyStringIf { !reverse }} [0vPreprocessed];" +
          "[0vPreprocessed][2:v] overlay=0:0${
            resolutionParams(
              cropParams,
              resolutionShortLength,
              rotation
            )
          } [videoWithText]; " +
          "[videoWithText][1:v] paletteuse=dither=bayer\" -final_delay ${MySettings.gifFinalDelay} -y $OUTPUT_GIF_TEMP_PATH"
    logRed("commandVideoToGif", commandVideoToGif)
    FFmpegKit.executeAsync(commandVideoToGif, { ffmpegVideoToGifSession ->
      val commandVideoToGifReturnCode = ffmpegVideoToGifSession.returnCode
      when {
        commandVideoToGifReturnCode.isValueSuccess -> executeTaskVideoToGifPart3Of3(
          taskBuilderVideoToGif
        )

        commandVideoToGifReturnCode.isValueError -> quitOrFailed("出现错误")
      }
    }, { log -> logRed("logcallback", log.message.toString()) }, {
      putProgress(
        "[2/3] 正在导出 GIF",
        it.videoFrameNumber * 100 / outputFramesEstimated,
        null
      )
    })
  }
}

private fun executeTaskVideoToGifPart3Of3(taskBuilderVideoToGif: TaskBuilderVideoToGif) {
  with(taskBuilderVideoToGif) {
    val inputVideoUri = inputVideoUriWrapper.getUri()
    putProgress("[3/3] 正在压缩 GIF", null, null)
    when (gifsicleLossy(lossy, OUTPUT_GIF_TEMP_PATH)) {
      true -> {
        if (!taskQuitOrFailed) {
          val outputUri = createNewFile(inputVideoUri, "gif")
          copyFile(OUTPUT_GIF_TEMP_PATH, outputUri, true)
          finish()
          FileSavedActivity.start(this@TaskPerformerActivity, outputUri)
        }
      }

      false -> {
        quitOrFailed("出现错误")
      }
    }
  }
}

companion object {

  data class UriWrapper(val uriString: String) : Serializable {
    constructor(uri: Uri) : this(uri.toString())

    fun getUri() = Uri.parse(uriString)!!
  }

  fun start(context: Context, taskBuilderBase: TaskBuilderBase) =
    context.startActivity(
      Intent(context, TaskPerformerActivity::class.java)
        .putExtra(EXTRA_TASK_BUILDER, taskBuilderBase)
    )

  private fun gifsicleLossy(
    lossy: Int,
    inputGifPath: String,
    outputGifPath: String? = null
  ): Boolean {
    val nativeLibraryDir = appContext.applicationInfo.nativeLibraryDir
    val gifsiclePath = "${nativeLibraryDir}/libgifsicle.so"
    val gifsicleEnvp = arrayOf("LD_LIBRARY_PATH=${nativeLibraryDir}")
    val gifsicleCmd =
      if (outputGifPath == null) "$gifsiclePath -b -O3 --lossy=$lossy $inputGifPath" else "$gifsiclePath -O3 --lossy=$lossy --output $outputGifPath $inputGifPath"
    logRed("gifsicleCmd", gifsicleCmd)
    logRed("gifsicleEnvp", gifsicleEnvp.joinToString())
    return try {
      (Runtime.getRuntime().exec(gifsicleCmd, gifsicleEnvp).waitFor() == 0)
    } catch (e: Exception) {
      logRed("gifsicleLossy() failed", e.message)
      false
    }
  }

  // Slow operation: this function may takes >5 sec
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
          UNKNOWN_INT
        }
      }.filter { it != UNKNOWN_INT }

  /**
  lossy should >= 0 .
  return true when succeed, false when failed.
  if outputGifPath is null, then output will overwrite input file.
   */

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

  private fun resolutionParams(cropParams: CropParams, widthAndHeight: Pair<Int, Int>): String {
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
}

data class TaskBuilderVideoToGif(
  val taskType = MyConstants.TASK_TYPE_VIDEO_TO_GIF
  val inputVideoUriWrapper: Toolbox.UriWrapper,
  val trimTime: Pair<Int, Int>,
  val cropParams: CropParams,
  val resolutionPair: Pair<Int, Int>,
  val outputSpeed: Float,
  val outputFps: Int,
  val colorQuality: Int,
  val reverse: Boolean,
  val textRender: TextRender2?,
  val lossy: Int?,
  val videoWH: Pair<Int, Int>,
) : Serializable

