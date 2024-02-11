package me.tasy5kg.cutegif

import android.annotation.SuppressLint
import android.graphics.*
import android.text.SpannableString
import android.text.Spanned
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.core.view.drawToBitmap
import me.tasy5kg.cutegif.MyApplication.Companion.appContext
import me.tasy5kg.cutegif.toolbox.MediaTools.generateTransparentBitmap
import me.tasy5kg.cutegif.toolbox.OutlineSpan
import me.tasy5kg.cutegif.toolbox.Toolbox.appGetString
import me.tasy5kg.cutegif.toolbox.Toolbox.getContrastColor
import me.tasy5kg.cutegif.toolbox.Toolbox.toInt
import java.io.Serializable
import java.util.*

@SuppressLint("InflateParams")
data class TextRender(
  val content: String,
  val size: Float,
  @ColorInt val color: Int,
  val bold: Boolean,
  val italic: Boolean,
  val gravity: Int,
  val translateX: Float,
  val translateY: Float,
  val rotation: Float,
) : Serializable {

  companion object {
    fun render(textRender: TextRender?, width: Int, height: Int): Bitmap {
      if (textRender == null) return generateTransparentBitmap(1, 1)
      with(textRender) {
        val textBitmap = renderText(content, size, color, bold, italic, gravity)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        Canvas(bitmap).drawBitmap(
          textBitmap, Matrix().apply {
            postRotate(rotation, textBitmap.width / 2f, textBitmap.height / 2f)
            postTranslate(
              (width - textBitmap.width) / 2f + translateX, (height - textBitmap.height) / 2f + translateY
            )
          }, null
        )
        return bitmap
      }
    }

    private fun renderText(
      content: String,
      size: Float,
      @ColorInt color: Int,
      bold: Boolean,
      italic: Boolean,
      gravity: Int,
    ) = with(
      LayoutInflater.from(appContext).inflate(R.layout.view_invisible_mtv_text_render, null) as TextView
    ) {
      /** Add a space before and after each line of text, so that italic text will not be cut off when rendered. */
      text = SpannableString(content.split('\n').joinToString("\n") { " $it " }).apply {
        setSpan(
          OutlineSpan(color.getContrastColor(), size / 8f), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
      }
      textSize = size
      setTypeface(null, bold.toInt() + italic.toInt() * 2)
      setTextColor(color)
      setShadowLayer(size / 8f, 0f, 0f, color.getContrastColor())
      this.gravity = gravity
      measure(WRAP_CONTENT, WRAP_CONTENT)
      layout(0, 0, measuredWidth, measuredHeight)
      drawToBitmap()
    }

    val DEFAULT = TextRender(
      content = appGetString(R.string.input_text_here),
      size = 36f,
      color = Color.WHITE,
      bold = false,
      italic = false,
      gravity = Gravity.CENTER,
      translateX = 0f,
      translateY = 0f,
      rotation = 0f
    )
  }
}