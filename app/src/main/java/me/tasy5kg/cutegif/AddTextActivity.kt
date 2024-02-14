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
import androidx.core.view.children
import androidx.core.widget.addTextChangedListener
import com.bumptech.glide.Glide
import com.google.android.material.radiobutton.MaterialRadioButton
import me.tasy5kg.cutegif.MyConstants.EXTRA_TEXT_RENDER
import me.tasy5kg.cutegif.MyConstants.EXTRA_VIDEO_PATH
import me.tasy5kg.cutegif.MyConstants.EXTRA_VIDEO_POSITION
import me.tasy5kg.cutegif.databinding.ActivityAddTextBinding
import me.tasy5kg.cutegif.toolbox.MediaTools.getVideoSingleFrame
import me.tasy5kg.cutegif.toolbox.Toolbox
import me.tasy5kg.cutegif.toolbox.Toolbox.constraintBy
import me.tasy5kg.cutegif.toolbox.Toolbox.flipVisibility
import me.tasy5kg.cutegif.toolbox.Toolbox.getExtra
import me.tasy5kg.cutegif.toolbox.Toolbox.onClick
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

@SuppressLint("RtlHardcoded")
class AddTextActivity : BaseActivity() {
  private val binding by lazy { ActivityAddTextBinding.inflate(layoutInflater) }
  private val expandableLayoutSet by lazy {
    setOf(
      binding.gridLayoutColorPicker, binding.sliderRotation
    )
  }
  private lateinit var frame: Bitmap
  private lateinit var textRender: TextRender
  private var viewReferenceLineVerticalPerformedHapticFeedback = false
  private var viewReferenceLineHorizontalPerformedHapticFeedback = false

  override fun onCreateIfEulaAccepted(savedInstanceState: Bundle?) {
    setContentView(binding.root)
    setFinishOnTouchOutside(false)
    val videoPath = intent.getExtra<String>(EXTRA_VIDEO_PATH)
    val videoPosition = intent.getExtra<Long>(EXTRA_VIDEO_POSITION)
    textRender = intent.getExtra(EXTRA_TEXT_RENDER)
    frame = getVideoSingleFrame(videoPath, videoPosition)
    Glide.with(this).load(frame).into(binding.acivFrame)
    binding.mbClose.setOnClickListener {
      finishAfterTransition()
    }
    binding.mbDone.onClick(HapticFeedbackType.CONFIRM) {
      setResult(RESULT_OK, VideoToGifActivity.intentAddTextResult(textRender))
      finishAfterTransition()
    }
    binding.tiet.apply {
      setText(textRender.content)
      selectAll()
      gravity = textRender.gravity
      addTextChangedListener { updateTextRender(textRender.copy(content = binding.tiet.text.toString())) }
    }
    binding.mbTextAlign.apply {
      setIconResource(gravityToIconPairs[textRender.gravity]!!)
      setOnClickListener {
        it.performHapticFeedback(HapticFeedbackType.SWITCH_TOGGLING)
        val newGravity = when (textRender.gravity) {
          Gravity.LEFT -> Gravity.CENTER
          Gravity.CENTER -> Gravity.RIGHT
          Gravity.RIGHT -> Gravity.LEFT
          else -> throw IllegalArgumentException()
        }
        setIconResource(gravityToIconPairs[newGravity]!!)
        binding.tiet.gravity = newGravity
        updateTextRender(textRender.copy(gravity = newGravity))
      }
    }
    binding.mbTextBold.apply {
      isChecked = textRender.bold
      onClick {
        performHapticFeedback(HapticFeedbackType.SWITCH_TOGGLING)
        updateTextRender(textRender.copy(bold = isChecked))
      }
    }
    binding.mbTextItalic.apply {
      isChecked = textRender.italic
      onClick {
        performHapticFeedback(HapticFeedbackType.SWITCH_TOGGLING)
        updateTextRender(textRender.copy(italic = isChecked))
      }
    }
    binding.mbRotate.onClick {
      binding.sliderRotation.showOrHide()
      onClick {
        performHapticFeedback(HapticFeedbackType.SWITCH_TOGGLING)
      }
    }
    binding.sliderRotation.apply {
      setLabelFormatter { context.getString(R.string.rotate_d_degrees, it.toInt()) }
      addOnChangeListener { _, value, _ ->
        updateTextRender(textRender.copy(rotation = value))
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
          updateTextRender(textRender.copy(color = mbPickedColorBackgroundColor))
        }
      }
    }
    binding.mbPickedColor.onClick {
      performHapticFeedback(HapticFeedbackType.SWITCH_TOGGLING)
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
                    view.performHapticFeedback(HapticFeedbackType.GESTURE_END)
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
                    view.performHapticFeedback(HapticFeedbackType.GESTURE_END)
                    viewReferenceLineHorizontalPerformedHapticFeedback = true
                  }
                  0f
                } else {
                  binding.viewReferenceLineHorizontal.visibility = GONE
                  viewReferenceLineHorizontalPerformedHapticFeedback = false
                  dy
                }
                updateTextRender(textRender.copy(translateX = translateX, translateY = translateY))
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
                if (System.nanoTime() - timestampActionDown < ViewConfiguration.getLongPressTimeout() * 1000000 && (event.rawX - xActionDown).pow(
                    2
                  ) + (event.rawX - xActionDown).pow(
                    2
                  ) < 10f
                ) {
                  binding.tiet.requestFocus()
                  getSystemService(InputMethodManager::class.java).showSoftInput(
                    binding.tiet, InputMethodManager.SHOW_IMPLICIT
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
                textRender.copy(
                  size = (textSizeActionPointerDown + (event.twoFigureDistance() - distanceActionPointerDown) / 10f).constraintBy(
                    4f..128f
                  )
                )
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

  private fun updateTextRender(textRender: TextRender? = null) {
    if (textRender != null) this.textRender = textRender
    binding.acivText.setImageBitmap(TextRender.render(this.textRender, frame.width, frame.height))
  }

  companion object {
    private val gravityToIconPairs = mapOf(
      Gravity.LEFT to R.drawable.ic_baseline_format_align_left_24,
      Gravity.CENTER to R.drawable.ic_baseline_format_align_center_24,
      Gravity.RIGHT to R.drawable.ic_baseline_format_align_right_24
    )

    private fun MotionEvent.twoFigureDistance() = sqrt((getX(0) - getX(1)).pow(2) + (getY(0) - getY(1)).pow(2))

    fun startIntent(
      context: Context, videoPath: String, videoPosition: Long, textRender: TextRender?
    ): Intent {
      return Intent(context, AddTextActivity::class.java).putExtra(EXTRA_VIDEO_PATH, videoPath)
        .putExtra(EXTRA_VIDEO_POSITION, videoPosition).putExtra(EXTRA_TEXT_RENDER, textRender ?: TextRender.DEFAULT)
    }
  }
}