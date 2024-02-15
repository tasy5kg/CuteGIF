package me.tasy5kg.cutegif

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import me.tasy5kg.cutegif.MyConstants.EXTRA_ADD_TEXT_RENDER
import me.tasy5kg.cutegif.MyConstants.EXTRA_VIDEO_PATH
import me.tasy5kg.cutegif.databinding.ActivityVideoToGifBinding
import me.tasy5kg.cutegif.toolbox.MediaTools.getVideoDurationByAndroidSystem
import me.tasy5kg.cutegif.toolbox.Toolbox.boundRange
import me.tasy5kg.cutegif.toolbox.Toolbox.constraintBy
import me.tasy5kg.cutegif.toolbox.Toolbox.getExtra
import me.tasy5kg.cutegif.toolbox.Toolbox.msToMinSecDs
import me.tasy5kg.cutegif.toolbox.Toolbox.newRunnableWithSelf
import me.tasy5kg.cutegif.toolbox.Toolbox.onClick
import me.tasy5kg.cutegif.toolbox.Toolbox.onSliderTouch
import me.tasy5kg.cutegif.toolbox.Toolbox.sceneTransitionAnimationOptionBuilder
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToLong


class VideoToGifActivity : BaseActivity() {
  private val binding by lazy { ActivityVideoToGifBinding.inflate(layoutInflater) }
  private val bottomSheetVideoToGif2PlaybackSpeed by lazy { BottomSheetVideoToGifPlaybackSpeed() }
  private val videoToGifExportOptionsDialogFragment by lazy { VideoToGifExportOptionsDialogFragment() }
  private val bottomSheetVideoToGifCropRatio by lazy { BottomSheetVideoToGifCropRatio() }
  private val videoDuration by lazy { getVideoDurationByAndroidSystem(inputVideoPath) }

  // 为 VideoToGifExportOptionsDialogFragment 保存色度抠图所选的颜色
  var savedColorKeyColor: Int? = null

  val cropParams get() = CropParams(binding.cropImageView.cropRect!!)
  val inputVideoPath by lazy { intent.getExtra<String>(EXTRA_VIDEO_PATH) }

  val videoView by lazy { binding.videoView }
  val rangeSlider by lazy { binding.rangeSlider } // value 1.0f == 100ms

  private var videoLastPosition = 0L // 1 == 1ms
  var playbackSpeed = 1f
  var textRender: TextRender? = null

  lateinit var videoWH: Pair<Int, Int>
  private lateinit var mMediaPlayer: MediaPlayer

