package me.tasy5kg.cutegif

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import me.tasy5kg.cutegif.databinding.DialogFragmentVideoToGifExportOptionsSkiBinding
import me.tasy5kg.cutegif.toolbox.Toolbox.constraintBy
import me.tasy5kg.cutegif.toolbox.Toolbox.joinToStringSpecial
import me.tasy5kg.cutegif.toolbox.Toolbox.onClick
import me.tasy5kg.cutegif.toolbox.Toolbox.visibleIf
import kotlin.math.min

class VideoToGifExportOptionsSkiDialogFragment : DialogFragment() {
  private var _binding: DialogFragmentVideoToGifExportOptionsSkiBinding? = null
  private val binding get() = _binding!!
  private val vtgActivity get() = activity as VideoToGifActivity

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
    _binding = DialogFragmentVideoToGifExportOptionsSkiBinding.inflate(layoutInflater, container, false)
    vtgActivity.videoView.pause()
    binding.mbSave.onClick {
      vtgActivity.videoView.pause()
      VideoToGifPerformerSkiActivity.start(vtgActivity, createTaskBuilderSki())
    }
    binding.chipGroupMoreOptions.setOnCheckedStateChangeListener { _, checkedIds ->
      val chipEffectNeedsToBeViewedAfterExporting = listOf(
        binding.chipFramerate, binding.chipEnableReverse //, binding.chipEnableFinalDelay
      )
      val checkedChips = chipEffectNeedsToBeViewedAfterExporting.filter { checkedIds.contains(it.id) }
      if (checkedChips.isEmpty()) {
        binding.mtvMoreOptionsTips.visibility = GONE
      } else {
        binding.mtvMoreOptionsTips.text = getString(
          R.string.effect_needs_to_be_viewed_after_exporting,
          checkedChips.map { it.text }.joinToStringSpecial(getString(R.string.language_item_separator_normal), getString(R.string.language_item_separator_last))
        )
        binding.mtvMoreOptionsTips.visibility = VISIBLE
      }
    }
    binding.sliderQuality.setLabelFormatter { "${it.toInt()}%" }
    binding.mtvFramerateOver10Warning.visibleIf { createTaskBuilderSki().outputFps > 10 }
    binding.chipFramerate.setOnCheckedChangeListener { _, isChecked ->
      binding.llcGroupFramerate.visibleIf { isChecked }
    }

    binding.mbtgResolution.addOnButtonCheckedListener { group, checkedId, isChecked ->
      if (isChecked) {
        group.performHapticFeedback(HapticFeedbackType.SWITCH_TOGGLING)
        binding.llcGroupResolutionInput.visibleIf { checkedId == binding.mbResolutionCustom.id }
        if (checkedId == binding.mbResolutionCustom.id) binding.tietResolutionInputValue.requestFocus()
      }
    }
    binding.mbtgFramerate.addOnButtonCheckedListener { group, _, isChecked ->
      if (isChecked) {
        binding.mtvFramerateOver10Warning.visibleIf { createTaskBuilderSki().outputFps > 10 }
        group.performHapticFeedback(HapticFeedbackType.SWITCH_TOGGLING)
      }
    }
    binding.mbClose.onClick { dismiss() }
    return binding.root
  }

  private fun createTaskBuilderSki() = with(vtgActivity) {
    TaskBuilderVideoToGifSki(
      trimTime = with(rangeSlider) {
        if ((values[0] * 100).toInt() == 0 && (values[1] * 100).toInt() == videoView.duration) null
        else ((values[0] * 100).toInt() to (values[1] * 100).toInt())
      },
      inputVideoPath = inputVideoPath,
      cropParams = cropParams,
      width = gifOutputWH().first,
      height = gifOutputWH().second,
      outputSpeed = playbackSpeed,
      outputFps = when (binding.mbtgFramerate.checkedButtonId) {
        binding.mbFramerate5.id -> 5
        binding.mbFramerate10.id -> 10
        binding.mbFramerate16.id -> 16
        binding.mbFramerate25.id -> 25
        binding.mbFramerate50.id -> 50
        else -> throw IllegalArgumentException()
      },
      gifQuality = binding.sliderQuality.value.toInt(),
      reverse = binding.chipEnableReverse.isChecked,
      textRender = textRender,
      videoWH = videoWH,
      duration = videoView.duration,
      colorKey = null
    )
  }


  private fun gifOutputWH() = vtgActivity.cropParams.calcScaledResolution(getSelectedShortLength())

  private fun getSelectedShortLength() =
    when (binding.mbtgResolution.checkedButtonId) {
      binding.mbResolution144p.id -> 144
      binding.mbResolution240p.id -> 240
      binding.mbResolution320p.id -> 320
      binding.mbResolutionCustom.id -> {
        val inputValue = ("0" + binding.tietResolutionInputValue.text.toString()).toInt()
        if (inputValue == 0) 240 else if (inputValue % 2 == 0) inputValue else inputValue + 1
      }

      else -> throw IllegalArgumentException()
    }.constraintBy(2..min(vtgActivity.cropParams.outW, vtgActivity.cropParams.outH))


  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
    vtgActivity.videoView.start()
  }

  companion object {
    const val TAG = "VideoToGifExportOptionsDialogSkiFragment"
  }
}