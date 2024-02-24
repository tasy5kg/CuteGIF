package me.tasy5kg.cutegif

import me.tasy5kg.cutegif.MyConstants.ADD_TEXT_RENDER_PNG_PATH
import me.tasy5kg.cutegif.MyConstants.FFMPEG_COMMAND_PREFIX_FOR_ALL_AN
import me.tasy5kg.cutegif.MyConstants.VIDEO_TO_GIF_SKI_EXTRACTED_FRAMES_PATH
import me.tasy5kg.cutegif.toolbox.MediaTools.saveToPng
import me.tasy5kg.cutegif.toolbox.Toolbox.toEmptyStringIf
import java.io.Serializable
import kotlin.math.ceil

data class TaskBuilderVideoToGifSki(
  val inputVideoPath: String,
  /** 1 == 1ms , do not use IntRange because it is not serializable */
  val trimTime: Pair<Int, Int>?,
  val cropParams: CropParams,
  val width: Int,
  val height: Int,
  val outputSpeed: Float,
  val outputFps: Int,
  val gifQuality: Int,
  val reverse: Boolean,
  val textRender: TextRender?,
  val videoWH: Pair<Int, Int>,
  val duration: Int,
  /** Color(RRGGBB), Similarity * 100, Blend * 100 */
  val colorKey: Triple<String, Int, Int>?
) : Serializable {

  init {
    TextRender.render(textRender, videoWH.first, videoWH.second).saveToPng(ADD_TEXT_RENDER_PNG_PATH)
  }

  fun getCommandExtractFrame() =
    "$FFMPEG_COMMAND_PREFIX_FOR_ALL_AN ${trimTime?.let { "-ss ${trimTime.first}ms -to ${trimTime.second}ms " } ?: ""} -i \"$inputVideoPath\" -i \"$ADD_TEXT_RENDER_PNG_PATH\" " +
      "-filter_complex \"[0:v] setpts=PTS/$outputSpeed,fps=fps=$outputFps [0vPreprocessed]; " +
      "[0vPreprocessed][1:v] overlay=0:0," + cropParams.toFFmpegCropCommand() + ",scale=$width:$height:flags=lanczos" +
      (colorKey?.let { ",colorkey=#${it.first}:${it.second / 100f}:${it.third / 100f}" } ?: "") + (",reverse").toEmptyStringIf { !reverse } +
      "\" \"${VIDEO_TO_GIF_SKI_EXTRACTED_FRAMES_PATH}%05d.png\""


  fun getCommandGifski() = ""

  fun getOutputFramesEstimated() = ceil((trimTime?.let { it.second - it.first } ?: duration) * outputFps / outputSpeed / 1000.0).toInt()

}