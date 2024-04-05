package me.tasy5kg.cutegif

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import me.tasy5kg.cutegif.databinding.BottomSheetVideoToGifCropRatioBinding
import me.tasy5kg.cutegif.toolbox.Toolbox.appGetString
import me.tasy5kg.cutegif.toolbox.Toolbox.onClick
import me.tasy5kg.cutegif.toolbox.Toolbox.swapIf

class BottomSheetVideoToGifCropRatio : BottomSheetDialogFragment() {
  private var _binding: BottomSheetVideoToGifCropRatioBinding? = null
  private val binding get() = _binding!!
  private val videoToGifActivity get() = activity as VideoToGifActivity
  private var lastCropRatio: Pair<Int, Int>? = null

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
    _binding = BottomSheetVideoToGifCropRatioBinding.inflate(layoutInflater, container, false)
    // make BottomSheetDialogFragment always fully expanded
    (dialog as BottomSheetDialog).behavior.state = BottomSheetBehavior.STATE_EXPANDED
    val mbCropRatiosToRatio = setOf(
      binding.mbCropRatioFree to null,
      binding.mbCropRatioSquare to (1 to 1),
      binding.mbCropRatio43 to (4 to 3),
      binding.mbCropRatio169 to (16 to 9)
    )
    mbCropRatiosToRatio.forEach {
      it.first.onClick {
        performHapticFeedback(HapticFeedbackType.SWITCH_TOGGLING)
        lastCropRatio = it.second?.swapIf { this == lastCropRatio }
        videoToGifActivity.setCropRatio(lastCropRatio)
      }
    }
    return binding.root
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }

  companion object {
    const val TAG = "BottomSheetVideoToGifCropRatio"

    fun cropRatioToText(ratio: Pair<Int, Int>?) = when (ratio) {
      null -> appGetString(R.string.crop_ratio_)
      (1 to 1) -> appGetString(R.string.crop_square)
      else -> "${ratio.first}:${ratio.second}"
    }
  }
}