package me.tasy5kg.cutegif

import android.annotation.SuppressLint
import android.graphics.*
import android.text.SpannableString
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.drawToBitmap
import java.io.Serializable
import java.util.*
import me.tasy5kg.cutegif.MyApplication.Companion.appContext
import me.tasy5kg.cutegif.Toolbox.getB
import me.tasy5kg.cutegif.Toolbox.getContrastColor
import me.tasy5kg.cutegif.Toolbox.setSpan
import me.tasy5kg.cutegif.Toolbox.toInt
import ru.santaev.outlinespan.OutlineSpan

@SuppressLint("InflateParams")
data class TextRender(
  val content: String,
  val size: Float,
  @ColorInt val color: Int,
  val font: String,
  val bold: Boolean,
  val italic: Boolean,
  val gravity: Int,
  val translateX: Float,
  val translateY: Float,
  val rotation: Float,
) : Serializable {

  fun toBitmap(width: Int, height: Int): Bitmap {
    val textBitmap = renderText(content, size, color, font, bold, italic, gravity)
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    Canvas(bitmap).drawBitmap(
      textBitmap,
      Matrix().apply {
        postRotate(rotation, textBitmap.width / 2f, textBitmap.height / 2f)
        postTranslate(
          (width - textBitmap.width) / 2f + translateX,
          (height - textBitmap.height) / 2f + translateY
        )
      },
      null
    )
    return bitmap
  }

  companion object {
    private fun renderText(
      content: String,
      size: Float,
      @ColorInt color: Int,
      font: String,
      bold: Boolean,
      italic: Boolean,
      gravity: Int,
    ) =
      with(LayoutInflater.from(appContext).inflate(R.layout.view_invisible_mtv_text_render, null) as TextView) {
        /** Add a space before and after each line of text, so that italic text will not be cut off when rendered. */
        text = SpannableString(content.split('\n').joinToString("\n") { " $it " }).apply {
          setSpan(OutlineSpan(color.getContrastColor(), size / 8f))
        }
        textSize = size
        setTypeface(FONT_LIST.getB(font), bold.toInt() + italic.toInt() * 2)
        setTextColor(color)
        setShadowLayer(size / 8f, 0f, 0f, color.getContrastColor())
        this.gravity = gravity
        measure(WRAP_CONTENT, WRAP_CONTENT)
        layout(0, 0, measuredWidth, measuredHeight)
        drawToBitmap()
      }

    val FONT_LIST = listOf(
      "默认字体" to null,
      "衬线字体" to Typeface.SERIF,
      "得意黑" to ResourcesCompat.getFont(appContext, R.font.smiley_sans_oblique),
      "霞鹜文楷" to ResourcesCompat.getFont(appContext, R.font.lxgw_wenkai_regular_lite),
      "霞鹜漫黑" to ResourcesCompat.getFont(appContext, R.font.lxgw_marker_gothic),
    )

    val DEFAULT = TextRender(
      content = "在此输入文字",
      size = 36f,
      color = Color.WHITE,
      font = FONT_LIST[0].first,
      bold = false,
      italic = false,
      gravity = Gravity.CENTER,
      translateX = 0f,
      translateY = 0f,
      rotation = 0f
    )
  }
}