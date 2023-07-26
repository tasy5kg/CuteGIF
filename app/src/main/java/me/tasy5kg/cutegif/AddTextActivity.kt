package me.tasy5kg.cutegif

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.inputmethod.InputMethodManager
import androidx.annotation.ColorInt
import androidx.core.view.children
import androidx.core.widget.addTextChangedListener
import com.bumptech.glide.Glide
import com.google.android.material.radiobutton.MaterialRadioButton
import me.tasy5kg.cutegif.MyConstants.EXTRA_TEXT_RENDER
import me.tasy5kg.cutegif.MyConstants.EXTRA_VIDEO_POSITION
import me.tasy5kg.cutegif.MyConstants.EXTRA_VIDEO_URI
import me.tasy5kg.cutegif.MyConstants.EXTRA_VIDEO_WH
import me.tasy5kg.cutegif.TextRender.Companion.FONT_LIST
import me.tasy5kg.cutegif.Toolbox.constraintBy
import me.tasy5kg.cutegif.Toolbox.flipVisibility
import me.tasy5kg.cutegif.Toolbox.getB
import me.tasy5kg.cutegif.Toolbox.getExtra
import me.tasy5kg.cutegif.Toolbox.onClick
import me.tasy5kg.cutegif.databinding.ActivityAddText2Binding
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

@SuppressLint("RtlHardcoded")
class AddTextActivity : BaseActivity() {
  private val binding by lazy { ActivityAddText2Binding.inflate(layoutInflater) }
  private val tiet by lazy { binding.tiet }
  private val mbPickedColor by lazy { binding.mbPickedColor }
  private val mbTextFont by lazy { binding.mbTextFont }
  private val mbTextBold by lazy { binding.mbTextBold }
  private val mbTextItalic by lazy { binding.mbTextItalic }
  private val mbTextAlign by lazy { binding.mbTextAlign }
  private val acivText by lazy { binding.acivText }
  private val gridLayoutColorPicker by lazy { binding.gridLayoutColorPicker }
  private val viewReferenceLineHorizontal by lazy { binding.viewReferenceLineHorizontal }
  private val viewReferenceLineVertical by lazy { binding.viewReferenceLineVertical }
  private val expandableLayoutSet by lazy { setOf(gridLayoutColorPicker, sliderRotation) }
  private val sliderRotation by lazy { binding.sliderRotation }
  private lateinit var frame: Bitmap
  private lateinit var textRender: TextRender
  private var viewReferenceLineVerticalPerformedHapticFeedback = false
  private var viewReferenceLineHorizontalPerformedHapticFeedback = false
  private var fontIndex = 0

