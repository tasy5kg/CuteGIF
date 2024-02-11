package me.tasy5kg.cutegif

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.core.graphics.get
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.DialogFragment
import com.arthenica.ffmpegkit.FFmpegKit
import me.tasy5kg.cutegif.MyConstants.FFMPEG_COMMAND_PREFIX_FOR_ALL_AN
import me.tasy5kg.cutegif.MyConstants.VIDEO_TO_GIF_PREVIEW_CACHE_DIR
import me.tasy5kg.cutegif.databinding.DialogFragmentVideoToGifExportOptionsBinding
import me.tasy5kg.cutegif.toolbox.FileTools.makeDirEmpty
import me.tasy5kg.cutegif.toolbox.MediaTools.getVideoSingleFrame
import me.tasy5kg.cutegif.toolbox.MediaTools.gifsicleLossy
import me.tasy5kg.cutegif.toolbox.MediaTools.saveToPng
import me.tasy5kg.cutegif.toolbox.Toolbox
import me.tasy5kg.cutegif.toolbox.Toolbox.backgroundColor
import me.tasy5kg.cutegif.toolbox.Toolbox.colorIntToHex
import me.tasy5kg.cutegif.toolbox.Toolbox.constraintBy
import me.tasy5kg.cutegif.toolbox.Toolbox.joinToStringSpecial
import me.tasy5kg.cutegif.toolbox.Toolbox.logRed
import me.tasy5kg.cutegif.toolbox.Toolbox.onClick
import me.tasy5kg.cutegif.toolbox.Toolbox.visibleIf
import kotlin.math.min

class VideoToGifExportOptionsDialogFragment : DialogFragment() {
  private var _binding: DialogFragmentVideoToGifExportOptionsBinding? = null
  private val binding get() = _binding!!
  private val previewBitmapMap = mutableMapOf<TaskBuilderVideoToGifForPreview, Bitmap>()
  private val vtgActivity get() = activity as VideoToGifActivity
  private lateinit var frame: Bitmap

  /** Determining whether a Key exists in a Map/Set is fast, while determining whether a file exists is much slower */
  private val fileExistsCache = mutableSetOf<String>()

