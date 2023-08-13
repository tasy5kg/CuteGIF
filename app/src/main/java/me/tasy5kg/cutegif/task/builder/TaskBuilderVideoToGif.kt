package me.tasy5kg.cutegif.task.builder

import me.tasy5kg.cutegif.CropParams
import me.tasy5kg.cutegif.TextRender
import me.tasy5kg.cutegif.Toolbox
import java.io.Serializable

data class TaskBuilderVideoToGif(
  val inputVideoUriWrapper: Toolbox.UriWrapper,
  val trimTime: Pair<Int, Int>,
  val cropParams: CropParams,
  val resolutionPair: Pair<Int, Int>,
  val outputSpeed: Float,
  val outputFps: Int,
  val colorQuality: Int,
  val reverse: Boolean,
  val textRender: TextRender?,
  val lossy: Int?,
  val videoWH: Pair<Int, Int>,
  val videoDuration: Int,
) : Serializable