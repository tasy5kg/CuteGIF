package me.tasy5kg.cutegif

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.*
import android.view.*
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.widget.doAfterTextChanged
import com.arthenica.ffmpegkit.*
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import me.tasy5kg.cutegif.MyConstants.DOUBLE_BACK_TO_EXIT_DELAY
import me.tasy5kg.cutegif.MyConstants.EXTRA_CROP_PARAMS
import me.tasy5kg.cutegif.MyConstants.FFMPEG_COMMAND_FOR_ALL
import me.tasy5kg.cutegif.MyConstants.FIRST_FRAME_PATH
import me.tasy5kg.cutegif.MyConstants.GIF_COLOR_QUALITY_MAP
import me.tasy5kg.cutegif.MyConstants.GIF_FRAME_RATE_MAP
import me.tasy5kg.cutegif.MyConstants.GIF_RESOLUTION_MAP
import me.tasy5kg.cutegif.MyConstants.GIF_SPEED_GLANCE_MODE
import me.tasy5kg.cutegif.MyConstants.GIF_SPEED_MAP
import me.tasy5kg.cutegif.MyConstants.LAST_SELECTED_OPTION_LOADED_DISPLAY_DURATION
import me.tasy5kg.cutegif.MyConstants.PALETTE_PATH
import me.tasy5kg.cutegif.MyConstants.REMEMBER_GIF_CONFIG_DEFAULT
import me.tasy5kg.cutegif.MyConstants.REMEMBER_GIF_CONFIG_OFF
import me.tasy5kg.cutegif.MyConstants.REMEMBER_GIF_CONFIG_ON
import me.tasy5kg.cutegif.MyConstants.THUMBNAIL_PATH
import me.tasy5kg.cutegif.MySettings.INT_PREVIOUS_GIF_CONFIG_COLOR_QUALITY
import me.tasy5kg.cutegif.MySettings.INT_PREVIOUS_GIF_CONFIG_FRAME_RATE
import me.tasy5kg.cutegif.MySettings.INT_PREVIOUS_GIF_CONFIG_RESOLUTION
import me.tasy5kg.cutegif.MySettings.INT_PREVIOUS_GIF_CONFIG_SPEED
import me.tasy5kg.cutegif.MySettings.INT_REMEMBER_GIF_CONFIG
import me.tasy5kg.cutegif.databinding.ActivityGifBinding
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.*

@SuppressLint("InflateParams")
class GifActivity : AppCompatActivity() {
  private var gifViewRatio = 0.0
  private var converting = false
  private var doubleBackToExitPressedOnce = false
  private lateinit var binding: ActivityGifBinding
  private lateinit var inputVideoUri: Uri
  private lateinit var mbConvert: MaterialButton
  private lateinit var linearProgressIndicator: LinearProgressIndicator
  private lateinit var videoInformationSession: MediaInformationSession
  private lateinit var cmivResolution: CustomMenuItemView
  private lateinit var cmivColorQuality: CustomMenuItemView
  private lateinit var cmivFrameRate: CustomMenuItemView
  private lateinit var cmivSpeed: CustomMenuItemView
  private lateinit var gifUri: Uri
  private lateinit var materialToolbar: MaterialToolbar
  private lateinit var mbClose: MaterialButton
  private lateinit var viewMaskLayer: View
  private lateinit var myCropParams: MyCropParams
  private lateinit var materialToolbarSubtitle: AppCompatTextView
  private var command1VfCached = ""

