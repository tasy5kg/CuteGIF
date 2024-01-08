package me.tasy5kg.cutegif

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.arthenica.ffmpegkit.FFmpegKit
import me.tasy5kg.cutegif.MyConstants.FFMPEG_COMMAND_PREFIX_FOR_ALL_AN
import me.tasy5kg.cutegif.MyConstants.VIDEO_TO_GIF_EXPORT_OPTIONS_PREVIEW_DIR
import me.tasy5kg.cutegif.databinding.DialogFragmentVideoToGifExportOptionsBinding
import me.tasy5kg.cutegif.toolbox.FileTools.makeDirEmpty
import me.tasy5kg.cutegif.toolbox.MediaTools.generateTransparentBitmap
import me.tasy5kg.cutegif.toolbox.MediaTools.getVideoSingleFrame
import me.tasy5kg.cutegif.toolbox.MediaTools.gifsicleLossy
import me.tasy5kg.cutegif.toolbox.MediaTools.saveToPng
import me.tasy5kg.cutegif.toolbox.Toolbox.onClick
import java.io.File

class VideoToGifExportOptionsDialogFragment : DialogFragment() {
  private var _binding: DialogFragmentVideoToGifExportOptionsBinding? = null
  private val binding get() = _binding!!
  private val previewBitmapMap = mutableMapOf<String, Bitmap>()
  private val videoToGifActivity get() = activity as VideoToGifActivity
  private lateinit var frame: Bitmap
  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
    _binding = DialogFragmentVideoToGifExportOptionsBinding.inflate(layoutInflater, container, false)
    makeDirEmpty(VIDEO_TO_GIF_EXPORT_OPTIONS_PREVIEW_DIR)
    videoToGifActivity.videoView.pause()
    previewBitmapMap.clear()
    binding.mbSave.onClick { videoToGifActivity.startVideoToGifPerformer() }
    binding.mtvMoreOptions.onClick {
      visibility = View.GONE
      binding.llcMoreOptions.visibility = View.VISIBLE
    }
    binding.sliderResolution.apply {
      setLabelFormatter { with(gifOutputWH(binding.sliderResolution.value.toInt())) { "${first}x${second}" } }
      addOnChangeListener { slider, value, _ ->
        performHapticFeedback(HapticFeedbackConstants.TEXT_HANDLE_MOVE)
        updatePreviewImage()
      }
    }
    binding.mbtgColorQuality.addOnButtonCheckedListener { group, checkedId, isChecked ->
      updatePreviewImage()
      if (isChecked) {
        group.performHapticFeedback(MyConstants.HAPTIC_FEEDBACK_TYPE_SWITCH_TOGGLING)
      }
    }
    binding.mbtgImageQuality.addOnButtonCheckedListener { group, checkedId, isChecked ->
      updatePreviewImage()
      if (isChecked) {
        group.performHapticFeedback(MyConstants.HAPTIC_FEEDBACK_TYPE_SWITCH_TOGGLING)
      }
    }
    binding.mbtgFramerate.addOnButtonCheckedListener { group, checkedId, isChecked ->
      if (isChecked) {
        group.performHapticFeedback(MyConstants.HAPTIC_FEEDBACK_TYPE_SWITCH_TOGGLING)
      }
    }

