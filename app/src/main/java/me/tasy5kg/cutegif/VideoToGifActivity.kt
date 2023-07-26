package me.tasy5kg.cutegif

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.content.res.AppCompatResources
import me.tasy5kg.cutegif.MyApplication.Companion.appContext
import me.tasy5kg.cutegif.MyConstants.EXTRA_ADD_TEXT_RENDER
import me.tasy5kg.cutegif.MyConstants.EXTRA_VIDEO_URI
import me.tasy5kg.cutegif.Toolbox.constraintBy
import me.tasy5kg.cutegif.Toolbox.getExtra
import me.tasy5kg.cutegif.Toolbox.logRed
import me.tasy5kg.cutegif.Toolbox.newRunnableWithSelf
import me.tasy5kg.cutegif.Toolbox.onClick
import me.tasy5kg.cutegif.Toolbox.onSliderTouch
import me.tasy5kg.cutegif.bottom.sheet.BottomSheetVideoToGif2CropRatio
import me.tasy5kg.cutegif.bottom.sheet.BottomSheetVideoToGif2MoreOptions
import me.tasy5kg.cutegif.bottom.sheet.BottomSheetVideoToGif2PlaybackSpeed
import me.tasy5kg.cutegif.databinding.ActivityVideoToGifBinding
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToLong

class VideoToGifActivity : BaseActivity() {
  private val binding by lazy { ActivityVideoToGifBinding.inflate(layoutInflater) }
  private val bottomSheetVideoToGif2PlaybackSpeed by lazy { BottomSheetVideoToGif2PlaybackSpeed() }
  private val bottomSheetVideoToGif2MoreOptions by lazy { BottomSheetVideoToGif2MoreOptions() }
  private val bottomSheetVideoToGif2CropRatio by lazy { BottomSheetVideoToGif2CropRatio() }
  private val inputVideoUri by lazy {
    with(intent) {
      getParcelableExtra(EXTRA_VIDEO_URI) ?: getParcelableExtra(Intent.EXTRA_STREAM) ?: data ?: Uri.EMPTY
    }
  }
  private val loopRunnable by lazy {
    newRunnableWithSelf { self ->
      try {
        val currentPosition = videoView.currentPosition / 100
        sliderCursor.value =
          (videoView.currentPosition / 100f).constraintBy(0f..sliderCursor.valueTo)
        if ((currentPosition < rangeSlider.values[0] && currentPosition < videoLastPosition)
          || (currentPosition > rangeSlider.values[1])
        ) {
          seekMediaPlayer(rangeSlider.values[0])
        }
        videoLastPosition = currentPosition / 100
        mtvVideoCurrentPosition.text = msToMinSecDs(videoView.currentPosition)

        mbPlayPause.setIconResource(if (videoView.isPlaying) R.drawable.ic_baseline_pause_24 else R.drawable.ic_baseline_play_arrow_24)
      } catch (e: Exception) {
        e.printStackTrace()
      }
      videoView.postDelayed(self, 50)
    }
  }
  private val videoView by lazy { binding.videoView }
  private val acivText by lazy { binding.acivText }
  private val mtvVideoCurrentPosition by lazy { binding.mtvVideoCurrentPosition }
  private val mtvGifDurationS by lazy { binding.mtvGifDurationS }
  private val rangeSlider by lazy { binding.rangeSlider } // value 1.0f == 100ms
  private val sliderCursor by lazy { binding.sliderCursor }
  private val cropImageView by lazy { binding.cropImageView }
  private val mbPlaybackSpeed by lazy { binding.mbPlaybackSpeed }
  private val mbPlayPause by lazy { binding.mbPlayPause }
  private val mbCropRatio by lazy { binding.mbCropRatio }
  private val mbAddText by lazy { binding.mbAddText }

  private var videoLastPosition = 0 // 1 == 1ms
  private var playbackSpeed = 1.5f
  private var textRender: TextRender? = null

  private lateinit var videoWH: Pair<Int, Int>
  private lateinit var mMediaPlayer: MediaPlayer

  private val getAddTextResult =
    registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { activityResult ->
      if (activityResult.resultCode == RESULT_OK) {
        textRender = activityResult.data?.getExtra(EXTRA_ADD_TEXT_RENDER)
        textRender?.let {
          acivText.setImageBitmap(it.toBitmap(videoWH.first, videoWH.second))
        }
      }
    }

  override fun onCreateIfEulaAccepted(savedInstanceState: Bundle?) {
    if (inputVideoUri == Uri.EMPTY) {
      loadVideoFailed()
      return
    }
    // CropImageView cannot automatically resize itself when the parent layout changes size. Help it resize here.
    binding.rl.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
      cropImageView.layoutParams.height = MATCH_PARENT
    }

