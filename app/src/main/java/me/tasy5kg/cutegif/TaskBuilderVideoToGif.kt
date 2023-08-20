package me.tasy5kg.cutegif

import java.io.Serializable

data class TaskBuilderVideoToGif(
  val inputVideoPath: String,
  val trimTime: Pair<Int, Int>?,
  val cropParams: CropParams,
  val resolutionShortLength: Int,
  val outputSpeed: Float,
  val outputFps: Int,
  val colorQuality: Int,
  val reverse: Boolean,
  val textRender: TextRender?,
  val lossy: Int,
  val videoWH: Pair<Int, Int>,
  val duration: Int,
  val finalDelay: Int,
) : Serializable