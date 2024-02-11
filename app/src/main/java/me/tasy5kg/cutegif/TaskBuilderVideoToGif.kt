package me.tasy5kg.cutegif

import me.tasy5kg.cutegif.MyConstants.ADD_TEXT_RENDER_PNG_PATH
import me.tasy5kg.cutegif.MyConstants.FFMPEG_COMMAND_PREFIX_FOR_ALL_AN
import me.tasy5kg.cutegif.toolbox.MediaTools
import me.tasy5kg.cutegif.toolbox.MediaTools.saveToPng
import me.tasy5kg.cutegif.toolbox.Toolbox.toEmptyStringIf
import java.io.Serializable
import kotlin.math.ceil
import kotlin.math.min

data class TaskBuilderVideoToGif(
  val inputVideoPath: String,
  /** 1 == 1ms , do not use IntRange because it is not serializable */
  val trimTime: Pair<Int, Int>?,
  val cropParams: CropParams,
  /** 输出时短边的分辨率 */
  val shortLength: Int,
  val outputSpeed: Float,
  val outputFps: Int,
  val colorQuality: Int,
  val reverse: Boolean,
  val textRender: TextRender?,
  val lossy: Int?,
  val videoWH: Pair<Int, Int>,
  val duration: Int,
  /** The interval between every loops, in centiseconds. (1 == 0.01 sec) */
  val finalDelay: Int,
  /** Color(RRGGBB), Similarity * 100, Blend * 100 */
  val colorKey: Triple<String, Int, Int>?
) : Serializable {

  init {
    TextRender.render(textRender, videoWH.first, videoWH.second).saveToPng(ADD_TEXT_RENDER_PNG_PATH)
  }

  fun getForPreviewOnly() = TaskBuilderVideoToGifForPreview(
    shortLength, colorQuality, lossy, videoWH, colorKey
  )

  fun getSkipNoKeyFrame() =
    duration >= INPUT_VIDEO_TRIMMED_DURATION_MIN_TO_ENABLE_SKIP_NO_KEY_FRAME || (trimTime != null && trimTime.second - trimTime.first >= INPUT_VIDEO_TRIMMED_DURATION_MIN_TO_ENABLE_SKIP_NO_KEY_FRAME)

  fun getTrimTimeCommandForCreatePalette() = "-skip_frame nokey ".toEmptyStringIf { !getSkipNoKeyFrame() } + when {
    trimTime == null -> ""
    trimTime.first == 0 -> "-ss 0ms -to ${trimTime.second}ms "
    else -> when (getSkipNoKeyFrame()) {
      true -> "-ss ${
        MediaTools.videoKeyFramesTimestampList(inputVideoPath).findLast { it <= trimTime.first }
      }ms -to ${trimTime.second}ms "

      false -> "-ss ${trimTime.first}ms -to ${trimTime.second}ms "
    }
  }

  fun getTrimTimeCommandForVideoToGif() = trimTime?.let { "-ss ${trimTime.first}ms -to ${trimTime.second}ms " } ?: ""

  fun getOutputFramesEstimated() = ceil((trimTime?.let { it.second - it.first } ?: duration) * outputFps / outputSpeed / 1000.0).toInt()

  private fun resolutionParams(cropParams: CropParams, shortLength: Int): String {
    val short = cropParams.shortLength()
    val pixel = min(shortLength, short)
    return if (shortLength == 0 || shortLength >= short) {
      ""
    } else {
      ",scale=" + if ((cropParams.outW > cropParams.outH)) {
        "-2:$pixel"
      } else {
        "$pixel:-2"
      } + ":flags=lanczos"
    }
  }

  fun getCommand_Overlay_CropParams_ResolutionParams_ColorKey() = "overlay=0:0," + cropParams.toFFmpegCropCommand() + resolutionParams(
    cropParams, shortLength
  ) + (colorKey?.let { ",colorkey=#${it.first}:${it.second / 100f}:${it.third / 100f}" } ?: "")

  fun getCommandCreatePalette() =
    "$FFMPEG_COMMAND_PREFIX_FOR_ALL_AN ${getTrimTimeCommandForCreatePalette()}" + "-i \"$inputVideoPath\" -i \"$ADD_TEXT_RENDER_PNG_PATH\" " + "-filter_complex ${getCommand_Overlay_CropParams_ResolutionParams_ColorKey()}" + ",palettegen=max_colors=${colorQuality}:stats_mode=diff -y \"${MyConstants.PALETTE_PATH}\""

  fun getCommandVideoToGif() =
    "$FFMPEG_COMMAND_PREFIX_FOR_ALL_AN " + getTrimTimeCommandForVideoToGif() + "-i \"$inputVideoPath\" -i \"$ADD_TEXT_RENDER_PNG_PATH\" -i \"${MyConstants.PALETTE_PATH}\" " + "-filter_complex \"[0:v] setpts=PTS/$outputSpeed,fps=fps=$outputFps [0vPreprocessed];" + "[0vPreprocessed][1:v] ${getCommand_Overlay_CropParams_ResolutionParams_ColorKey()} [videoWithText]; " + "[videoWithText][2:v] paletteuse=dither=bayer" + (",reverse").toEmptyStringIf { !reverse } + "\" -final_delay $finalDelay -y \"${MyConstants.OUTPUT_GIF_TEMP_PATH}\""

  companion object {
    /**
     * 输入的视频截取片段后，不考虑倍速，剩余的时长如果大于 15 秒，则在生成调色板时不跳过非关键帧，
     * 避免因为视频时长过短，导致其关键帧太少，出现过于明显的色彩失真。
     */
    private const val INPUT_VIDEO_TRIMMED_DURATION_MIN_TO_ENABLE_SKIP_NO_KEY_FRAME = 15000
  }
}