    setContentView(binding.root)
    videoView.apply {
      setAudioFocusRequest(AudioManager.AUDIOFOCUS_NONE)
      setOnErrorListener { _, _, _ ->
        stopPlayback()
        VideoToGifVideoFallbackActivity.start(this@VideoToGifActivity, inputVideoUri)
        finish()
        return@setOnErrorListener true
      }
      setOnPreparedListener { mediaPlayerReady(it) }
      setVideoURI(inputVideoUri)
    }
    binding.mbClose.setOnClickListener { finish() }
    binding.mbSave.onClick(HapticFeedbackType.CONFIRM) {
      bottomSheetVideoToGif2MoreOptions.show(
        supportFragmentManager,
        BottomSheetVideoToGif2MoreOptions.TAG
      )
    }
  }

  fun startVideoToGifPerformer() {
    videoView.pause()
    VideoToGifPerformerActivity.start(this@VideoToGifActivity, createTaskBuilderBase())
  }

  private fun createTaskBuilderBase(): TaskBuilderVideoToGif {
    val inputVideoUriWrapper = Toolbox.UriWrapper(inputVideoUri)
    val trimTime = with(rangeSlider) { (values[0] * 100).toInt() to (values[1] * 100).toInt() }
    val cropParams = CropParams(cropImageView.cropRect!!)
    val outputSpeed = playbackSpeed
    val outputFps = bottomSheetVideoToGif2MoreOptions.getFramerateValue()
    val colorQuality = bottomSheetVideoToGif2MoreOptions.getColorQualityValue()
    val reverse = bottomSheetVideoToGif2MoreOptions.getReverseValue()
    val textRender = textRender
    val resolutionShortLength = bottomSheetVideoToGif2MoreOptions.getImageResolutionValue()
    val lossy = bottomSheetVideoToGif2MoreOptions.getLossyValue()
    val sliderTouched =
      rangeSlider.values[0] != rangeSlider.valueFrom || rangeSlider.values[1] != rangeSlider.valueTo
    return TaskBuilderVideoToGif(
      inputVideoUriWrapper = inputVideoUriWrapper,
      trimTime = if (sliderTouched) trimTime else null,
      cropParams = cropParams,
      resolutionShortLength = resolutionShortLength,
      outputSpeed = outputSpeed,
      outputFps = outputFps,
      colorQuality = colorQuality,
      reverse = reverse,
      textRender = textRender,
      lossy = lossy,
      videoWH = videoWH,
      duration = videoView.duration
    )

  }

  private fun mediaPlayerReady(mediaPlayer: MediaPlayer) {
    val notInitializedBefore = !::mMediaPlayer.isInitialized
    mMediaPlayer = mediaPlayer.apply {
      //   setVolume(0f, 0f)
      setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT)
      isLooping = true
      playbackParams = playbackParams.setSpeed(playbackSpeed)
    }
    if (notInitializedBefore) {
      videoWH = with(mediaPlayer) { videoWidth to videoHeight }
      cropImageView.apply {
        setBackgroundColor(Color.TRANSPARENT)
        setImageBitmap(
          Bitmap.createBitmap(
            videoWH.first,
            videoWH.second,
            Bitmap.Config.ALPHA_8
          ).apply { eraseColor(Color.TRANSPARENT) })
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
          if (abs(previousValuesHapticFeedbacked[0] - values[0]) >= 1f || abs(
              previousValuesHapticFeedbacked[1] - values[1]
            ) >= 1f
          ) {
            slider.performHapticFeedback(HapticFeedbackConstants.TEXT_HANDLE_MOVE)
            previousValuesHapticFeedbacked = values
          }
        }
      }
      sliderCursor.apply {
        valueFrom = 0f
        valueTo = videoView.duration / 100f
        setCustomThumbDrawable(
          AppCompatResources.getDrawable(
            appContext,
            (R.drawable.cursor)
          )!!
        )
      }
      mbPlaybackSpeed.setOnClickListener {
        bottomSheetVideoToGif2PlaybackSpeed.show(
          supportFragmentManager,
          BottomSheetVideoToGif2PlaybackSpeed.TAG
        )
      }
      mbCropRatio.setOnClickListener {
        bottomSheetVideoToGif2CropRatio.show(
          supportFragmentManager,
          BottomSheetVideoToGif2CropRatio.TAG
        )
      }
      mbAddText.setOnClickListener {
        mMediaPlayer.pause()
        getAddTextResult.launch(
          AddTextActivity.startIntent(
            this,
            inputVideoUri,
            videoView.currentPosition.toLong(),
            textRender,
            videoWH
          )
        )
      }
      mbPlayPause.setOnClickListener { mMediaPlayer.apply { if (isPlaying) pause() else start() } }
    }
  }

  private fun seekMediaPlayer(sliderValue: Float) =
    mMediaPlayer.seekTo((sliderValue * 100).roundToLong(), MediaPlayer.SEEK_CLOSEST)


  private fun updateGifDuration() {
    mtvGifDurationS.text =
      getString(
        R.string.gif_duration_s,
        String.format(
          "%.1f",
          with(rangeSlider.values) { ((this[1] - this[0]) * 100f).toInt() } / 1000f / playbackSpeed))
  }

  private fun loadVideoFailed() {
    runOnUiThread {
      Toolbox.toast("无法读取视频")
      finish()
    }
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
    mbPlaybackSpeed.text = text
    playbackSpeed = speed
    mMediaPlayer.apply {
      playbackParams = playbackParams.setSpeed(playbackSpeed)
      logRed("playbackSpeed", playbackSpeed)
    }
    updateGifDuration()
  }

  fun setCropRatio(ratio: Pair<Int, Int>?) {
    cropImageView.apply {
      clearAspectRatio()
      cropRect = wholeImageRect
      ratio?.let { setAspectRatio(it.first, ratio.second) }
    }
    mbCropRatio.text = BottomSheetVideoToGif2CropRatio.cropRatioToText(ratio)
  }

  companion object {
    fun start(context: Context, videoUri: Uri) =
      context.startActivity(
        Intent(context, VideoToGifActivity::class.java)
          .putExtra(EXTRA_VIDEO_URI, videoUri)
      )

    fun intentAddTextResult(textRender: TextRender) =
      Intent().putExtra(EXTRA_ADD_TEXT_RENDER, textRender)

    /** Example: 123456L -> "2:03.4" */
    private fun msToMinSecDs(ms: Int) =
      with(ms / 1000f) {
        "${(this / 60).toInt()}:${
          String.format(
            "%02d",
            (this % 60).toInt()
          )
        }.${((this - floor(this)) * 10).toInt()}"
      }
  }
}