  override fun onCreateIfEulaAccepted(savedInstanceState: Bundle?) {
    setContentView(binding.root)
    setFinishOnTouchOutside(false)
    val videoUri = intent.getExtra<Uri>(EXTRA_VIDEO_URI)
    val videoPosition = intent.getExtra<Long>(EXTRA_VIDEO_POSITION)
    textRender = intent.getExtra(EXTRA_TEXT_RENDER)
    frame = Toolbox.getVideoSingleFrame(videoUri, videoPosition, true)
    val acivFrame = binding.acivFrame
    val videoWH = intent.getExtra<Pair<Int, Int>>(EXTRA_VIDEO_WH)
    binding.acivFrame.setImageBitmap(
      Toolbox.generateTransparentBitmap(
        videoWH.first,
        videoWH.second
      )
    )
    Glide.with(this).load(frame).into(acivFrame)
    binding.mbClose.setOnClickListener {
      finishAfterTransition()
    }
    binding.mbDone.onClick(HapticFeedbackType.CONFIRM) {
      setResult(RESULT_OK, VideoToGifActivity.intentAddTextResult(textRender))
      finishAfterTransition()
    }
    mbTextFont.apply {
      text = textRender.font
      onClick {
        fontIndex++
        if (fontIndex >= FONT_LIST.size) {
          fontIndex = 0
        }
        val fontName = FONT_LIST[fontIndex].first
        text = fontName
        updateTextRender(font = fontName)
      }
    }
    tiet.apply {
      setText(textRender.content)
      selectAll()
      //  requestFocus()
      gravity = textRender.gravity
      addTextChangedListener { updateTextRender(content = tiet.text.toString()) }
    }
    mbTextAlign.apply {
      setIconResource(gravityToIconPairs.getB(textRender.gravity))
      setOnClickListener {
        it.performHapticFeedback(MyConstants.HAPTIC_FEEDBACK_TYPE_SWITCH_TOGGLING)
        val newGravity = when (textRender.gravity) {
          Gravity.LEFT -> Gravity.CENTER
          Gravity.CENTER -> Gravity.RIGHT
          Gravity.RIGHT -> Gravity.LEFT
          else -> throw IllegalArgumentException()
        }
        setIconResource(gravityToIconPairs.getB(newGravity))
        tiet.gravity = newGravity
        updateTextRender(gravity = newGravity)
      }
    }
    mbTextBold.apply {
      isChecked = textRender.bold
      setOnClickListener {
        it.performHapticFeedback(MyConstants.HAPTIC_FEEDBACK_TYPE_SWITCH_TOGGLING)
        updateTextRender(bold = isChecked)
      }
    }
    mbTextItalic.apply {
      isChecked = textRender.italic
      setOnClickListener {
        updateTextRender(italic = isChecked)
      }
    }
    binding.mbRotate.onClick {
      sliderRotation.showOrHide()
    }
    sliderRotation.apply {
      setLabelFormatter { "旋转${it.toInt()}°" }
      addOnChangeListener { slider, value, fromUser ->
        updateTextRender(rotation = value)
      }
    }
    val sequenceOfMrb = gridLayoutColorPicker.children.map { it as MaterialRadioButton }
    sequenceOfMrb.forEach { aMrb ->
      aMrb.isChecked = (aMrb.buttonTintList!!.defaultColor == textRender.color)
      aMrb.setOnCheckedChangeListener { buttonView, isChecked ->
        if (isChecked) {
          val checkedMrb = buttonView as MaterialRadioButton
          sequenceOfMrb.forEach { it.isChecked = (it == checkedMrb) }
          mbPickedColorBackgroundColor = checkedMrb.buttonTintList!!.defaultColor
          updateTextRender(color = mbPickedColorBackgroundColor)
        }
      }
    }
    mbPickedColor.setOnClickListener {
      it.performHapticFeedback(MyConstants.HAPTIC_FEEDBACK_TYPE_SWITCH_TOGGLING)
      gridLayoutColorPicker.showOrHide()
    }
    mbPickedColorBackgroundColor = textRender.color
    setupAcivText()
    updateTextRender()
  }