    binding.mbClose.onClick { dismiss() }
    frame = getVideoSingleFrame(videoToGifActivity.inputVideoPath, videoToGifActivity.videoView.currentPosition.toLong())
    // Merge the text layer with the frame
    Canvas(frame).drawBitmap(videoToGifActivity.textRender?.toBitmap(frame.width, frame.height) ?: generateTransparentBitmap(1, 1), 0f, 0f, null)
    // Crop
    frame = Bitmap.createBitmap(
      frame,
      videoToGifActivity.cropParams.x,
      videoToGifActivity.cropParams.y,
      videoToGifActivity.cropParams.outW,
      videoToGifActivity.cropParams.outH
    )
    updatePreviewImage()
    return binding.root
  }

  private fun updatePreviewImage() {
    val shortLength = binding.sliderResolution.value.toInt()
    val colorQuality = getColorQualityValue()
    val lossy = getLossyValue()
    //val colorQualityReduced = colorQuality / 4 * 3
    val colorQualityReduced = colorQuality
    val previewImagePath_short_color_lossy = "$VIDEO_TO_GIF_EXPORT_OPTIONS_PREVIEW_DIR${shortLength}_${colorQualityReduced}_${lossy}.gif"
    val previewImagePath_short_color = "$VIDEO_TO_GIF_EXPORT_OPTIONS_PREVIEW_DIR${shortLength}_${colorQualityReduced}.gif"
    val previewPalettePath_short_color = "$VIDEO_TO_GIF_EXPORT_OPTIONS_PREVIEW_DIR${shortLength}_${colorQualityReduced}.png"
    val previewImagePath_short = "$VIDEO_TO_GIF_EXPORT_OPTIONS_PREVIEW_DIR${shortLength}.png"
    if (!File(previewImagePath_short_color_lossy).exists()) {
      if (!File(previewImagePath_short_color).exists()) {
        if (!File(previewPalettePath_short_color).exists()) {
          if (!File(previewImagePath_short).exists()) {
            Bitmap.createScaledBitmap(frame, gifOutputWH(shortLength).first, gifOutputWH(shortLength).second, true).saveToPng(previewImagePath_short)
          }
          FFmpegKit.execute("$FFMPEG_COMMAND_PREFIX_FOR_ALL_AN -i $previewImagePath_short -filter_complex palettegen=max_colors=${colorQualityReduced}:stats_mode=diff -y $previewPalettePath_short_color")
        }
        FFmpegKit.execute("$FFMPEG_COMMAND_PREFIX_FOR_ALL_AN -i $previewImagePath_short -i $previewPalettePath_short_color -filter_complex \"[0:v][1:v] paletteuse=dither=bayer\" -y $previewImagePath_short_color")
      }
     gifsicleLossy(lossy, previewImagePath_short_color, previewImagePath_short_color_lossy, false)
      previewBitmapMap["${shortLength}_${colorQualityReduced}_${lossy}"] = BitmapFactory.decodeFile(previewImagePath_short_color_lossy)
    }
    binding.acivSingleFramePreview.setImageBitmap(previewBitmapMap["${shortLength}_${colorQualityReduced}_${lossy}"])

  }

  private fun gifOutputWH(shortLength: Int) = videoToGifActivity.cropParams.calcScaledResolution(shortLength)

  fun getLossyValue() = when (binding.mbtgImageQuality.checkedButtonId) {
    R.id.mb_image_quality_low -> 200
    R.id.mb_image_quality_mid -> 70
    R.id.mb_image_quality_high -> 30
    else -> throw IllegalArgumentException()
  }

  /** The interval between every loops, in centiseconds. (1 == 0.01 sec) */
  fun getFinalDelayValue() = -1/*TODO when (binding.mcbFinalDelay.isChecked) {
    true -> 50 // 0.5 sec
    false -> -1 // no final delay
  }*/

  fun getImageResolutionValue() = binding.sliderResolution.value.toInt()

  fun getFramerateValue() = when (binding.mbtgFramerate.checkedButtonId) {
    binding.mbFramerate5.id -> 5
    binding.mbFramerate10.id -> 10
    binding.mbFramerate15.id -> 15
    binding.mbFramerate25.id -> 25
    binding.mbFramerate50.id -> 50
    else -> throw IllegalArgumentException()
  }

  fun getColorQualityValue() = when (binding.mbtgColorQuality.checkedButtonId) {
    R.id.mb_color_quality_low -> 64
    R.id.mb_color_quality_mid -> 128
    R.id.mb_color_quality_high -> 256
    else -> throw IllegalArgumentException()
  }

  // TODO fun getReverseValue() = binding.mcbReverseVideo.isChecked
  fun getReverseValue() = true

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
    previewBitmapMap.clear()
    makeDirEmpty(VIDEO_TO_GIF_EXPORT_OPTIONS_PREVIEW_DIR)
    videoToGifActivity.videoView.start()
  }

  companion object {
    const val TAG = "VideoToGifExportOptionsDialogFragment"
  }
}