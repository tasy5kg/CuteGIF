package me.tasy5kg.cutegif.bottom.sheet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.google.android.material.button.MaterialButton
import me.tasy5kg.cutegif.R
import me.tasy5kg.cutegif.Toolbox.onClick
import me.tasy5kg.cutegif.VideoToGifActivity
import me.tasy5kg.cutegif.databinding.BottomSheetVideoToGif2MoreOptionsBinding

class BottomSheetVideoToGif2MoreOptions : DialogFragment() {

  private lateinit var binding: BottomSheetVideoToGif2MoreOptionsBinding
  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    binding = BottomSheetVideoToGif2MoreOptionsBinding.inflate(layoutInflater)
    binding.mbSave.onClick {
      (activity as VideoToGifActivity).startVideoToGifPerformer()
    }
    binding.llcShowAdvancedOptions.onClick {
      binding.llcAdvancedOptions.visibility=View.VISIBLE
      this.visibility=View.GONE
    }
    return binding.root
  }

  fun getLossyValue() =
    when (binding.mbtgImageQuality.checkedButtonId) {
      R.id.mb_image_quality_low -> 200
      R.id.mb_image_quality_mid -> 70
      R.id.mb_image_quality_high -> 30
      else -> throw IllegalArgumentException()
    }

  fun getImageResolutionValue(): Int {
    return binding.root.findViewById<MaterialButton>(binding.mbtgResolution.checkedButtonId).text.split(
      "P"
    )[0].toInt()
  }

  fun getFramerateValue() =
    when (binding.mbtgFramerate.checkedButtonId) {
      R.id.mb_framerate_low -> 6
      R.id.mb_framerate_mid -> 12
      R.id.mb_framerate_high -> 18
      R.id.mb_framerate_super -> 25
      else -> throw IllegalArgumentException()

    }

  fun getColorQualityValue() =
    when (binding.mbtgColorQuality.checkedButtonId) {
      R.id.mb_color_quality_low -> 32
      R.id.mb_color_quality_mid -> 128
      R.id.mb_color_quality_high -> 256
      else -> throw IllegalArgumentException()
    }

  fun getReverseValue() = binding.msReverseVideo.isChecked

  companion object {
    const val TAG = "BottomSheetVideoToGif2MoreOptions"
  }
}