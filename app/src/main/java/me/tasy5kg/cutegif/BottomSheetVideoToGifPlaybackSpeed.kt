package me.tasy5kg.cutegif

import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import me.tasy5kg.cutegif.databinding.BottomSheetVideoToGifPlaybackSpeedBinding

class BottomSheetVideoToGifPlaybackSpeed : BottomSheetDialogFragment() {
  private var _binding: BottomSheetVideoToGifPlaybackSpeedBinding? = null
  private val binding get() = _binding!!
  private val videoToGifActivity get() = activity as VideoToGifActivity

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
    _binding = BottomSheetVideoToGifPlaybackSpeedBinding.inflate(layoutInflater, container, false)
    binding.sliderSpeed.apply {
      setLabelFormatter { sliderValueToText(it) }
      addOnChangeListener { slider, value, _ ->
        videoToGifActivity.setPlaybackSpeed(sliderValueToSpeed(value), sliderValueToText(value))
        slider.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
      }
    }
    return binding.root
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }

  companion object {
    const val TAG = "BottomSheetVideoToGif2PlaybackSpeed"

    private fun sliderValueToSpeed(value: Float) = when (value.toInt()) {
      0 -> 0.5f
      1 -> 0.75f
      2 -> 1f
      3 -> 1.5f
      4 -> 2f
      5 -> 3f
      6 -> 4f
      else -> throw IllegalArgumentException()
    }

    private fun sliderValueToText(value: Float) = when (value.toInt()) {
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