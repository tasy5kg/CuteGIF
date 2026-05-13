package com.nht.gif

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
import androidx.core.content.ContextCompat
import androidx.core.graphics.get
import androidx.core.widget.TextViewCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import com.arthenica.ffmpegkit.FFmpegKit
import com.nht.gif.model.EstimationState
import com.nht.gif.model.OutputFormat
import com.nht.gif.model.WebpQuality
import com.nht.gif.model.formatEstimatedSize
import com.nht.gif.ui.videotogif.VideoToGifExportOptionsViewModel
import com.nht.gif.MyConstants.FFMPEG_COMMAND_PREFIX_FOR_ALL_AN
import com.nht.gif.MyConstants.VIDEO_TO_GIF_PREVIEW_CACHE_DIR
import com.nht.gif.databinding.DialogFragmentVideoToGifExportOptionsBinding
import com.nht.gif.toolbox.FileTools.resetDirectory
import com.nht.gif.toolbox.MediaTools.getVideoSingleFrame
import com.nht.gif.toolbox.MediaTools.gifsicleLossy
import com.nht.gif.toolbox.MediaTools.saveToPng
import com.nht.gif.toolbox.Toolbox.backgroundColor
import com.nht.gif.toolbox.Toolbox.colorIntToHex
import com.nht.gif.toolbox.Toolbox.constraintBy
import com.nht.gif.toolbox.Toolbox.joinToStringSpecial
import com.nht.gif.toolbox.Toolbox.logRed
import com.nht.gif.toolbox.Toolbox.onClick
import com.nht.gif.toolbox.Toolbox.toast
import com.nht.gif.toolbox.Toolbox.visibleIf
import kotlin.math.min

class VideoToGifExportOptionsDialogFragment : DialogFragment() {
  private var _binding: DialogFragmentVideoToGifExportOptionsBinding? = null
  private val binding get() = _binding!!
  private val previewBitmapMap = mutableMapOf<TaskBuilderVideoToGifForPreview, Bitmap>()
  private val vtgActivity get() = activity as VideoToGifActivity
  private lateinit var frame: Bitmap
  private val viewModel: VideoToGifExportOptionsViewModel by lazy {
    ViewModelProvider(
      this,
      VideoToGifExportOptionsViewModel.factory(
        inputVideoPath = vtgActivity.inputVideoPath,
        duration = vtgActivity.videoView.duration,
        cropParams = vtgActivity.cropParams,
        outputSpeed = vtgActivity.playbackSpeed,
      )
    )[VideoToGifExportOptionsViewModel::class.java]
  }

  /** Determining whether a Key exists in a Map/Set is fast, while determining whether a file exists is much slower */
  private val fileExistsCache = mutableSetOf<String>()

