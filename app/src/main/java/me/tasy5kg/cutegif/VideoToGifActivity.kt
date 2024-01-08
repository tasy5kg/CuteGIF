package me.tasy5kg.cutegif

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.content.res.AppCompatResources
import me.tasy5kg.cutegif.MyApplication.Companion.appContext
import me.tasy5kg.cutegif.MyConstants.EXTRA_ADD_TEXT_RENDER
import me.tasy5kg.cutegif.MyConstants.EXTRA_VIDEO_PATH
import me.tasy5kg.cutegif.bottom.sheet.BottomSheetVideoToGif2CropRatio
import me.tasy5kg.cutegif.bottom.sheet.BottomSheetVideoToGif2PlaybackSpeed
import me.tasy5kg.cutegif.databinding.ActivityVideoToGifBinding
import me.tasy5kg.cutegif.toolbox.MediaTools.getVideoDurationByAndroidSystem
import me.tasy5kg.cutegif.toolbox.Toolbox.constraintBy
import me.tasy5kg.cutegif.toolbox.Toolbox.getExtra
import me.tasy5kg.cutegif.toolbox.Toolbox.newRunnableWithSelf
import me.tasy5kg.cutegif.toolbox.Toolbox.onClick
import me.tasy5kg.cutegif.toolbox.Toolbox.onSliderTouch
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToLong

class VideoToGifActivity : BaseActivity() {
  private val binding by lazy { ActivityVideoToGifBinding.inflate(layoutInflater) }
  private val bottomSheetVideoToGif2PlaybackSpeed by lazy { BottomSheetVideoToGif2PlaybackSpeed() }
  private val videoToGifExportOptionsDialogFragment by lazy { VideoToGifExportOptionsDialogFragment() }
  private val bottomSheetVideoToGif2CropRatio by lazy { BottomSheetVideoToGif2CropRatio() }
  private val videoDuration by lazy { getVideoDurationByAndroidSystem(inputVideoPath) }
  val cropParams get() = CropParams(binding.cropImageView.cropRect!!)
  val inputVideoPath by lazy { intent.getExtra<String>(EXTRA_VIDEO_PATH) }

  val videoView by lazy { binding.videoView }
  private val rangeSlider by lazy { binding.rangeSlider } // value 1.0f == 100ms

  private var videoLastPosition = 0L // 1 == 1ms
  private var playbackSpeed = 1f
  var textRender: TextRender? = null

  private lateinit var videoWH: Pair<Int, Int>
  private lateinit var mMediaPlayer: MediaPlayer

  private val loopRunnable by lazy {
    newRunnableWithSelf { self ->
      try {
        val currentPosition = videoView.currentPosition / 100
        binding.sliderCursor.value =
          (videoView.currentPosition / 100f).constraintBy(0f..binding.sliderCursor.valueTo)
        if ((currentPosition < rangeSlider.values[0] && currentPosition < videoLastPosition) || (currentPosition > rangeSlider.values[1])) {
          seekMediaPlayer(rangeSlider.values[0])
        }
        videoLastPosition = currentPosition / 100L
        binding.mtvVideoCurrentPosition.text = msToMinSecDs(videoView.currentPosition)
        binding.mbPlayPause.setIconResource(if (videoView.isPlaying) R.drawable.ic_baseline_pause_24 else R.drawable.ic_baseline_play_arrow_24)
      } catch (e: Exception) {
        e.printStackTrace()
      }
      videoView.postDelayed(self, 50)
    }
  }

  private val getAddTextResult =
    registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { activityResult ->
      if (activityResult.resultCode == RESULT_OK) {
        textRender = activityResult.data?.getExtra(EXTRA_ADD_TEXT_RENDER)
        textRender?.let { binding.acivText.setImageBitmap(it.toBitmap(videoWH.first, videoWH.second)) }
      }
    }