  private val getCropResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
    if (it.resultCode == RESULT_OK) {
      myCropParams = it.data!!.extras!!.get(EXTRA_CROP_PARAMS) as MyCropParams
      binding.chipCrop.text = getString(R.string.re_crop)
      loadCroppedThumbnailAndSetupCmivResolution()
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = ActivityGifBinding.inflate(layoutInflater)
    setContentView(binding.root)
    setFinishOnTouchOutside(false)
    materialToolbar = binding.materialToolbar
    setSupportActionBar(materialToolbar)
    materialToolbarSubtitle = (materialToolbar.getChildAt(1) as AppCompatTextView).apply {
      doAfterTextChanged {
        visibility = when (text.isNullOrBlank()) {
          true -> GONE
          false -> VISIBLE
        }
      }
    }
    binding.llcMoreOptionsButton.setOnClickListener {
      it.visibility = GONE
      binding.llcMoreOptionsGroup.visibility = VISIBLE
    }
    linearProgressIndicator = binding.linearProgressIndicator
    inputVideoUri = intent.getParcelableExtra(EXTRA_VIDEO_URI) ?: intent.getParcelableExtra(Intent.EXTRA_STREAM) ?: intent.data ?: Uri.EMPTY
    if (inputVideoUri == Uri.EMPTY) {
      loadVideoFailed()
      return
    }
    videoInformationSession = FFprobeKit.getMediaInformation(inputVideoSaf())
    viewMaskLayer = binding.viewMaskLayer
    mbConvert = binding.mbConvert.apply { setOnClickListener { onConvertClick() } }
    mbClose = binding.mbCancel.apply {
      setOnClickListener {
        conversionUnsuccessfully(canceledByUser = true, finishActivity = true)
      }
    }
    if (MySettings.firstStartCurrentVersion()) {
      AboutActivity.start(this@GifActivity)
    }
    loadCustomMenuItemView()
    setDefaultCropParams()
    loadFirstFrame()
    binding.chipCrop.setOnClickListener {
      getCropResult.launch(Intent(this@GifActivity, CropActivity::class.java).putExtra(EXTRA_CROP_PARAMS, myCropParams))
    }
  }

  private fun setDefaultCropParams() {
    val videoStreamInfo = videoInformationSession.mediaInformation.streams.first { it.type == "video" }
    var rotation =
      try {
        -((videoStreamInfo.allProperties.get("side_data_list") as JSONArray).get(0) as JSONObject).getInt("rotation")
      } catch (e: Exception) {
        0
      }
    if (rotation % 90 != 0) {
      MyToolbox.logging("rotation = $rotation", "rotation%90 != 0")
      rotation = 0
    }
    while (rotation < 0) {
      rotation += 360
    }
    while (rotation > 360) {
      rotation -= 360
    }
    myCropParams = MyCropParams(videoStreamInfo.width.toInt(), videoStreamInfo.height.toInt(), 0, 0, rotation)
  }

  private fun loadCustomMenuItemView() {
    cmivSpeed = binding.cmivSpeed
    cmivResolution = binding.cmivResolution
    cmivFrameRate = binding.cmivFrameRate
    cmivColorQuality = binding.cmivColorQuality
    cmivSpeed.setUpWithDropDownConfig(GIF_SPEED_MAP, true)
    cmivResolution.setUpWithDropDownConfig(GIF_RESOLUTION_MAP)
    cmivFrameRate.setUpWithDropDownConfig(GIF_FRAME_RATE_MAP)
    cmivColorQuality.setUpWithDropDownConfig(GIF_COLOR_QUALITY_MAP)
    loadPreviousGifConfig()
  }

