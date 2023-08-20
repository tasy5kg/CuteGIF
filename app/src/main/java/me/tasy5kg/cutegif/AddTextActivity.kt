package me.tasy5kg.cutegif

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
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
import me.tasy5kg.cutegif.MyConstants.EXTRA_VIDEO_PATH
import me.tasy5kg.cutegif.MyConstants.EXTRA_VIDEO_POSITION
import me.tasy5kg.cutegif.MyConstants.EXTRA_VIDEO_WH
import me.tasy5kg.cutegif.TextRender.Companion.FONT_LIST
import me.tasy5kg.cutegif.Toolbox.constraintBy
import me.tasy5kg.cutegif.Toolbox.flipVisibility
import me.tasy5kg.cutegif.Toolbox.getB
import me.tasy5kg.cutegif.Toolbox.getExtra
import me.tasy5kg.cutegif.Toolbox.onClick
import me.tasy5kg.cutegif.Toolbox.pathToUri
import me.tasy5kg.cutegif.databinding.ActivityAddText2Binding
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

@SuppressLint("RtlHardcoded")
class AddTextActivity : BaseActivity() {
  private val binding by lazy { ActivityAddText2Binding.inflate(layoutInflater) }
  private val expandableLayoutSet by lazy { setOf(binding.gridLayoutColorPicker, binding.sliderRotation) }
  private lateinit var frame: Bitmap
  private lateinit var textRender: TextRender
  private var viewReferenceLineVerticalPerformedHapticFeedback = false
  private var viewReferenceLineHorizontalPerformedHapticFeedback = false
  private var fontIndex = 0

  override fun onCreateIfEulaAccepted(savedInstanceState: Bundle?) {
    setContentView(binding.root)
    setFinishOnTouchOutside(false)
    val videoPath = intent.getExtra<String>(EXTRA_VIDEO_PATH)
    val videoPosition = intent.getExtra<Long>(EXTRA_VIDEO_POSITION)
    textRender = intent.getExtra(EXTRA_TEXT_RENDER)
    frame = Toolbox.getVideoSingleFrame(pathToUri(videoPath), videoPosition)
    val videoWH = intent.getExtra<Pair<Int, Int>>(EXTRA_VIDEO_WH)
    binding.acivFrame.setImageBitmap(
      Toolbox.generateTransparentBitmap(
        videoWH.first,
        videoWH.second
      )
    )
    Glide.with(this).load(frame).into(binding.acivFrame)
    binding.mbClose.setOnClickListener {
      finishAfterTransition()
    }
    binding.mbDone.onClick(HapticFeedbackType.CONFIRM) {
      setResult(RESULT_OK, VideoToGifActivity.intentAddTextResult(textRender))
      finishAfterTransition()
    }
    binding.mbTextFont.apply {
      text = textRender.font
      onClick {
        performHapticFeedback(MyConstants.HAPTIC_FEEDBACK_TYPE_SWITCH_TOGGLING)
        fontIndex++
        if (fontIndex >= FONT_LIST.size) {
          fontIndex = 0
        }
        val fontName = FONT_LIST[fontIndex].first
        text = fontName
        updateTextRender(font = fontName)
      }
    }
    binding.tiet.apply {
      setText(textRender.content)
      selectAll()
      gravity = textRender.gravity
      addTextChangedListener { updateTextRender(content = binding.tiet.text.toString()) }
    }
    binding.mbTextAlign.apply {
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
        binding.tiet.gravity = newGravity
        updateTextRender(gravity = newGravity)
      }
    }
    binding.mbTextBold.apply {
      isChecked = textRender.bold
      onClick {
        performHapticFeedback(MyConstants.HAPTIC_FEEDBACK_TYPE_SWITCH_TOGGLING)
        updateTextRender(bold = isChecked)
      }
    }
    binding.mbTextItalic.apply {
      isChecked = textRender.italic
      onClick {
        performHapticFeedback(MyConstants.HAPTIC_FEEDBACK_TYPE_SWITCH_TOGGLING)
        updateTextRender(italic = isChecked)
      }
    }
    binding.mbRotate.onClick {
      binding.sliderRotation.showOrHide()
      onClick {
        performHapticFeedback(MyConstants.HAPTIC_FEEDBACK_TYPE_SWITCH_TOGGLING)
      }
    }
    binding.sliderRotation.apply {
      setLabelFormatter { "旋转${it.toInt()}°" }
      addOnChangeListener { slider, value, fromUser ->
        updateTextRender(rotation = value)
      }
    }
    val sequenceOfMrb = binding.gridLayoutColorPicker.children.map { it as MaterialRadioButton }
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
    binding.mbPickedColor.onClick {
      performHapticFeedback(MyConstants.HAPTIC_FEEDBACK_TYPE_SWITCH_TOGGLING)
      binding.gridLayoutColorPicker.showOrHide()
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
    binding.acivText.setOnTouchListener { view, event ->
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
                  binding.viewReferenceLineVertical.visibility = VISIBLE
                  if (!viewReferenceLineVerticalPerformedHapticFeedback) {
                    view.performHapticFeedback(MyConstants.HAPTIC_FEEDBACK_TYPE_GESTURE_END)
                    viewReferenceLineVerticalPerformedHapticFeedback = true
                  }
                  0f
                } else {
                  binding.viewReferenceLineVertical.visibility = GONE
                  viewReferenceLineVerticalPerformedHapticFeedback = false
                  dx
                }
                val dy =
                  (translateYActionUp + event.rawY - yActionDown).constraintBy((-frame.height / 2f)..(frame.height / 2f))
                val translateY = if (abs(dy) < frame.height * 0.03f) {
                  binding.viewReferenceLineHorizontal.visibility = VISIBLE
                  if (!viewReferenceLineHorizontalPerformedHapticFeedback) {
                    view.performHapticFeedback(MyConstants.HAPTIC_FEEDBACK_TYPE_GESTURE_END)
                    viewReferenceLineHorizontalPerformedHapticFeedback = true
                  }
                  0f
                } else {
                  binding.viewReferenceLineHorizontal.visibility = GONE
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
                binding.viewReferenceLineVertical.visibility = GONE
                binding.viewReferenceLineHorizontal.visibility = GONE
                if (System.nanoTime() - timestampActionDown < ViewConfiguration.getLongPressTimeout() * 1000000
                  && (event.rawX - xActionDown).pow(2) + (event.rawX - xActionDown).pow(
                    2
                  ) < 10f
                ) {
                  binding.tiet.requestFocus()
                  getSystemService(InputMethodManager::class.java).showSoftInput(
                    binding.tiet,
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
    get() = binding.mbPickedColor.iconTint.defaultColor
    set(value) {
      binding.mbPickedColor.iconTint =
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
    rotation: Float = textRender.rotation,
  ) {
    textRender =
      TextRender(content, size, color, font, bold, italic, gravity, translateX, translateY, rotation)
    if (updateAcivText) {
      binding.acivText.setImageBitmap(textRender.toBitmap(frame.width, frame.height))
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
      videoPath: String,
      videoPosition: Long,
      textRender: TextRender?,
      videoWH: Pair<Int, Int>,
    ): Intent {
      return Intent(context, AddTextActivity::class.java)
        .putExtra(EXTRA_VIDEO_PATH, videoPath)
        .putExtra(EXTRA_VIDEO_POSITION, videoPosition)
        .putExtra(EXTRA_TEXT_RENDER, textRender ?: TextRender.DEFAULT)
        .putExtra(EXTRA_VIDEO_WH, videoWH)
    }
  }
}