  override fun onCreateIfEulaAccepted(savedInstanceState: Bundle?) {
    // CropImageView cannot automatically resize itself when the parent layout changes size. Help it resize here.
    binding.rl.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
      binding.cropImageView.layoutParams.height = MATCH_PARENT
    }
    setContentView(binding.root)
    if (videoDuration == null) {
      VideoToGifVideoFallbackActivity.start(this@VideoToGifActivity, inputVideoPath)
      finish()
      return
    }
    videoView.apply {
      setAudioFocusRequest(AudioManager.AUDIOFOCUS_NONE)
      setOnErrorListener { _, _, _ ->
        stopPlayback()
        VideoToGifVideoFallbackActivity.start(this@VideoToGifActivity, inputVideoPath)
        finish()
        return@setOnErrorListener true
      }
      setOnPreparedListener {
        mediaPlayerReady(it)
      }
      setVideoPath(inputVideoPath)
    }
    binding.mbClose.setOnClickListener { finish() }
    binding.mbSave.onClick(HapticFeedbackType.CONFIRM) {
      videoView.pause()
      supportFragmentManager.beginTransaction().add(videoToGifExportOptionsDialogFragment, VideoToGifExportOptionsDialogFragment.TAG).commit()
    }
  }

  fun startVideoToGifPerformer() {
    videoView.pause()
    VideoToGifPerformerActivity.start(this@VideoToGifActivity, createTaskBuilderBase())
  }

  private fun createTaskBuilderBase() = TaskBuilderVideoToGif(
    trimTime = with(rangeSlider) {
      if ((values[0] * 100).toInt() == 0 && (values[1] * 100).toInt() == videoView.duration) null else ((values[0] * 100).toInt() to (values[1] * 100).toInt())
    },
    inputVideoPath = inputVideoPath,
    cropParams = cropParams,
    resolutionShortLength = videoToGifExportOptionsDialogFragment.getImageResolutionValue(),
    outputSpeed = playbackSpeed,
    outputFps = videoToGifExportOptionsDialogFragment.getFramerateValue(),
    colorQuality = videoToGifExportOptionsDialogFragment.getColorQualityValue(),
    reverse = videoToGifExportOptionsDialogFragment.getReverseValue(),
    textRender = textRender,
    lossy = videoToGifExportOptionsDialogFragment.getLossyValue(),
    videoWH = videoWH,
    duration = videoView.duration,
    finalDelay = videoToGifExportOptionsDialogFragment.getFinalDelayValue()
  )

  private fun mediaPlayerReady(mediaPlayer: MediaPlayer) {
    val notInitializedBefore = !::mMediaPlayer.isInitialized
    mMediaPlayer = mediaPlayer.apply {
      setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT)
      isLooping = true
      playbackParams = playbackParams.setSpeed(playbackSpeed)
    }
    if (notInitializedBefore) {
      videoWH = with(mediaPlayer) { videoWidth to videoHeight }
      binding.cropImageView.apply {
        setBackgroundColor(Color.TRANSPARENT)
        setImageBitmap(Bitmap.createBitmap(videoWH.first, videoWH.second, Bitmap.Config.ALPHA_8).apply { eraseColor(Color.TRANSPARENT) })
        cropRect = wholeImageRect
      }

      rangeSlider.apply {
        valueFrom = 0f
        valueTo = videoView.duration / 100f
        values = listOf(valueFrom, valueTo)
        setMinSeparationValue(1f)
        setLabelFormatter { msToMinSecDs((it * 100).toInt()) }
        updateGifDuration()
        onSliderTouch(onStartTrackingTouch = {
          mMediaPlayer.pause()
        }, onStopTrackingTouch = { mMediaPlayer.start() })
        var previousValuesHapticFeedbacked = values
        addOnChangeListener { slider, value, _ ->
          seekMediaPlayer(value)
          updateGifDuration()
          if (abs(previousValuesHapticFeedbacked[0] - values[0]) >= 1f || abs(previousValuesHapticFeedbacked[1] - values[1]) >= 1f) {
            slider.performHapticFeedback(HapticFeedbackConstants.TEXT_HANDLE_MOVE)
            previousValuesHapticFeedbacked = values
          }
        }
      }
      binding.sliderCursor.apply {
        valueFrom = 0f
        valueTo = videoView.duration / 100f
        setCustomThumbDrawable(AppCompatResources.getDrawable(appContext, (R.drawable.cursor))!!)
      }
      binding.mbPlaybackSpeed.setOnClickListener {
        bottomSheetVideoToGif2PlaybackSpeed.show(supportFragmentManager, BottomSheetVideoToGif2PlaybackSpeed.TAG)
      }
      binding.mbCropRatio.setOnClickListener {
        bottomSheetVideoToGif2CropRatio.show(supportFragmentManager, BottomSheetVideoToGif2CropRatio.TAG)
      }
      binding.mbAddText.setOnClickListener {
        mMediaPlayer.pause()
        getAddTextResult.launch(AddTextActivity.startIntent(this, inputVideoPath, videoView.currentPosition.toLong(), textRender, videoWH))
      }
      binding.mbPlayPause.setOnClickListener { mMediaPlayer.apply { if (isPlaying) pause() else start() } }
    }
  }

  private fun seekMediaPlayer(sliderValue: Float) =
    mMediaPlayer.seekTo((sliderValue * 100).roundToLong(), MediaPlayer.SEEK_CLOSEST)

  private fun updateGifDuration() {
    binding.mtvGifDurationS.text =
      getString(R.string.gif_duration_s, String.format("%.1f", with(rangeSlider.values) { ((this[1] - this[0]) * 100f).toInt() } / 1000f / playbackSpeed))
  }

  override fun onPause() {
    super.onPause()
    videoView.pause()
    videoView.removeCallbacks(loopRunnable)
  }

  override fun onResume() {
    super.onResume()
    videoView.start()
    videoView.postDelayed(loopRunnable, 50)
  }

  fun setPlaybackSpeed(speed: Float, text: String) {
    binding.mbPlaybackSpeed.text = text
    playbackSpeed = speed
    // set playback speed too high may cause exceptions
    try {
      mMediaPlayer.playbackParams.speed = playbackSpeed
    } catch (e: Exception) {
      e.printStackTrace()
    }
    updateGifDuration()
  }

  fun setCropRatio(ratio: Pair<Int, Int>?) {
    binding.cropImageView.apply {
      clearAspectRatio()
      cropRect = wholeImageRect
      ratio?.let { setAspectRatio(it.first, ratio.second) }
    }
    binding.mbCropRatio.text = BottomSheetVideoToGif2CropRatio.cropRatioToText(ratio)
  }

  companion object {
    fun start(context: Context, inputVideoPath: String) =
      context.startActivity(Intent(context, VideoToGifActivity::class.java).putExtra(EXTRA_VIDEO_PATH, inputVideoPath))

    fun intentAddTextResult(textRender: TextRender) =
      Intent().putExtra(EXTRA_ADD_TEXT_RENDER, textRender)

    /** Example: 123456L -> "2:03.4" */
    private fun msToMinSecDs(ms: Int) =
      with(ms / 1000f) { "${(this / 60).toInt()}:${String.format("%02d", (this % 60).toInt())}.${((this - floor(this)) * 10).toInt()}" }
  }
}