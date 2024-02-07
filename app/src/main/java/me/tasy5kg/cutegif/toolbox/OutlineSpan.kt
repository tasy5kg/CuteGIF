// Copyright: https://github.com/santaevpavel/OutlineSpan/tree/master
// License: Apache-2.0

package me.tasy5kg.cutegif.toolbox

import android.graphics.Canvas
import android.graphics.Paint
import android.text.style.ReplacementSpan
import androidx.annotation.ColorInt
import androidx.annotation.Dimension

class OutlineSpan(
  @ColorInt private val strokeColor: Int, @Dimension private val strokeWidth: Float
) : ReplacementSpan() {

  override fun getSize(
    paint: Paint, text: CharSequence, start: Int, end: Int, fontMetrics: Paint.FontMetricsInt?
  ): Int {
    if (fontMetrics != null && paint.fontMetricsInt != null) {
      fontMetrics.bottom = paint.fontMetricsInt.bottom
      fontMetrics.top = paint.fontMetricsInt.top
      fontMetrics.descent = paint.fontMetricsInt.descent
      fontMetrics.leading = paint.fontMetricsInt.leading
    }
    return paint.measureText(text.toString().substring(start until end)).toInt()
  }

  override fun draw(
    canvas: Canvas, text: CharSequence, start: Int, end: Int, x: Float, top: Int, y: Int, bottom: Int, paint: Paint
  ) {
    val originTextColor = paint.color
    paint.apply {
      color = strokeColor
      style = Paint.Style.STROKE
      this.strokeWidth = this@OutlineSpan.strokeWidth
    }
    canvas.drawText(text, start, end, x, y.toFloat(), paint)
    paint.apply {
      color = originTextColor
      style = Paint.Style.FILL
    }
    canvas.drawText(text, start, end, x, y.toFloat(), paint)
  }
}