  @SuppressLint("ClickableViewAccessibility")
  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
  ): View {
    _binding = DialogFragmentVideoToGifExportOptionsBinding.inflate(layoutInflater, container, false)
    clearPreviewImageCache()
    vtgActivity.videoView.pause()
    binding.mbSave.onClick {
      vtgActivity.videoView.pause()
      VideoToGifPerformerActivity.start(vtgActivity, createTaskBuilder())
    }
    binding.chipGroupMoreOptions.setOnCheckedStateChangeListener { _, checkedIds ->
      val chipEffectNeedsToBeViewedAfterExporting = listOf(
        binding.chipFramerate, binding.chipEnableReverse, binding.chipEnableFinalDelay
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
    binding.acivSingleFramePreview.setOnTouchListener { _, event ->
      if (binding.chipEnableColorKey.isChecked && event.pointerCount == 1) {
        val eventXY = floatArrayOf(event.x, event.y)
        val invertMatrix = Matrix()
        binding.acivSingleFramePreview.imageMatrix.invert(invertMatrix)
        invertMatrix.mapPoints(eventXY)
        val bitmap = renderPreviewImage(createTaskBuilder().getForPreviewOnly().copy(colorKey = null))
        logRed("(v.drawable as BitmapDrawable).bitmap", "${bitmap.width}x${bitmap.height}")
        val x = eventXY[0].toInt().constraintBy(0 until bitmap.width)
        val y = eventXY[1].toInt().constraintBy(0 until bitmap.height)
        binding.viewColorKeyIndicator.backgroundColor = bitmap[x, y]
        binding.mcbColorKeyPreview.isChecked = true
        updatePreviewImage()
      }
      true
    }
    binding.mtvFramerateOver10Warning.visibleIf { createTaskBuilder().outputFps > 10 }
    binding.chipEnableColorKey.setOnCheckedChangeListener { _, isChecked ->
      binding.llcGroupColorKey.visibleIf { isChecked }
      updatePreviewImage()
    }
    binding.chipFramerate.setOnCheckedChangeListener { _, isChecked ->
      binding.llcGroupFramerate.visibleIf { isChecked }
    }
    binding.viewColorKeyIndicator.onClick { Toolbox.toast(context.getString(R.string.click_on_the_preview_image_to_pick_an_color)) }
    binding.sliderColorKeyBlend.apply {
      setLabelFormatter { "${it.toInt()}%" }
      addOnChangeListener { _, _, _ ->
        performHapticFeedback(HapticFeedbackConstants.TEXT_HANDLE_MOVE)
        updatePreviewImage()
      }
    }
    binding.sliderColorKeySimilarity.apply {
      setLabelFormatter { "${it.toInt()}%" }
      addOnChangeListener { _, _, _ ->
        performHapticFeedback(HapticFeedbackConstants.TEXT_HANDLE_MOVE)
        updatePreviewImage()
      }
    }
    binding.tietResolutionInputValue.doAfterTextChanged {
      updatePreviewImage()
    }
    binding.mbtgColorQuality.addOnButtonCheckedListener { group, _, isChecked ->
      if (isChecked) {
        updatePreviewImage()
        group.performHapticFeedback(HapticFeedbackType.SWITCH_TOGGLING)
      }
    }

    binding.mbtgResolution.addOnButtonCheckedListener { group, checkedId, isChecked ->
      if (isChecked) {
        group.performHapticFeedback(HapticFeedbackType.SWITCH_TOGGLING)
        binding.llcGroupResolutionInput.visibleIf { checkedId == binding.mbResolutionCustom.id }
        if (checkedId == binding.mbResolutionCustom.id) binding.tietResolutionInputValue.requestFocus()
        updatePreviewImage()
      }
    }
    binding.mbtgImageQuality.addOnButtonCheckedListener { group, _, isChecked ->
      if (isChecked) {
        updatePreviewImage()
        group.performHapticFeedback(HapticFeedbackType.SWITCH_TOGGLING)
      }
    }
    binding.mbtgFramerate.addOnButtonCheckedListener { group, _, isChecked ->
      if (isChecked) {
        binding.mtvFramerateOver10Warning.visibleIf { createTaskBuilder().outputFps > 10 }
        group.performHapticFeedback(HapticFeedbackType.SWITCH_TOGGLING)
      }
    }
    binding.mcbColorKeyPreview.setOnCheckedChangeListener { buttonView, _ ->
      buttonView.performHapticFeedback(HapticFeedbackType.SWITCH_TOGGLING)
      updatePreviewImage()
    }
    binding.mbClose.onClick { dismiss() }
    frame = getVideoSingleFrame(
      vtgActivity.inputVideoPath, vtgActivity.videoView.currentPosition.toLong()
    )
    Canvas(frame).drawBitmap(
      TextRender.render(vtgActivity.textRender, frame.width, frame.height), 0f, 0f, null
    ) // Merge the text layer with the frame
    frame = vtgActivity.cropParams.crop(frame) // Crop
    binding.viewColorKeyIndicator.backgroundColor = vtgActivity.savedColorKeyColor ?: frame[0, 0]
    updatePreviewImage()
    return binding.root
  }

  private fun createTaskBuilder() = with(vtgActivity) {
    TaskBuilderVideoToGif(trimTime = with(rangeSlider) {
      if ((values[0] * 100).toInt() == 0 && (values[1] * 100).toInt() == videoView.duration) null
      else ((values[0] * 100).toInt() to (values[1] * 100).toInt())
    },
      inputVideoPath = inputVideoPath,
      cropParams = cropParams,
      shortLength = when (binding.mbtgResolution.checkedButtonId) {
        binding.mbResolution144p.id -> 144
        binding.mbResolution240p.id -> 240
        binding.mbResolution320p.id -> 320
        binding.mbResolutionCustom.id -> getUserCustomResolution()
        else -> throw IllegalArgumentException()
      },
      outputSpeed = playbackSpeed,
      outputFps = when (binding.mbtgFramerate.checkedButtonId) {
        binding.mbFramerate5.id -> 5
        binding.mbFramerate10.id -> 10
        binding.mbFramerate16.id -> 16
        binding.mbFramerate25.id -> 25
        binding.mbFramerate50.id -> 50
        else -> throw IllegalArgumentException()
      },
      colorQuality = when (binding.mbtgColorQuality.checkedButtonId) {
        binding.mbColorQualityLow.id -> 32
        binding.mbColorQualityMid.id -> 64
        binding.mbColorQualityHigh.id -> 128
        binding.mbColorQualityMax.id -> 256
        else -> throw IllegalArgumentException()
      },
      reverse = binding.chipEnableReverse.isChecked,
      textRender = textRender,
      lossy = when (binding.mbtgImageQuality.checkedButtonId) {
        binding.mbImageQualityLow.id -> 200
        binding.mbImageQualityMid.id -> 70
        binding.mbImageQualityHigh.id -> 30
        binding.mbImageQualityMax.id -> null
        else -> throw IllegalArgumentException()
      },
      videoWH = videoWH,
      duration = videoView.duration,
      finalDelay = if (binding.chipEnableFinalDelay.isChecked) 50 else -1,
      colorKey = with(binding) {
        if (chipEnableColorKey.isChecked) Triple(
          viewColorKeyIndicator.backgroundColor.colorIntToHex(),
          sliderColorKeySimilarity.value.toInt(),
          sliderColorKeyBlend.value.toInt()
        )
        else null
      })
  }

  private fun renderPreviewImage(taskBuilder: TaskBuilderVideoToGifForPreview) = with(taskBuilder) {
    if (!fileExistsCache.contains(getCache_shortLength_colorKey_paletteuse())) {
      if (!fileExistsCache.contains(getCache_shortLength_colorKey_palettegen())) {
        if (!fileExistsCache.contains(getCache_shortLength_colorKey())) {
          if (!fileExistsCache.contains(getCache_shortLength())) {
            Bitmap.createScaledBitmap(
              frame, gifOutputWH(shortLength).first, gifOutputWH(shortLength).second, true
            ).saveToPng(getCache_shortLength())
            fileExistsCache.add(getCache_shortLength())
          }
          colorKey?.let {
            val command =
              "$FFMPEG_COMMAND_PREFIX_FOR_ALL_AN -i \"${getCache_shortLength()}\" -vf colorkey=#${it.first}:${it.second / 100f}:${it.third / 100f} -y \"${getCache_shortLength_colorKey()}\""
            logRed("colorKey cmd", command)
            FFmpegKit.execute(command)
          }
          fileExistsCache.add(getCache_shortLength_colorKey())
        }
        FFmpegKit.execute("$FFMPEG_COMMAND_PREFIX_FOR_ALL_AN -i \"${getCache_shortLength_colorKey()}\" -filter_complex palettegen=max_colors=$colorQuality:stats_mode=diff -y \"${getCache_shortLength_colorKey_palettegen()}\"")
        fileExistsCache.add(getCache_shortLength_colorKey_palettegen())
      }
      FFmpegKit.execute("$FFMPEG_COMMAND_PREFIX_FOR_ALL_AN -i \"${getCache_shortLength_colorKey()}\" -i ${getCache_shortLength_colorKey_palettegen()} -filter_complex \"[0:v][1:v] paletteuse=dither=bayer\" -y \"${getCache_shortLength_colorKey_paletteuse()}\"")
      fileExistsCache.add(getCache_shortLength_colorKey_paletteuse())
      previewBitmapMap[this.copy(lossy = null)] = BitmapFactory.decodeFile(getCache_shortLength_colorKey_paletteuse())
    }
    if (!previewBitmapMap.containsKey(this)) {
      gifsicleLossy(
        lossy!!, getCache_shortLength_colorKey_paletteuse(), getCache_shortLength_colorKey_paletteuse_lossy(), false
      )
      fileExistsCache.add(getCache_shortLength_colorKey_paletteuse_lossy())
      previewBitmapMap[this] = BitmapFactory.decodeFile(getCache_shortLength_colorKey_paletteuse_lossy())
    }
    previewBitmapMap[this]!!
  }

  private fun updatePreviewImage() {
    val taskBuilder = createTaskBuilder().getForPreviewOnly()
    binding.acivSingleFramePreview.setImageBitmap(
      renderPreviewImage(
        if (binding.chipEnableColorKey.isChecked && binding.mcbColorKeyPreview.isChecked) taskBuilder
        else taskBuilder.copy(colorKey = null)
      )
    )
  }

  private fun gifOutputWH(shortLength: Int) = vtgActivity.cropParams.calcScaledResolution(shortLength)

  private fun getUserCustomResolution(): Int {
    val inputValue = ("0" + binding.tietResolutionInputValue.text.toString()).toInt()
    val result = (if (inputValue == 0) 240 else if (inputValue % 2 == 0) inputValue else inputValue + 1).constraintBy(
      2..min(frame.width, frame.height)
    )
    logRed("getUserCustomResolution", result)
    return result
  }

  override fun onDestroyView() {
    vtgActivity.savedColorKeyColor = binding.viewColorKeyIndicator.backgroundColor
    super.onDestroyView()
    _binding = null
    previewBitmapMap.clear()
    makeDirEmpty(VIDEO_TO_GIF_PREVIEW_CACHE_DIR)
    vtgActivity.videoView.start()
  }

  private fun clearPreviewImageCache() {
    previewBitmapMap.clear()
    fileExistsCache.clear()
    makeDirEmpty(VIDEO_TO_GIF_PREVIEW_CACHE_DIR)
  }

  companion object {
    const val TAG = "VideoToGifExportOptionsDialogFragment"
  }
}