  private fun loadPreviousGifConfig() {
    when (MySettings.getInt(INT_REMEMBER_GIF_CONFIG, REMEMBER_GIF_CONFIG_DEFAULT)) {
      REMEMBER_GIF_CONFIG_ON -> {
        val notFoundValue = -1
        val previousGifConfigSpeed = MySettings.getInt(INT_PREVIOUS_GIF_CONFIG_SPEED, notFoundValue)
        val previousGifConfigResolution = MySettings.getInt(INT_PREVIOUS_GIF_CONFIG_RESOLUTION, notFoundValue)
        val previousGifConfigFrameRate = MySettings.getInt(INT_PREVIOUS_GIF_CONFIG_FRAME_RATE, notFoundValue)
        val previousGifConfigColorQuality = MySettings.getInt(INT_PREVIOUS_GIF_CONFIG_COLOR_QUALITY, notFoundValue)
        if (previousGifConfigSpeed != notFoundValue &&
          previousGifConfigResolution != notFoundValue &&
          previousGifConfigFrameRate != notFoundValue &&
          previousGifConfigColorQuality != notFoundValue
        ) {
          cmivSpeed.setSelectedValue(previousGifConfigSpeed)
          cmivResolution.setSelectedValue(previousGifConfigResolution)
          cmivFrameRate.setSelectedValue(previousGifConfigFrameRate)
          cmivColorQuality.setSelectedValue(previousGifConfigColorQuality)
          materialToolbarSubtitle.text = getString(R.string.last_selected_option_loaded)
          Handler(Looper.getMainLooper()).postDelayed({
            if (materialToolbarSubtitle.text.toString() == getString(R.string.last_selected_option_loaded)) {
              materialToolbarSubtitle.text = ""
            }
          }, LAST_SELECTED_OPTION_LOADED_DISPLAY_DURATION)
        } else {
          materialToolbarSubtitle.text = ""
        }
      }
      REMEMBER_GIF_CONFIG_OFF -> materialToolbarSubtitle.text = ""
    }
  }

  private fun saveCurrentGifConfig() {
    when (MySettings.getInt(INT_REMEMBER_GIF_CONFIG, REMEMBER_GIF_CONFIG_DEFAULT)) {
      REMEMBER_GIF_CONFIG_ON -> {
        MySettings.setInt(INT_PREVIOUS_GIF_CONFIG_SPEED, cmivSpeed.selectedValue())
        MySettings.setInt(INT_PREVIOUS_GIF_CONFIG_RESOLUTION, cmivResolution.selectedValue())
        MySettings.setInt(INT_PREVIOUS_GIF_CONFIG_FRAME_RATE, cmivFrameRate.selectedValue())
        MySettings.setInt(INT_PREVIOUS_GIF_CONFIG_COLOR_QUALITY, cmivColorQuality.selectedValue())
      }
      REMEMBER_GIF_CONFIG_OFF -> {} // do nothing
    }
  }

  private fun loadFirstFrame() {
    try {
      // FFmpegKit.executeAsync("$FFMPEG_COMMAND_FOR_ALL -i ${inputVideoSaf()} -vframes 1 -vf scale=${resolutionPara(1080)}:flags=lanczos -q:v 5 -y $FIRST_FRAME_PATH") {
      FFmpegKit.executeAsync("$FFMPEG_COMMAND_FOR_ALL -i ${inputVideoSaf()} -vframes 1 -q:v 10 -y $FIRST_FRAME_PATH") {
        when (it.returnCode.isValueSuccess) {
          true -> loadCroppedThumbnailAndSetupCmivResolution()
          false -> loadVideoFailed()
        }
      }
    } catch (e: Exception) {
      loadVideoFailed()
    }
  }

  private fun loadCroppedThumbnailAndSetupCmivResolution() {
    /*
    limit width/height: export<=import

    val croppedShortLength = max(min(cropParams.outW, cropParams.outH), GIF_RESOLUTION_MAP.values.min())
    cmivResolution.setUpWithDropDownConfig(GIF_RESOLUTION_MAP.filter { it.value <= croppedShortLength } as LinkedHashMap<String, Int>,
        R.string.resolution_guide)
           */
    try {
      FFmpegKit.executeAsync(
        "$FFMPEG_COMMAND_FOR_ALL -i $FIRST_FRAME_PATH -vf ${cropParams()}${transposeParams()} -q:v 10 -y $THUMBNAIL_PATH"
        // "$FFMPEG_COMMAND_FOR_ALL -i $FIRST_FRAME_PATH -vf ${cropParams()}${transposeParams()},scale=${resolutionParams(240)}:flags=lanczos -y $THUMBNAIL_PATH"
      ) {
        when (it.returnCode.isValueSuccess) {
          true -> {
            runOnUiThread {
              Glide.with(this@GifActivity)
                .load(THUMBNAIL_PATH)
                .fitCenter()
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .transition(withCrossFade())
                .into(binding.aciv)
            }
          }
          false -> loadVideoFailed()
        }
      }
    } catch (e: Exception) {
      loadVideoFailed()
    }
  }

