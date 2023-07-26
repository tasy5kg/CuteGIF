package me.tasy5kg.cutegif.bottom.sheet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import me.tasy5kg.cutegif.R
import me.tasy5kg.cutegif.Toolbox.appGetString
import me.tasy5kg.cutegif.Toolbox.swapIf
import me.tasy5kg.cutegif.VideoToGifActivity
import me.tasy5kg.cutegif.databinding.BottomSheetVideoToGif2CropRatioBinding

class BottomSheetVideoToGif2CropRatio : BottomSheetDialogFragment() {

  private var lastCropRatio: Pair<Int, Int>? = null
  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    val binding = BottomSheetVideoToGif2CropRatioBinding.inflate(layoutInflater)
    val parentActivity = activity as VideoToGifActivity
    val mbCropRatiosToRatio = setOf(
      binding.mbCropRatioFree to null,
      binding.mbCropRatioSquare to (1 to 1),
      binding.mbCropRatio43 to (4 to 3),
      binding.mbCropRatio169 to (16 to 9)
    )
    mbCropRatiosToRatio.forEach {
      it.first.setOnClickListener { _ ->
        lastCropRatio = it.second?.swapIf { this == lastCropRatio }
        parentActivity.setCropRatio(lastCropRatio)
      }
    }
    return binding.root
  }

  companion object {
    const val TAG = "BottomSheetVideoToGif2CropRatio"

    fun cropRatioToText(ratio: Pair<Int, Int>?) =
      when (ratio) {
        null -> "画幅"
        (1 to 1) -> appGetString(R.string.crop_square)
        else -> "${ratio.first}:${ratio.second}"
      }
  }
}