  private val loopRunnable by lazy {
    newRunnableWithSelf { self ->
      try {
        val currentPosition = videoView.currentPosition / 100
        binding.sliderCursor.value = (videoView.currentPosition / 100f).constraintBy(0f..binding.sliderCursor.valueTo)
        if ((currentPosition < rangeSlider.values[0] && currentPosition < videoLastPosition) || (currentPosition > rangeSlider.values[1])) {
          seekMediaPlayer(rangeSlider.values[0])
        }
        videoLastPosition = currentPosition / 100L
        binding.mbTrimTimeCurrentPlaying.text = msToMinSecDs(videoView.currentPosition)
        binding.mbTrimTimeInPoint.text = msToMinSecDs((rangeSlider.values[0] * 100).toInt())
        binding.mbTrimTimeOutPoint.text = msToMinSecDs((rangeSlider.values[1] * 100).toInt())
        binding.mbTrimTimeCurrentPlaying.text = msToMinSecDs(videoView.currentPosition)
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
        binding.acivText.setImageBitmap(
          TextRender.render(
            textRender, videoWH.first, videoWH.second
          )
        )
      }
    }

  @SuppressLint("ClickableViewAccessibility")
  override fun onCreateIfEulaAccepted(savedInstanceState: Bundle?) {
    setContentView(binding.root)
    // CropImageView cannot automatically resize itself when the parent layout changes size. Help it resize here.
    binding.rl.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
      binding.cropImageView.layoutParams.width = binding.rl.width
      binding.cropImageView.layoutParams.height = binding.rl.height
    }
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
      supportFragmentManager.beginTransaction()
        .add(videoToGifExportOptionsDialogFragment, VideoToGifExportOptionsDialogFragment.TAG)
        .commit()
    }
    binding.mbTrimTimeMinus.updateTrimTimeOnTouch(-100)
    binding.mbTrimTimePlus.updateTrimTimeOnTouch(+100)
  }

  @SuppressLint("ClickableViewAccessibility")
  private fun Button.updateTrimTimeOnTouch(deltaMs: Int) {
    setOnTouchListener { _, event ->
      when (event.action) {
        MotionEvent.ACTION_DOWN -> updateTrimTime(deltaMs)
        MotionEvent.ACTION_UP -> stopUpdateTrimTime()
      }
      true
    }
  }

  private var sesUpdateTrimTime: ScheduledExecutorService? = null

  private fun updateTrimTime(deltaMs: Int) {
    sesUpdateTrimTime = Executors.newSingleThreadScheduledExecutor().apply {
      scheduleWithFixedDelay({
        val seekValue: Float
        when (binding.mbtgMbTrimTime.checkedButtonId) {
          binding.mbTrimTimeInPoint.id -> {
            seekValue = (rangeSlider.values[0] + deltaMs / 100f).constraintBy(rangeSlider.boundRange())
            rangeSlider.setValues(seekValue, rangeSlider.values[1])
          }

          binding.mbTrimTimeOutPoint.id -> {
            seekValue = (rangeSlider.values[1] + deltaMs / 100f).constraintBy(rangeSlider.boundRange())
            rangeSlider.setValues(rangeSlider.values[0], seekValue)
          }

          binding.mbTrimTimeCurrentPlaying.id -> {
            seekValue = (binding.sliderCursor.value + deltaMs / 100f).constraintBy(rangeSlider.boundRange())
            binding.sliderCursor.value = seekValue
          }

          else -> throw IllegalStateException()
        }
        mMediaPlayer.pause()
        seekMediaPlayer(seekValue)
      }, 0, 300, TimeUnit.MILLISECONDS)
    }
  }

  private fun stopUpdateTrimTime() {
    sesUpdateTrimTime?.shutdownNow()
    sesUpdateTrimTime = null
  }

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
        setImageBitmap(Bitmap.createBitmap(videoWH.first, videoWH.second, Bitmap.Config.ALPHA_8)
          .apply { eraseColor(Color.TRANSPARENT) })
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
        addOnChangeListener { slider, value, fromUser ->
          if (fromUser) {
            binding.mbtgMbTrimTime.check(
              if (value == values[0]) binding.mbTrimTimeInPoint.id else binding.mbTrimTimeOutPoint.id
            )
            seekMediaPlayer(value)
            if (max(
                abs(previousValuesHapticFeedbacked[0] - values[0]), abs(previousValuesHapticFeedbacked[1] - values[1])
              ) >= 1f
            ) {
              slider.performHapticFeedback(HapticFeedbackConstants.TEXT_HANDLE_MOVE)
              previousValuesHapticFeedbacked = values
            }
          }
          updateGifDuration()
        }
      }
      binding.sliderCursor.apply {
        valueFrom = 0f
        valueTo = videoView.duration / 100f
      }
      binding.mbPlaybackSpeed.setOnClickListener {
        bottomSheetVideoToGif2PlaybackSpeed.show(
          supportFragmentManager, BottomSheetVideoToGifPlaybackSpeed.TAG
        )
      }
      binding.mbCropRatio.setOnClickListener {
        bottomSheetVideoToGifCropRatio.show(
          supportFragmentManager, BottomSheetVideoToGifCropRatio.TAG
        )
      }
      binding.mbAddText.onClick {
        mMediaPlayer.pause()
        getAddTextResult.launch(
          AddTextActivity.startIntent(
            this@VideoToGifActivity, inputVideoPath, videoView.currentPosition.toLong(), textRender
          ), sceneTransitionAnimationOptionBuilder(this@VideoToGifActivity, binding.acivText, binding.videoView)
        )
      }
      binding.mbPlayPause.setOnClickListener { mMediaPlayer.apply { if (isPlaying) pause() else start() } }
    }
  }

  private fun seekMediaPlayer(sliderValue: Float) =
    mMediaPlayer.seekTo((sliderValue * 100f).roundToLong(), MediaPlayer.SEEK_CLOSEST)

  private fun updateGifDuration() {
    binding.mtvGifDurationS.text = getString(R.string.gif_duration_s,
      String.format("%.1f", with(rangeSlider.values) { ((this[1] - this[0]) * 100f).toInt() } / 1000f / playbackSpeed))
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
      mMediaPlayer.playbackParams = mMediaPlayer.playbackParams.setSpeed(playbackSpeed)
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
    binding.mbCropRatio.text = BottomSheetVideoToGifCropRatio.cropRatioToText(ratio)
  }

  companion object {
    fun start(context: Context, inputVideoPath: String) = context.startActivity(
      Intent(context, VideoToGifActivity::class.java).putExtra(
        EXTRA_VIDEO_PATH, inputVideoPath
      )
    )

    fun intentAddTextResult(textRender: TextRender) = Intent().putExtra(EXTRA_ADD_TEXT_RENDER, textRender)
  }
}