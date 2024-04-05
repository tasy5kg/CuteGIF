package me.tasy5kg.cutegif

import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import me.tasy5kg.cutegif.databinding.BottomSheetVideoToGifPlaybackSpeedBinding
import me.tasy5kg.cutegif.toolbox.Toolbox.visibleIf

class BottomSheetVideoToGifPlaybackSpeed : BottomSheetDialogFragment() {
  private var _binding: BottomSheetVideoToGifPlaybackSpeedBinding? = null
  private val binding get() = _binding!!
  private val videoToGifActivity get() = activity as VideoToGifActivity

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
    _binding = BottomSheetVideoToGifPlaybackSpeedBinding.inflate(layoutInflater, container, false)
    (dialog as BottomSheetDialog).behavior.state = BottomSheetBehavior.STATE_EXPANDED
    binding.sliderSpeed.apply {
      setLabelFormatter { sliderValueToText(it) }
      addOnChangeListener { slider, value, _ ->
        binding.mtvSpeedWarning.visibleIf { !(videoToGifActivity.setPlaybackSpeed(sliderValueToSpeed(value), sliderValueToText(value))) }
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
      3 -> 1.25f
      4 -> 1.5f
      5 -> 2f
      6 -> 3f
      7 -> 4f
      else -> throw IllegalArgumentException()
    }

    private fun sliderValueToText(value: Float) = when (value.toInt()) {
      0 -> "0.5X"
      1 -> "0.75X"
      2 -> "1X"
      3 -> "1.25X"
      4 -> "1.5X"
      5 -> "2X"
      6 -> "3X"
      7 -> "4X"
      else -> throw IllegalArgumentException()
    }

  }
}