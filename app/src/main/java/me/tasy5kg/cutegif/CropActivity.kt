package me.tasy5kg.cutegif

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.media.MediaPlayer.SEEK_CLOSEST
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Pair
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ContextThemeWrapper
import com.canhub.cropper.CropImageView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.slider.LabelFormatter
import com.google.android.material.slider.RangeSlider
import me.tasy5kg.cutegif.MyConstants.EXTRA_CROP_PARAMS
import me.tasy5kg.cutegif.MyConstants.EXTRA_CROP_PARAMS_DEFAULT
import me.tasy5kg.cutegif.MyConstants.EXTRA_TRIM_END
import me.tasy5kg.cutegif.MyConstants.EXTRA_TRIM_START
import me.tasy5kg.cutegif.MyConstants.EXTRA_VIDEO_URI
import me.tasy5kg.cutegif.MyConstants.FIRST_FRAME_PATH
import me.tasy5kg.cutegif.MyConstants.TRANSPARENT_COLOR
import me.tasy5kg.cutegif.MyConstants.UNKNOWN_INT
import me.tasy5kg.cutegif.databinding.ActivityCropBinding
import kotlin.math.*

class CropActivity : AppCompatActivity() {
  private lateinit var binding: ActivityCropBinding
  private lateinit var chipGroupCropRatio: ChipGroup
  private lateinit var cropImageView: CropImageView
  private lateinit var videoUri: Uri
  private lateinit var rangeSlider: RangeSlider // value 1.0f == 100ms
  private lateinit var videoView: VideoView
  private lateinit var mMediaPlayer: MediaPlayer
  private lateinit var materialToolbar: MaterialToolbar
  private lateinit var myCropParams: MyCropParams
  private lateinit var defaultCropParams: MyCropParams
  private var loopRunnable: Runnable? = null
  private var videoLastPosition = 0
  private var trimTimeStart = UNKNOWN_INT // 1 == 1ms
  private var trimTimeEnd = UNKNOWN_INT // 1 == 1ms
  private val timeLabelFormatter = LabelFormatter {
    val timeInSecond = it / 10f
    return@LabelFormatter "${(timeInSecond / 60).toInt()}:${String.format("%02d", (timeInSecond % 60).toInt())}.${((timeInSecond - floor(timeInSecond)) * 10).toInt()}"
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = ActivityCropBinding.inflate(layoutInflater)
    setContentView(binding.root)
    materialToolbar = binding.materialToolbar
    setSupportActionBar(materialToolbar)
    setFinishOnTouchOutside(false)
    chipGroupCropRatio = binding.chipGroupCropRatio
    cropImageView = binding.cropImageView
    rangeSlider = binding.rangeSlider
    with(intent!!.extras!!) {
      myCropParams = get(EXTRA_CROP_PARAMS) as MyCropParams
      defaultCropParams = get(EXTRA_CROP_PARAMS_DEFAULT) as MyCropParams
      videoUri = get(EXTRA_VIDEO_URI) as Uri
      trimTimeStart = get(EXTRA_TRIM_START) as Int
      trimTimeEnd = get(EXTRA_TRIM_END) as Int
    }
    loadCropRatioOptions(null)
    cropImageView.apply {
      val bitmapOptions = BitmapFactory.Options()
      bitmapOptions.inJustDecodeBounds = true
      BitmapFactory.decodeFile(FIRST_FRAME_PATH, bitmapOptions)
      val imageWidth = bitmapOptions.outWidth
      val imageHeight = bitmapOptions.outHeight
      val bitmap = Bitmap.createBitmap(imageWidth, imageHeight, Bitmap.Config.ALPHA_8)
      setBackgroundColor(TRANSPARENT_COLOR)
      bitmap.eraseColor(TRANSPARENT_COLOR)
      setImageBitmap(bitmap)
      isAutoZoomEnabled = false
      guidelines = CropImageView.Guidelines.ON_TOUCH
      isShowProgressBar = false
      cropRect = myCropParams.toRect()
    }
    binding.mbDone.setOnClickListener {
      setResult(RESULT_OK,
        Intent().putExtra(EXTRA_CROP_PARAMS, MyCropParams(cropImageView.cropRect!!))
          .putExtra(EXTRA_TRIM_START, trimTimeStart)
          .putExtra(EXTRA_TRIM_END, trimTimeEnd)
      )
      finish()
    }
    binding.mbCancel.setOnClickListener { finish() }
    videoView = binding.videoView.apply {
      setOnErrorListener { mp, what, extra ->
        Toast.makeText(this@CropActivity, context.getString(R.string.your_system_cannot_crop_this_video_but), Toast.LENGTH_LONG).show()
        finish()
        stopPlayback()
        return@setOnErrorListener true
      }
      setVideoURI(videoUri)
      setOnPreparedListener { mediaPlayer ->
        mMediaPlayer = mediaPlayer
        with(mMediaPlayer) {
          setVolume(0f, 0f)
          setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT)
          isLooping = true
        }
        with(rangeSlider) {
          valueFrom = 0f
          valueTo = mMediaPlayer.duration / 100f
          values = listOf(trimTimeStart / 100f, min(trimTimeEnd / 100f, valueTo))
          setLabelFormatter(timeLabelFormatter)
          seekMediaPlayer(values[0])
          materialToolbar.subtitle = getString(R.string.crop_X_s_taken, String.format("%.1f", (values[1] - values[0]) / 10.0))
          addOnSliderTouchListener(object : RangeSlider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: RangeSlider) {
              binding.videoView.pause()
            }

            override fun onStopTrackingTouch(slider: RangeSlider) {
              binding.videoView.start()
            }
          })
          addOnChangeListener { slider, value, _ ->
            materialToolbar.subtitle = getString(R.string.crop_X_s_taken, String.format("%.1f", (values[1] - values[0]) / 10f))
            trimTimeStart = (values[0] * 100).roundToInt()
            trimTimeEnd = (values[1] * 100).roundToInt()
            seekMediaPlayer(value)
            // slider.setLabelFormatter TODO
          }

        }
        loopRunnable = object : Runnable {
          override fun run() {
            try {
              val currentPosition = videoView.currentPosition / 100
              if ((currentPosition < rangeSlider.values[0] && currentPosition < videoLastPosition)
                || (currentPosition > rangeSlider.values[1])
              ) {
                seekMediaPlayer(rangeSlider.values[0])
              }
              videoLastPosition = currentPosition / 100
            } catch (e: Exception) {
              e.printStackTrace()
            }
            postDelayed(this, 100)
          }
        }
        this.postDelayed(loopRunnable!!, 100)
      }
      start()
    }
  }

  private fun seekMediaPlayer(sliderValue: Float) =
    when {
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> mMediaPlayer.seekTo((sliderValue * 100).roundToLong(), SEEK_CLOSEST)
      else -> mMediaPlayer.seekTo((sliderValue * 100).roundToInt())
    }

  override fun onPause() {
    super.onPause()
    binding.videoView.pause()
    binding.videoView.removeCallbacks(loopRunnable)
  }

  override fun onResume() {
    super.onResume()
    binding.videoView.start()
  }

  private fun applyCropRatio(cropRatioText: String) {
    with(cropImageView) {
      when (cropRatioText) {
        getString(R.string.crop_free) -> clearAspectRatio()
        getString(R.string.crop_square) -> setAspectRatio(1, 1)
        getString(R.string.crop_4_3) -> when (aspectRatio) {
          Pair(4, 3) -> setAspectRatio(3, 4)
          else -> setAspectRatio(4, 3)
        }
        getString(R.string.crop_16_9) -> when (aspectRatio) {
          Pair(16, 9) -> setAspectRatio(9, 16)
          else -> setAspectRatio(16, 9)
        }
      }
    }
  }

  private fun loadCropRatioOptions(checkedCropRatioText: String?) {
    val cropRatioTextList = listOf(
      getString(R.string.crop_free),
      getString(R.string.crop_square),
      getString(R.string.crop_4_3),
      getString(R.string.crop_16_9)
    )
    cropRatioTextList.forEach {
      chipGroupCropRatio.addView(Chip(ContextThemeWrapper(this, com.google.android.material.R.style.Widget_Material3_Chip_Filter)).apply {
        text = it
        chipBackgroundColor = MyToolbox.createColorStateList(
          arrayOf(
            android.R.attr.state_checked to R.color.green_light,
            android.R.attr.state_checkable to R.color.light
          )
        )
        isCheckable = true
        isCheckedIconVisible = false
        val defaultCheckedCropRatioText = checkedCropRatioText ?: getString(R.string.crop_free)
        applyCropRatio(defaultCheckedCropRatioText)
        isChecked = (it == defaultCheckedCropRatioText)
        setOnClickListener { view ->
          val clickedChip = view as Chip
          clickedChip.isChecked = true
          cropImageView.cropRect = defaultCropParams.toRect()
          applyCropRatio(clickedChip.text.toString())
        }
      }
      )
    }
  }

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    menuInflater.inflate(R.menu.toolbar_close, menu)
    return true
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    when (item.itemId) {
      R.id.menu_item_close -> finish()
    }
    return true
  }

  companion object {
    fun startIntent(
      context: Context,
      myCropParams: MyCropParams,
      defaultCropParams: MyCropParams,
      inputVideoUri: Uri,
      trimTimeStart: Int,
      trimTimeEnd: Int,
    ) =
      Intent(context, CropActivity::class.java)
        .putExtra(EXTRA_CROP_PARAMS, myCropParams)
        .putExtra(EXTRA_CROP_PARAMS_DEFAULT, defaultCropParams)
        .putExtra(EXTRA_VIDEO_URI, inputVideoUri)
        .putExtra(EXTRA_TRIM_START, trimTimeStart)
        .putExtra(EXTRA_TRIM_END, trimTimeEnd)
  }
}