  private fun loadVideoFailed() {
    runOnUiThread {
      Toast.makeText(this@GifActivity, getString(R.string.load_video_failed), Toast.LENGTH_LONG).show()
      finishAndRemoveTask()
      keepScreenOn(false)
    }
  }

  private fun updateConvertingState() {
    runOnUiThread {
      converting = !converting
      when (converting) {
        true -> {
          keepScreenOn(true)
          viewMaskLayer.visibility = VISIBLE
          linearProgressIndicator.apply {
            visibility = VISIBLE
            progress = 0
            isIndeterminate = true
          }
          materialToolbar.subtitle = getString(R.string.analyzing_video)
          mbClose.visibility = GONE
          mbConvert.apply {
            text = getString(R.string.cancel)
            icon = AppCompatResources.getDrawable(this@GifActivity, R.drawable.ic_baseline_close_24)
          }
        }
        false -> {
          conversionUnsuccessfully(canceledByUser = true, finishActivity = false)
          keepScreenOn(false)
          viewMaskLayer.visibility = GONE
          linearProgressIndicator.apply {
            progress = 0
            isIndeterminate = true
            visibility = GONE
          }
          materialToolbar.subtitle = getString(R.string.conversion_canceled)
          mbClose.visibility = VISIBLE
          mbConvert.apply {
            text = getString(R.string.convert_to_gif)
            icon = AppCompatResources.getDrawable(this@GifActivity, R.drawable.ic_baseline_video_library_24)
          }
        }
      }
    }

  }

  private fun conversionSuccessfully() {
    converting = false
    runOnUiThread {
      binding.chipCrop.visibility = GONE
      linearProgressIndicator.visibility = GONE
      binding.frameLayoutGoneWhenFinished.visibility = GONE
      viewMaskLayer.visibility = GONE
      materialToolbar.subtitle = getString(R.string.gif_saved_s_mb, MyToolbox.keepNDecimalPlaces(MyToolbox.getFileSizeFromUri(gifUri) / 1048576.0, 2))
      Glide.with(this@GifActivity)
        .load(gifUri)
        .fitCenter()
        .diskCacheStrategy(DiskCacheStrategy.NONE)
        .skipMemoryCache(true)
        .into(binding.aciv.apply {
          this.layoutParams.height =
            min((this@GifActivity.resources.displayMetrics.heightPixels * 0.5).toInt(), (this@apply.width * gifViewRatio).toInt())
          requestLayout()
        })
      mbConvert.apply {
        layoutParams.width = ViewGroup.LayoutParams.WRAP_CONTENT
        text = getString(R.string.done)
        icon = AppCompatResources.getDrawable(this@GifActivity, R.drawable.ic_baseline_done_24)
        setOnClickListener {
          finishAndRemoveTask()
        }
      }
      mbClose.apply {
        visibility = VISIBLE
        text = getString(R.string.share)
        setOnClickListener {
          startActivity(Intent.createChooser(Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, gifUri)
            type = "image/gif"
          }, "Share GIF to"))
        }
      }

