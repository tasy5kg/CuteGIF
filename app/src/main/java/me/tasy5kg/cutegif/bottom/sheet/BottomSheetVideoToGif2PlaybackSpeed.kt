package me.tasy5kg.cutegif.bottom.sheet

import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.slider.Slider
import me.tasy5kg.cutegif.VideoToGifActivity
import me.tasy5kg.cutegif.databinding.BottomSheetVideoToGif2PlaybackSpeedBinding

class BottomSheetVideoToGif2PlaybackSpeed : BottomSheetDialogFragment() {

  private lateinit var sliderSpeed: Slider
  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View {
    val binding = BottomSheetVideoToGif2PlaybackSpeedBinding.inflate(layoutInflater)
    sliderSpeed = binding.sliderSpeed
    with(sliderSpeed) {
      setLabelFormatter {
        sliderValueToText(it)
      }
      addOnChangeListener { slider, value, _ ->
        (activity as VideoToGifActivity).setPlaybackSpeed(
          sliderValueToSpeed(value),
          sliderValueToText(value)
        )
        slider.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
      }
    }
    return binding.root
  }

  companion object {
    const val TAG = "BottomSheetVideoToGif2PlaybackSpeed"

    private fun sliderValueToSpeed(value: Float) =
      when (value.toInt()) {
        0 -> 0.5f
        1 -> 0.75f
        2 -> 1f
        3 -> 1.5f
        4 -> 2f
        5 -> 3f
        6 -> 4f
        else -> throw IllegalArgumentException()
      }

    private fun sliderValueToText(value: Float) =
      when (value.toInt()) {
        0 -> "0.5倍速"
        1 -> "0.75倍速"
        2 -> "正常"
        3 -> "1.5倍速"
        4 -> "2倍速"
        5 -> "3倍速"
        6 -> "4倍速"
        else -> throw IllegalArgumentException()
      }

  }
}