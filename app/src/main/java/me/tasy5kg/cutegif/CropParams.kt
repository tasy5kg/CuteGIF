package me.tasy5kg.cutegif

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import androidx.core.graphics.drawable.toDrawable
import me.tasy5kg.cutegif.toolbox.Toolbox.closestEven
import java.io.Serializable
import kotlin.math.max
import kotlin.math.min

data class CropParams(
  val outW: Int,
  val outH: Int,
  val x: Int,
  val y: Int,
) : Serializable {

  constructor(rect: Rect) : this(rect.width(), rect.height(), rect.left, rect.top)

  fun toRect() = Rect(x, y, x + outW, y + outH)

  fun toFFmpegCropCommand() = "crop=$outW:$outH:$x:$y"

  fun getFFmpegScaleCommand(targetShort: Int) = "scale=${calcScaledResolutionString(targetShort, ":", false)}"

  fun crop(bitmap: Bitmap) = Bitmap.createBitmap(bitmap, x, y, outW, outH)

  fun shortLength() = min(this.outW, this.outH)

  fun calcScaledResolution(targetShort: Int): Pair<Int, Int> {
    // According to the video encoder requirements, the length and width should be multiples of 2
    val long = max(outW, outH)
    val short = min(outW, outH)
    val targetLong = (long * targetShort / short.toDouble()).closestEven()
    return if (outW > outH) Pair(targetLong, targetShort) else Pair(targetShort, targetLong)
  }

  // useMinus2 = true will make FFmpeg calculate scaling by itself
  fun calcScaledResolutionString(targetShort: Int, splitter: String, useMinus2: Boolean) = when (useMinus2) {
    true -> if (outW > outH) "-2$splitter$targetShort" else "$targetShort$splitter-2"
    false -> calcScaledResolution(targetShort).run { "$first$splitter$second" }
  }

  fun createPlaceholderBitmap(resources: Resources) = Bitmap.createBitmap(outW, outH, Bitmap.Config.ALPHA_8).apply {
    eraseColor(Color.TRANSPARENT)
  }.toDrawable(resources)
}