      binding.space.visibility = VISIBLE
      binding.mbDelete.apply {
        visibility = VISIBLE
        setOnClickListener {
          finishAndRemoveTask()
          try {
            contentResolver.delete(gifUri, null, null)
            MyToolbox.logging("gifUri", "deleted")
          } catch (e: Exception) {
          }
          Toast.makeText(this@GifActivity, getString(R.string.gif_deleted), Toast.LENGTH_LONG).show()
        }
      }

    }
  }

  private val videoIFramesCount by lazy {
    FFprobeKit.execute("-hide_banner -loglevel error -skip_frame nokey -select_streams v:0 -count_frames -show_entries stream=nb_read_frames -of csv=p=0:sv=fail ${inputVideoSaf()}").logsAsString.split(",").first().toInt()
  }  // takes ~5 seconds

  private fun onConvertClick() {
    updateConvertingState()
    if (converting) {
      saveCurrentGifConfig()
      gifUri = MyToolbox.createNewGifFileAndReturnUri(MyToolbox.getFileNameFromUri(inputVideoUri, true))
      // Note: set framestep for command1 will not increase speed
      val command1Vf = "${cropParams()}${transposeParams()},scale=${resolutionParams(GIF_RESOLUTION_MAP.values.min())}:flags=fast_bilinear,palettegen=max_colors=${cmivColorQuality.selectedValue()}:stats_mode=diff"
      val command1 = if (command1Vf == command1VfCached) {
        "-version" // do nothing
      } else {
        "$FFMPEG_COMMAND_FOR_ALL -skip_frame nokey -i ${inputVideoSaf()} -vf $command1Vf -y $PALETTE_PATH"
      }
      MyToolbox.logging("command1", command1)
      FFmpegKit.executeAsync(command1) {
        when {
          it.returnCode.isValueSuccess -> {
            command1VfCached = command1Vf
            val outputFpsTarget = cmivFrameRate.selectedValue()
            val outputFpsReal: Double
            val outputSpeed: Double
            val frameStep: Int
            val outputFramesEstimated: Int
            val command2: String
            var progress: Int
            when (cmivSpeed.selectedValue()) {
              GIF_SPEED_GLANCE_MODE -> {
                val command2FrameStep = 30 / outputFpsTarget
                outputFramesEstimated = videoIFramesCount / command2FrameStep
                command2 = "$FFMPEG_COMMAND_FOR_ALL -skip_frame nokey -r 30 -i ${inputVideoSaf()} -i $PALETTE_PATH -lavfi \"framestep=$command2FrameStep,${cropParams()}${transposeParams()},scale=${resolutionParams()}:flags=lanczos [x]; [x][1:v] paletteuse=dither=bayer\" -y ${outputGifSaf()}"
              }
              else -> {
                outputSpeed = cmivSpeed.selectedValue() / 100.0
                frameStep = max(round(inputVideoFps() * outputSpeed / outputFpsTarget).toInt(), 1)
                outputFpsReal = inputVideoFps() * outputSpeed / frameStep
                outputFramesEstimated = (inputVideoDuration() * inputVideoFps() / frameStep).toInt()
                command2 = "$FFMPEG_COMMAND_FOR_ALL -i ${inputVideoSaf()} -i $PALETTE_PATH -r $outputFpsReal -lavfi \"framestep=$frameStep,setpts=PTS/$outputSpeed,${cropParams()}${transposeParams()},scale=${resolutionParams()}:flags=lanczos [x]; [x][1:v] paletteuse=dither=bayer\" -y ${outputGifSaf()}"
              }
            }
            MyToolbox.logging("command2", command2)
            MyToolbox.logging("outputFramesEstimated", outputFramesEstimated.toString())
            runOnUiThread { linearProgressIndicator.isIndeterminate = false }
            FFmpegKit.executeAsync(command2, { ffmpegSession ->
              runOnUiThread { keepScreenOn(false) }
              when {
                ffmpegSession.returnCode.isValueSuccess -> conversionSuccessfully()
                ffmpegSession.returnCode.isValueError -> conversionUnsuccessfully(canceledByUser = false, finishActivity = true)
              }
            }, { log -> MyToolbox.logging("logcallback", log.message.toString()) }, {
              runOnUiThread {
                progress = min((it.videoFrameNumber * 100 / outputFramesEstimated), 99)
                linearProgressIndicator.setProgress(progress, true)
                materialToolbar.subtitle = getString(R.string.converting_s_s_mb, progress, MyToolbox.keepNDecimalPlaces(it.size / 1048576.0, 2))
              }
            })
          }
          it.returnCode.isValueError -> conversionUnsuccessfully(canceledByUser = false, finishActivity = true)
        }
      }
    }
  }

  private fun inputVideoDuration() = videoInformationSession.mediaInformation.duration.toDouble()

  private fun inputVideoSaf() = FFmpegKitConfig.getSafParameterForRead(this, inputVideoUri)

  private fun outputGifSaf() = FFmpegKitConfig.getSafParameterForWrite(this, gifUri)

  private fun conversionUnsuccessfully(canceledByUser: Boolean, finishActivity: Boolean) {
    runOnUiThread {
      keepScreenOn(false)
      if (!canceledByUser) {
        Toast.makeText(this@GifActivity, getString(R.string.conversion_failed), Toast.LENGTH_LONG).show()
      }
      FFmpegKitConfig.clearSessions()
      FFmpegKit.cancel()
      if (::gifUri.isInitialized) {
        try {
          contentResolver.delete(gifUri, null, null)
          MyToolbox.logging("gifUri", "deleted")
        } catch (e: Exception) {
        }
      }
      if (finishActivity) {
        finishAndRemoveTask()
      }
    }
  }

  private fun inputVideoFps(): Double {
    val fpsFraction = videoInformationSession.mediaInformation.streams.first { it.type == "video" }.averageFrameRate
    val numerator = fpsFraction.split("/").toTypedArray()[0].toInt()
    val denominator = fpsFraction.split("/").toTypedArray()[1].toInt()
    return numerator.toDouble() / denominator
  }

  private fun resolutionParams(): String = resolutionParams(null)

  private fun resolutionParams(shortLength: Int?): String {
    val long = max(myCropParams.outW, myCropParams.outH)
    val short = min(myCropParams.outW, myCropParams.outH)
    val pixel = min(shortLength ?: cmivResolution.selectedValue(), short)
    return if ((myCropParams.outW > myCropParams.outH) == (myCropParams.rotatedDegrees % 180 == 0)) {
      gifViewRatio = short.toDouble() / long
      "-2:$pixel"
    } else {
      gifViewRatio = long.toDouble() / short
      "$pixel:-2"
    }
  }

  private fun cropParams() = "crop=${myCropParams.outW}:${myCropParams.outH}:${myCropParams.x}:${myCropParams.y}"

  private fun transposeParams() =
    when (myCropParams.rotatedDegrees) {
      0 -> ""
      90 -> ",transpose=1"
      180 -> ",transpose=1,transpose=1"
      270 -> ",transpose=2"
      else -> throw IllegalArgumentException("cropParams.rotatedDegrees = ${myCropParams.rotatedDegrees}")
    }

  private fun keepScreenOn(boolean: Boolean) {
    when (boolean) {
      true -> window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
      false -> window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
  }

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    menuInflater.inflate(R.menu.toolbar_close, menu)
    return true
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    when (item.itemId) {
      R.id.menu_item_close -> {
        conversionUnsuccessfully(canceledByUser = true, finishActivity = true)
      }
    }
    return true
  }

  override fun onBackPressed() {
    if (!(converting && !doubleBackToExitPressedOnce)) {
      conversionUnsuccessfully(canceledByUser = true, finishActivity = true)
      super.onBackPressed()
      return
    }
    doubleBackToExitPressedOnce = true
    Toast.makeText(this@GifActivity, getString(R.string.press_back_again_to_quit), Toast.LENGTH_SHORT).show()
    Handler(Looper.getMainLooper()).postDelayed({ doubleBackToExitPressedOnce = false }, DOUBLE_BACK_TO_EXIT_DELAY)
  }

  override fun onDestroy() {
    super.onDestroy()
    keepScreenOn(false)
    FFmpegKitConfig.clearSessions()
    FFmpegKit.cancel()
  }

  companion object {
    const val EXTRA_VIDEO_URI = "EXTRA_VIDEO_URI"
    fun start(context: Context, videoUri: Uri) {
      context.startActivity(Intent(context, GifActivity::class.java).putExtra(EXTRA_VIDEO_URI, videoUri))
    }
  }
}