  @SuppressLint("ClickableViewAccessibility")
  private fun setupAcivText() {
    var xActionDown = 0f
    var yActionDown = 0f
    var translateXActionUp = textRender.translateX
    var translateYActionUp = textRender.translateY
    var timestampActionDown = 0L
    var distanceActionPointerDown = 0f
    var textSizeActionPointerDown = textRender.size
    var enteredScaleMode = false
    acivText.setOnTouchListener { view, event ->
      when (event.pointerCount) {
        1 -> {
          when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
              if (!enteredScaleMode) {
                xActionDown = event.rawX
                yActionDown = event.rawY
                timestampActionDown = System.nanoTime()
              }
            }

            MotionEvent.ACTION_MOVE -> {
              if (!enteredScaleMode) {
                val dx =
                  (translateXActionUp + event.rawX - xActionDown).constraintBy((-frame.width / 2f)..(frame.width / 2f))
                val translateX = if (abs(dx) < frame.width * 0.03f) {
                  viewReferenceLineVertical.visibility = VISIBLE
                  if (!viewReferenceLineVerticalPerformedHapticFeedback) {
                    view.performHapticFeedback(MyConstants.HAPTIC_FEEDBACK_TYPE_GESTURE_END)
                    viewReferenceLineVerticalPerformedHapticFeedback = true
                  }
                  0f
                } else {
                  viewReferenceLineVertical.visibility = GONE
                  viewReferenceLineVerticalPerformedHapticFeedback = false
                  dx
                }
                val dy =
                  (translateYActionUp + event.rawY - yActionDown).constraintBy((-frame.height / 2f)..(frame.height / 2f))
                val translateY = if (abs(dy) < frame.height * 0.03f) {
                  viewReferenceLineHorizontal.visibility = VISIBLE
                  if (!viewReferenceLineHorizontalPerformedHapticFeedback) {
                    view.performHapticFeedback(MyConstants.HAPTIC_FEEDBACK_TYPE_GESTURE_END)
                    viewReferenceLineHorizontalPerformedHapticFeedback = true
                  }
                  0f
                } else {
                  viewReferenceLineHorizontal.visibility = GONE
                  viewReferenceLineHorizontalPerformedHapticFeedback = false
                  dy
                }
                updateTextRender(translateX = translateX, translateY = translateY)
              }
            }

            MotionEvent.ACTION_UP -> {
              if (enteredScaleMode) {
                enteredScaleMode = false
              } else {
                translateXActionUp = textRender.translateX
                translateYActionUp = textRender.translateY
                viewReferenceLineVertical.visibility = GONE
                viewReferenceLineHorizontal.visibility = GONE
                if (System.nanoTime() - timestampActionDown < ViewConfiguration.getLongPressTimeout() * 1000000
                  && (event.rawX - xActionDown).pow(2) + (event.rawX - xActionDown).pow(
                    2
                  ) < 10f
                ) {
                  tiet.requestFocus()
                  getSystemService(InputMethodManager::class.java).showSoftInput(
                    tiet,
                    InputMethodManager.SHOW_IMPLICIT
                  )
                }
              }
            }
          }
        }

        2 -> {
          when (event.actionMasked) {
            MotionEvent.ACTION_POINTER_DOWN -> {
              distanceActionPointerDown = event.twoFigureDistance()
              textSizeActionPointerDown = textRender.size
              enteredScaleMode = true
            }

            MotionEvent.ACTION_MOVE -> {
              updateTextRender(
                size = (textSizeActionPointerDown + (event.twoFigureDistance() - distanceActionPointerDown) / 10f)
                  .constraintBy(4f..128f)
              )
            }
          }
        }
      }
      true
    }
  }

  private var mbPickedColorBackgroundColor
    get() = mbPickedColor.iconTint.defaultColor
    set(value) {
      mbPickedColor.iconTint =
        Toolbox.createColorStateListFromColorParsed(arrayOf(android.R.attr.state_enabled to value))
    }

  private fun View.showOrHide() {
    expandableLayoutSet.forEach { if (it != this) it.visibility = GONE }
    flipVisibility()
  }

  private fun updateTextRender(
    content: String = textRender.content,
    size: Float = textRender.size,
    @ColorInt color: Int = textRender.color,
    font: String = textRender.font,
    bold: Boolean = textRender.bold,
    italic: Boolean = textRender.italic,
    gravity: Int = textRender.gravity,
    translateX: Float = textRender.translateX,
    translateY: Float = textRender.translateY,
    updateAcivText: Boolean = true,
    rotation: Float = textRender.rotation
  ) {
    textRender =
      TextRender(content, size, color, font, bold, italic, gravity, translateX, translateY, rotation)
    if (updateAcivText) {
      acivText.setImageBitmap(textRender.toBitmap(frame.width, frame.height))
    }
  }

  companion object {
    private val gravityToIconPairs = setOf(
      Gravity.LEFT to R.drawable.ic_baseline_format_align_left_24,
      Gravity.CENTER to R.drawable.ic_baseline_format_align_center_24,
      Gravity.RIGHT to R.drawable.ic_baseline_format_align_right_24
    )

    private fun MotionEvent.twoFigureDistance() =
      sqrt((getX(0) - getX(1)).pow(2) + (getY(0) - getY(1)).pow(2))

    fun startIntent(
      context: Context,
      videoUri: Uri,
      videoPosition: Long,
      textRender: TextRender?,
      videoWH: Pair<Int, Int>
    ): Intent {
      return Intent(context, AddTextActivity::class.java)
        .putExtra(EXTRA_VIDEO_URI, videoUri)
        .putExtra(EXTRA_VIDEO_POSITION, videoPosition)
        .putExtra(EXTRA_TEXT_RENDER, textRender ?: TextRender.DEFAULT)
        .putExtra(EXTRA_VIDEO_WH, videoWH)
    }
  }
}