  @SuppressLint("ClickableViewAccessibility")
  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
  ): View {
    _binding = DialogFragmentVideoToGifExportOptionsBinding.inflate(layoutInflater, container, false)

    // The system can restore this dialog fragment (e.g. after a permission-revoke restart) before
    // VideoToGifActivity.mediaPlayerReady() has been called. At that point the rangeSlider has only
    // its XML-default single-value list, so createTaskBuilder() would crash on values[1].
    // Dismiss immediately and let the user re-open the dialog once the activity is fully ready.
    if (!vtgActivity.isVideoReady) {
      dismissAllowingStateLoss()
      return binding.root
    }

    clearPreviewImageCache()
    vtgActivity.videoView.pause()
    binding.mbSave.onClick {
      vtgActivity.videoView.pause()
      VideoToGifPerformerActivity.start(vtgActivity, createTaskBuilder())
    }
    binding.mbtgOutputFormat.addOnButtonCheckedListener { _, checkedId, isChecked ->
      if (isChecked) {
        val format = if (checkedId == binding.mbOutputFormatWebp.id) OutputFormat.ANIMATED_WEBP else OutputFormat.GIF
        viewModel.setOutputFormat(format)
      }
    }
    viewLifecycleOwner.lifecycleScope.launch {
      viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
        launch {
          viewModel.outputFormat.collect { format ->
            val isGif = format == OutputFormat.GIF
            binding.llcRowGifImageQuality.visibleIf { isGif }
            binding.dividerGifControls.root.visibleIf { isGif }
            binding.llcRowGifColorQuality.visibleIf { isGif }
            binding.chipEnableFinalDelay.visibleIf { isGif }
            binding.llcWebpQualitySection.visibleIf { !isGif }
            val activeColor = ContextCompat.getColor(requireContext(), R.color.green_dark)
            val inactiveColor = ContextCompat.getColor(requireContext(), R.color.grey)
            TextViewCompat.setTextAppearance(binding.mtvEstimatedGifSize,
              if (isGif) R.style.TextAppearance_App_EstSize_Active
              else R.style.TextAppearance_App_EstSize_Inactive)
            binding.mtvEstimatedGifSize.setTextColor(if (isGif) activeColor else inactiveColor)
            TextViewCompat.setTextAppearance(binding.mtvEstimatedWebpSize,
              if (isGif) R.style.TextAppearance_App_EstSize_Inactive
              else R.style.TextAppearance_App_EstSize_Active)
            binding.mtvEstimatedWebpSize.setTextColor(if (isGif) inactiveColor else activeColor)
          }
        }
        launch {
          viewModel.webpQuality.collect { quality ->
            val targetId = when (quality) {
              WebpQuality.SMALL -> binding.mbWebpQualitySmall.id
              WebpQuality.MEDIUM -> binding.mbWebpQualityMedium.id
              WebpQuality.HIGH -> binding.mbWebpQualityHigh.id
              WebpQuality.LOSSLESS -> binding.mbWebpQualityLossless.id
            }
            if (binding.mbtgWebpQuality.checkedButtonId != targetId) {
              binding.mbtgWebpQuality.check(targetId)
            }
          }
        }
        launch {
          viewModel.showLosslessWarning.collect { show ->
            binding.mtvLosslessWarning.visibleIf { show }
          }
        }
        launch {
          viewModel.estimationState.collect { state ->
            val isLoading = state is EstimationState.Loading
            binding.cpiEstimation.visibleIf { isLoading }
            binding.mtvEstimatedGifSize.visibleIf { !isLoading }
            binding.mtvEstimatedWebpSize.visibleIf { !isLoading }
            if (!isLoading) updateSizeText(state)
          }
        }
      }
    }
    binding.mbtgWebpQuality.addOnButtonCheckedListener { _, checkedId, isChecked ->
      if (isChecked) {
        val quality = when (checkedId) {
          binding.mbWebpQualitySmall.id -> WebpQuality.SMALL
          binding.mbWebpQualityMedium.id -> WebpQuality.MEDIUM
          binding.mbWebpQualityHigh.id -> WebpQuality.HIGH
          binding.mbWebpQualityLossless.id -> WebpQuality.LOSSLESS
          else -> return@addOnButtonCheckedListener
        }
        if (viewModel.webpQuality.value != quality) viewModel.setWebpQuality(quality)
      }
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
    binding.viewColorKeyIndicator.onClick { toast(R.string.click_on_the_preview_image_to_pick_an_color) }
    binding.sliderColorKeySimilarity.apply {
      setLabelFormatter { "${it.toInt()}%" }
      addOnChangeListener { _, _, _ ->
        performHapticFeedback(HapticFeedbackConstants.TEXT_HANDLE_MOVE)
        updatePreviewImage()
      }
    }
    binding.tietResolutionInputValue.doAfterTextChanged {
      updatePreviewImage()
      viewModel.setShortLength(getSelectedShortLength())
    }
    binding.mbtgColorQuality.addOnButtonCheckedListener { group, checkedId, isChecked ->
      if (isChecked) {
        updatePreviewImage()
        group.performHapticFeedback(HapticFeedbackType.SWITCH_TOGGLING)
        val colorQuality = when (checkedId) {
          binding.mbColorQualityLow.id -> 32
          binding.mbColorQualityMid.id -> 64
          binding.mbColorQualityHigh.id -> 128
          binding.mbColorQualityMax.id -> 256
          else -> return@addOnButtonCheckedListener
        }
        viewModel.setColorQuality(colorQuality)
      }
    }

    binding.mbtgResolution.addOnButtonCheckedListener { group, checkedId, isChecked ->
      if (isChecked) {
        group.performHapticFeedback(HapticFeedbackType.SWITCH_TOGGLING)
        binding.llcGroupResolutionInput.visibleIf { checkedId == binding.mbResolutionCustom.id }
        if (checkedId == binding.mbResolutionCustom.id) binding.tietResolutionInputValue.requestFocus()
        updatePreviewImage()
        viewModel.setShortLength(getSelectedShortLength())
      }
    }
    binding.mbtgImageQuality.addOnButtonCheckedListener { group, checkedId, isChecked ->
      if (isChecked) {
        updatePreviewImage()
        group.performHapticFeedback(HapticFeedbackType.SWITCH_TOGGLING)
        val lossy = when (checkedId) {
          binding.mbImageQualityLow.id -> 200
          binding.mbImageQualityMid.id -> 70
          binding.mbImageQualityHigh.id -> 30
          binding.mbImageQualityMax.id -> null
          else -> return@addOnButtonCheckedListener
        }
        viewModel.setLossy(lossy)
      }
    }
    binding.mbtgFramerate.addOnButtonCheckedListener { group, checkedId, isChecked ->
      if (isChecked) {
        binding.mtvFramerateOver10Warning.visibleIf { createTaskBuilder().outputFps > 10 }
        group.performHapticFeedback(HapticFeedbackType.SWITCH_TOGGLING)
        val fps = when (checkedId) {
          binding.mbFramerate5.id -> 5
          binding.mbFramerate10.id -> 10
          binding.mbFramerate16.id -> 16
          binding.mbFramerate25.id -> 25
          binding.mbFramerate50.id -> 50
          else -> return@addOnButtonCheckedListener
        }
        viewModel.setFps(fps)
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
    TaskBuilderVideoToGif(
      trimTime = with(rangeSlider) {
        if ((values[0] * 100).toInt() == 0 && (values[1] * 100).toInt() == videoView.duration) null
        else ((values[0] * 100).toInt() to (values[1] * 100).toInt())
      },
      inputVideoPath = inputVideoPath,
      cropParams = cropParams,
      shortLength = getSelectedShortLength(),
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
        if (chipEnableColorKey.isChecked)
          (viewColorKeyIndicator.backgroundColor.colorIntToHex() to sliderColorKeySimilarity.value.toInt())
        else null
      },
      outputFormat = viewModel.outputFormat.value,
      webpQuality = if (viewModel.outputFormat.value == OutputFormat.ANIMATED_WEBP) viewModel.webpQuality.value else null,
    )
  }

  private fun renderPreviewImage(taskBuilder: TaskBuilderVideoToGifForPreview) = with(taskBuilder) {
    if (!fileExistsCache.contains(getCache_shortLength_colorKey_paletteuse())) {
      if (!fileExistsCache.contains(getCache_shortLength_colorKey_palettegen())) {
        if (!fileExistsCache.contains(getCache_shortLength_colorKey())) {
          if (!fileExistsCache.contains(getCache_shortLength())) {
            Bitmap.createScaledBitmap(frame, gifOutputWH(shortLength).first, gifOutputWH(shortLength).second, true).saveToPng(getCache_shortLength())
            fileExistsCache.add(getCache_shortLength())
          }
          colorKey?.let {
            val command =
              "$FFMPEG_COMMAND_PREFIX_FOR_ALL_AN -i \"${getCache_shortLength()}\" -vf colorkey=#${it.first}:${it.second / 100f}:0 -y \"${getCache_shortLength_colorKey()}\""
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

  /**
   * Sets the text of both size labels, appending a quality qualifier to the inactive format's
   * label because its quality controls are hidden when the other format is selected.
   */
  private fun updateSizeText(state: EstimationState) {
    val isGif = viewModel.outputFormat.value == OutputFormat.GIF
    val (gifText, webpText) = when (state) {
      is EstimationState.Ready ->
        "GIF ${formatEstimatedSize(state.gifSizeBytes)}" to
          "WebP ${formatEstimatedSize(state.webpSizeBytes)}"
      else -> "GIF —" to "WebP —"
    }
    binding.mtvEstimatedGifSize.text =
      if (!isGif) "$gifText · ${clarityLabel()}" else gifText
    binding.mtvEstimatedWebpSize.text =
      if (isGif) "$webpText · ${webpQualityLabel()}" else webpText
  }

  private fun webpQualityLabel() = when (viewModel.webpQuality.value) {
    WebpQuality.SMALL -> getString(R.string.low)
    WebpQuality.MEDIUM -> getString(R.string.mid)
    WebpQuality.HIGH -> getString(R.string.high)
    WebpQuality.LOSSLESS -> getString(R.string.best)
  }

  private fun clarityLabel() = when (viewModel.lossy.value) {
    200 -> getString(R.string.low)
    70 -> getString(R.string.mid)
    30 -> getString(R.string.high)
    null -> getString(R.string.max)
    else -> getString(R.string.high)
  }

  override fun onDestroyView() {
    vtgActivity.savedColorKeyColor = binding.viewColorKeyIndicator.backgroundColor
    super.onDestroyView()
    _binding = null
    previewBitmapMap.clear()
    resetDirectory(VIDEO_TO_GIF_PREVIEW_CACHE_DIR)
    // Only resume playback if the video was actually prepared; if we dismissed early due to the
    // activity not being ready, starting playback here would be premature.
    if (vtgActivity.isVideoReady) vtgActivity.videoView.start()
  }

  private fun clearPreviewImageCache() {
    previewBitmapMap.clear()
    fileExistsCache.clear()
    resetDirectory(VIDEO_TO_GIF_PREVIEW_CACHE_DIR)
  }

  companion object {
    const val TAG = "VideoToGifExportOptionsDialogFragment"
  }
}