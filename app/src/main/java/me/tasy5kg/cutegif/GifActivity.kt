package me.tasy5kg.cutegif

import android.Manifest
import android.annotation.SuppressLint
import android.content.*
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.view.*
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import com.arthenica.ffmpegkit.*
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import me.tasy5kg.cutegif.MyConstants.CUSTOM_MENU_ITEM_VIEW_TYPE_GIF_CHIPS
import me.tasy5kg.cutegif.MyConstants.CUSTOM_MENU_ITEM_VIEW_TYPE_GIF_SPEED
import me.tasy5kg.cutegif.MyConstants.DOUBLE_BACK_TO_EXIT_DELAY
import me.tasy5kg.cutegif.MyConstants.EXTRA_CROP_PARAMS
import me.tasy5kg.cutegif.MyConstants.EXTRA_TRIM_END
import me.tasy5kg.cutegif.MyConstants.EXTRA_TRIM_START
import me.tasy5kg.cutegif.MyConstants.EXTRA_VIDEO_URI
import me.tasy5kg.cutegif.MyConstants.FFMPEG_COMMAND_FOR_ALL
import me.tasy5kg.cutegif.MyConstants.FIRST_FRAME_PATH
import me.tasy5kg.cutegif.MyConstants.GIF_COLOR_QUALITY_MAP
import me.tasy5kg.cutegif.MyConstants.GIF_FRAME_RATE_MAP
import me.tasy5kg.cutegif.MyConstants.GIF_RESOLUTION_MAP
import me.tasy5kg.cutegif.MyConstants.GIF_SPEED_GLANCE_MODE
import me.tasy5kg.cutegif.MyConstants.GIF_SPEED_MAP
import me.tasy5kg.cutegif.MyConstants.MATERIAL_TOOLBAR_SUBTITLE_TEMP_DISPLAY_DURATION
import me.tasy5kg.cutegif.MyConstants.PALETTE_PATH
import me.tasy5kg.cutegif.MyConstants.THUMBNAIL_PATH
import me.tasy5kg.cutegif.MyConstants.UNKNOWN_INT
import me.tasy5kg.cutegif.MySettings.INT_PREVIOUS_GIF_CONFIG_UNKNOWN_VALUE
import me.tasy5kg.cutegif.databinding.ActivityGifBinding
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import kotlin.concurrent.thread
import kotlin.math.*

@SuppressLint("InflateParams")
class GifActivity : AppCompatActivity() {
  private var command1VfCached = ""
  private var rotation = 0
  private var trimTimeStart = UNKNOWN_INT // 1 == 1ms
  private var trimTimeEnd = UNKNOWN_INT // 1 == 1ms
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
  private lateinit var mbCancelInGroupConvert: MaterialButton
  private lateinit var viewMaskLayer: View
  private lateinit var myCropParams: MyCropParams
  private lateinit var defaultCropParams: MyCropParams
  private lateinit var materialToolbarSubtitle: AppCompatTextView
  private var trimTimeCached = Pair(UNKNOWN_INT, UNKNOWN_INT)

  private val getCropAndTrimResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
    if (it.resultCode == RESULT_OK) {
      with(it.data!!.extras!!) {
        myCropParams = get(EXTRA_CROP_PARAMS) as MyCropParams
        trimTimeStart = get(EXTRA_TRIM_START) as Int
        trimTimeEnd = get(EXTRA_TRIM_END) as Int
      }
      binding.chipCrop.text = getString(R.string.re_crop)
      loadFirstFrame()
    }
  }

  private val requestPermissionLauncher =
    registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
      Toast.makeText(this@GifActivity, when (isGranted) {
        true -> {
          materialToolbarSubtitle.text = ""
          getString(R.string.permission_granted_you_can_continue_converting_to_gif)
        }
        false -> getString(R.string.unable_to_save_your_gif_without_storage_permission)
      }, Toast.LENGTH_LONG).show()
    }

  // takes ~5 secs
  private val videoKeyFramesTimestampList by lazy {
    // count all key frames FFprobeKit.execute("-hide_banner -loglevel error -skip_frame nokey -select_streams v:0 -count_frames -show_entries stream=nb_read_frames -of csv=p=0:sv=fail ${inputVideoSaf()}").logsAsString.split(",").first().toInt()
    val list = FFprobeKit.execute("-loglevel error -skip_frame nokey -select_streams v:0 -show_entries frame=pts_time -of csv=p=0:sv=fail ${inputVideoSaf()}").allLogsAsString.split("\n").map {
      try {
        (it.toFloat() * 1000f).roundToInt() // 1 == 1ms
      } catch (e: NumberFormatException) {
        UNKNOWN_INT
      }
    }.filter { it != UNKNOWN_INT }
    MyToolbox.logging("counted", "videoKeyFramesTimestampList.count() = ${list.count()}")
    return@lazy list
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
    with(binding.llcMoreOptionsButton) {
      when (MySettings.alwaysShowMoreOptionsWhenConvertingGif) {
        true -> {
          visibility = GONE
          binding.llcMoreOptionsGroup.visibility = VISIBLE
        }
        false -> {
          setOnClickListener {
            visibility = GONE
            binding.llcMoreOptionsGroup.visibility = VISIBLE
          }
        }
      }
    }
    linearProgressIndicator = binding.linearProgressIndicator
    inputVideoUri = intent.getParcelableExtra(EXTRA_VIDEO_URI) ?: intent.getParcelableExtra(Intent.EXTRA_STREAM) ?: intent.data ?: Uri.EMPTY
    if (inputVideoUri == Uri.EMPTY) {
      loadVideoFailed()
      return
    }
    videoInformationSession = FFprobeKit.getMediaInformationFromCommand("-v quiet -hide_banner -print_format json -show_format -show_streams -show_chapters -i " + inputVideoSaf())
    viewMaskLayer = binding.viewMaskLayer
    mbConvert = binding.mbConvert.apply { setOnClickListener { onConvertClick() } }
    mbCancelInGroupConvert = binding.mbCancel.apply {
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
    binding.chipRotate.setOnClickListener {
      rotationAdd90()
      loadCroppedAndTrimmedThumbnail()
      binding.chipRotate.text = getString(R.string.re_rotate)
    }
    binding.chipCrop.setOnClickListener {
      getCropAndTrimResult.launch(CropActivity.startIntent(
        this@GifActivity,
        myCropParams,
        defaultCropParams,
        inputVideoUri,
        trimTimeStart,
        trimTimeEnd
      ))
    }
  }

  private fun rotationAdd90() {
    rotation = if (rotation == 270) {
      0
    } else {
      rotation + 90
    }
  }

  private fun setDefaultCropParams() {
    try {
      val videoStreamInfo = videoInformationSession.mediaInformation.streams.first { it.type == "video" }
      trimTimeStart = 0
      trimTimeEnd = inputVideoDuration()
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
      val width = videoStreamInfo.width.toInt()
      val height = videoStreamInfo.height.toInt()
      defaultCropParams = when (rotation % 180) {
        0 -> MyCropParams(width, height, 0, 0)
        else -> MyCropParams(height, width, 0, 0)
      }
      myCropParams = defaultCropParams
    } catch (e: Exception) {
      e.printStackTrace()
      loadVideoFailed()
    }
  }

  private fun loadCustomMenuItemView() {
    cmivSpeed = binding.cmivSpeed
    cmivResolution = binding.cmivResolution
    cmivFrameRate = binding.cmivFrameRate
    cmivColorQuality = binding.cmivColorQuality
    cmivSpeed.setUpWithDropDownConfig(GIF_SPEED_MAP, CUSTOM_MENU_ITEM_VIEW_TYPE_GIF_SPEED)
    cmivResolution.setUpWithDropDownConfig(GIF_RESOLUTION_MAP, CUSTOM_MENU_ITEM_VIEW_TYPE_GIF_CHIPS)
    cmivFrameRate.setUpWithDropDownConfig(GIF_FRAME_RATE_MAP, CUSTOM_MENU_ITEM_VIEW_TYPE_GIF_CHIPS)
    cmivColorQuality.setUpWithDropDownConfig(GIF_COLOR_QUALITY_MAP, CUSTOM_MENU_ITEM_VIEW_TYPE_GIF_CHIPS)
    loadPreviousGifConfig()
  }

  private fun loadPreviousGifConfig() {
    when (MySettings.rememberGifOptions) {
      true -> {
        val previousGifConfigSpeed = MySettings.previousGifConfigSpeed
        val previousGifConfigResolution = MySettings.previousGifConfigResolution
        val previousGifConfigFrameRate = MySettings.previousGifConfigFrameRate
        val previousGifConfigColorQuality = MySettings.previousGifConfigColorQuality
        if (previousGifConfigSpeed != INT_PREVIOUS_GIF_CONFIG_UNKNOWN_VALUE &&
          previousGifConfigResolution != INT_PREVIOUS_GIF_CONFIG_UNKNOWN_VALUE &&
          previousGifConfigFrameRate != INT_PREVIOUS_GIF_CONFIG_UNKNOWN_VALUE &&
          previousGifConfigColorQuality != INT_PREVIOUS_GIF_CONFIG_UNKNOWN_VALUE
        ) {
          cmivSpeed.setSelectedValue(previousGifConfigSpeed)
          cmivResolution.setSelectedValue(previousGifConfigResolution)
          cmivFrameRate.setSelectedValue(previousGifConfigFrameRate)
          cmivColorQuality.setSelectedValue(previousGifConfigColorQuality)
          materialToolbarSubtitle.text = getString(R.string.previously_saved_options_loaded)
          Handler(Looper.getMainLooper()).postDelayed({
            if (materialToolbarSubtitle.text.toString() == getString(R.string.previously_saved_options_loaded)) {
              materialToolbarSubtitle.text = ""
            }
          }, MATERIAL_TOOLBAR_SUBTITLE_TEMP_DISPLAY_DURATION)
        } else {
          materialToolbarSubtitle.text = ""
        }
      }
      false -> materialToolbarSubtitle.text = ""
    }
  }

  private fun saveCurrentGifConfig() {
    when (MySettings.rememberGifOptions) {
      true -> {
        MySettings.previousGifConfigSpeed = cmivSpeed.selectedValue()
        MySettings.previousGifConfigResolution = cmivResolution.selectedValue()
        MySettings.previousGifConfigFrameRate = cmivFrameRate.selectedValue()
        MySettings.previousGifConfigColorQuality = cmivColorQuality.selectedValue()
      }
      false -> {} // do nothing
    }
  }

  private fun loadFirstFrame() {
    runOnUiThread {
      binding.chipCrop.isEnabled = false
      binding.chipRotate.isEnabled = false
      Glide.with(this@GifActivity).clear(binding.acivPreviewVideo)
    }
    try {
      // FFmpegKit.executeAsync("$FFMPEG_COMMAND_FOR_ALL -i ${inputVideoSaf()} -vframes 1 -vf scale=${resolutionPara(1080)}:flags=lanczos -q:v 5 -y $FIRST_FRAME_PATH") {
      val commandLoadFirstFrame = "$FFMPEG_COMMAND_FOR_ALL -ss ${trimTimeStart}ms -i ${inputVideoSaf()} -frames:v 1 -q:v 10 -y $FIRST_FRAME_PATH"
      MyToolbox.logging("command", "loadFirstFrame: $commandLoadFirstFrame")
      FFmpegKit.executeAsync(commandLoadFirstFrame) {
        when (it.returnCode.isValueSuccess) {
          true -> loadCroppedAndTrimmedThumbnail()
          false -> loadVideoFailed()
        }
      }
    } catch (e: Exception) {
      loadVideoFailed()
    }
  }

  private fun loadCroppedAndTrimmedThumbnail() {
    runOnUiThread {
      binding.chipCrop.isEnabled = false
      binding.chipRotate.isEnabled = false
      Glide.with(this@GifActivity).clear(binding.acivPreviewVideo)
    }
    try {
      val commandLoadCroppedAndTrimmedThumbnail = "$FFMPEG_COMMAND_FOR_ALL -i $FIRST_FRAME_PATH -vf ${cropParams()}${transposeParams()} -q:v 10 -y $THUMBNAIL_PATH"
      MyToolbox.logging("command", "loadCroppedAndTrimmedThumbnail: $commandLoadCroppedAndTrimmedThumbnail")
      FFmpegKit.executeAsync(commandLoadCroppedAndTrimmedThumbnail) {
        when (it.returnCode.isValueSuccess) {
          true -> {
            runOnUiThread {
              Glide.with(this@GifActivity)
                .load(THUMBNAIL_PATH)
                .fitCenter()
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .transition(withCrossFade())
                .into(binding.acivPreviewVideo)
              binding.chipCrop.isEnabled = true
              binding.chipRotate.isEnabled = true
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
          mbCancelInGroupConvert.visibility = GONE
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
          mbCancelInGroupConvert.visibility = VISIBLE
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
      linearProgressIndicator.visibility = GONE
      materialToolbar.subtitle = getString(R.string.gif_saved_s_mb, MyToolbox.keepNDecimalPlaces(MyToolbox.getFileSizeFromUri(gifUri) / 1048576.0, 2))
      binding.flAcivPreviewGifContainer.visibility = VISIBLE
      binding.llcButtonGroupConvert.visibility = GONE
      binding.llcButtonGroupDone.visibility = VISIBLE
      Glide.with(this@GifActivity)
        .load(when (gifUri.scheme) {
          "content" -> gifUri
          "file" -> gifUri.path
          else -> throw IllegalArgumentException("gifUri.scheme = ${gifUri.scheme}")
        })
        .fitCenter()
        .diskCacheStrategy(DiskCacheStrategy.NONE)
        .skipMemoryCache(true)
        .into(binding.acivPreviewGif)
      binding.nsvGoneWhenFinished.visibility = GONE
      binding.mbDone.setOnClickListener {
        finishAndRemoveTask()
      }
      binding.mbShare.setOnClickListener {
        startActivity(Intent.createChooser(Intent().apply {
          action = Intent.ACTION_SEND
          putExtra(Intent.EXTRA_STREAM, gifUri)
          type = "image/gif"
        }, getString(R.string.share_gif_to)))
      }
      binding.mbDeleteAndRedo.setOnClickListener {
        deleteGifUriFile()
        materialToolbar.subtitle = getString(R.string.the_gif_just_converted_has_been_deleted)
        Handler(Looper.getMainLooper()).postDelayed({
          if (materialToolbarSubtitle.text.toString() == getString(R.string.the_gif_just_converted_has_been_deleted)) {
            materialToolbarSubtitle.text = ""
          }
        }, MATERIAL_TOOLBAR_SUBTITLE_TEMP_DISPLAY_DURATION)
        viewMaskLayer.visibility = GONE
        binding.nsvGoneWhenFinished.visibility = VISIBLE
        binding.flAcivPreviewGifContainer.visibility = GONE
        binding.llcButtonGroupConvert.visibility = VISIBLE
        binding.llcButtonGroupDone.visibility = GONE
        mbCancelInGroupConvert.visibility = VISIBLE
        mbConvert.apply {
          text = getString(R.string.convert_to_gif)
          icon = AppCompatResources.getDrawable(this@GifActivity, R.drawable.ic_baseline_video_library_24)
        }
      }
    }
  }

  private fun videoKeyFramesTimestampInTrimmedCount(): Int {
    val count = videoKeyFramesTimestampList.count { it in trimTimeStart..trimTimeEnd }
    MyToolbox.logging("counted", "videoKeyFramesTimestampInTrimmedCount() = $count")
    return count
  }

  private fun createNewGifFileAndReturnUri(): Uri {
    val fileName = "${MyToolbox.getFileNameFromUri(inputVideoUri, true)}_CuteGIF_${MyToolbox.getTimeYMDHMS()}.gif"
    return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
      val dir = "${Environment.getExternalStorageDirectory().path}/Pictures/CuteGIF/"
      val path = "$dir$fileName"
      File(dir).mkdirs()
      val file = File(path)
      file.createNewFile()
      Uri.fromFile(file)
    } else {
      MyApplication.context.contentResolver.insert(MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CuteGif")
        put(MediaStore.Images.Media.MIME_TYPE, "image/gif")
      })!!
    }
  }

  private fun startConversion() {
    val analyzeVideoSlowly = MySettings.analyzeVideoSlowly
    thread {
      // Note: set framestep for command1 will not increase speed
      val command1Vf =
        when (analyzeVideoSlowly) {
          false -> "${cropParams()}${transposeParams()},scale=${resolutionParams(GIF_RESOLUTION_MAP.values.min())}:flags=fast_bilinear,palettegen=max_colors=${cmivColorQuality.selectedValue()}:stats_mode=diff"
          true -> "${cropParams()}${transposeParams()},scale=${resolutionParams()}:flags=lanczos,palettegen=max_colors=${cmivColorQuality.selectedValue()}:stats_mode=diff"
        }
      //  val trimCommand = "-ss ${trimTimeStart / 10.0} -to ${trimTimeEnd / 10.0}"
      val command1 = if (command1Vf == command1VfCached && trimTimeCached.first == trimTimeStart && trimTimeCached.second == trimTimeEnd) {
        "-version" // do nothing
      } else {
        when (analyzeVideoSlowly && cmivSpeed.selectedValue() != GIF_SPEED_GLANCE_MODE) {
          false -> {
            // make sure at least 1 key frame in trimTimeKeyFrameStart..trimTimeKeyFrameEnd
            val trimTimeKeyFrameStart = videoKeyFramesTimestampList.filter { it < trimTimeStart }.maxOrNull() ?: 0
            val trimTimeKeyFrameEnd = videoKeyFramesTimestampList.filter { it > trimTimeEnd }.minOrNull() ?: inputVideoDuration()
            "$FFMPEG_COMMAND_FOR_ALL -skip_frame nokey -ss ${trimTimeKeyFrameStart}ms -to ${trimTimeKeyFrameEnd}ms -i ${inputVideoSaf()} -vf $command1Vf -y $PALETTE_PATH"
          }
          true -> {
            "$FFMPEG_COMMAND_FOR_ALL -ss ${trimTimeStart}ms -to ${trimTimeEnd}ms -i ${inputVideoSaf()} -vf $command1Vf -y $PALETTE_PATH"
          }
        }
      }
      MyToolbox.logging("command1", command1)
      FFmpegKit.executeAsync(command1) {
        when {
          it.returnCode.isValueSuccess -> {
            command1VfCached = command1Vf
            trimTimeCached = Pair(trimTimeStart, trimTimeEnd)
            val outputFpsTarget = cmivFrameRate.selectedValue()
            val outputSpeed: Float
            val outputFramesEstimated: Int
            var progress: Int
            val command2 = when (cmivSpeed.selectedValue()) {
              GIF_SPEED_GLANCE_MODE -> {
                val command2FrameStep = 30 / outputFpsTarget
                outputFramesEstimated = ceil(videoKeyFramesTimestampInTrimmedCount().toDouble() / command2FrameStep).toInt()
                "$FFMPEG_COMMAND_FOR_ALL -skip_frame nokey -r 30 -ss ${trimTimeStart}ms -to ${trimTimeEnd}ms -i ${inputVideoSaf()} -i $PALETTE_PATH -lavfi \"framestep=$command2FrameStep,${cropParams()}${transposeParams()},scale=${resolutionParams()}:flags=lanczos [x]; [x][1:v] paletteuse=dither=bayer\" -final_delay ${MySettings.gifFinalDelay} -y ${outputGifPath()}"
              }
              else -> {
                outputSpeed = cmivSpeed.selectedValue() / 100f
                outputFramesEstimated = ceil((trimTimeEnd - trimTimeStart) * outputFpsTarget / outputSpeed / 1000f).toInt()
                "$FFMPEG_COMMAND_FOR_ALL -ss ${trimTimeStart}ms -to ${trimTimeEnd}ms -i ${inputVideoSaf()} -i $PALETTE_PATH -lavfi \"setpts=PTS/$outputSpeed,fps=fps=${outputFpsTarget},${cropParams()}${transposeParams()},scale=${resolutionParams()}:flags=lanczos [x]; [x][1:v] paletteuse=dither=bayer\" -final_delay ${MySettings.gifFinalDelay} -y ${outputGifPath()}"
              }
            }
            MyToolbox.logging("command2", command2)
            MyToolbox.logging("outputFramesEstimated", outputFramesEstimated.toString())
            FFmpegKit.executeAsync(command2, { ffmpegSession ->
              runOnUiThread { keepScreenOn(false) }
              when {
                ffmpegSession.returnCode.isValueSuccess -> conversionSuccessfully()
                ffmpegSession.returnCode.isValueError -> conversionUnsuccessfully(canceledByUser = false, finishActivity = true)
              }
            }, { log -> MyToolbox.logging("logcallback", log.message.toString()) }, {
              runOnUiThread {
                linearProgressIndicator.isIndeterminate = false
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

  @SuppressLint("SimpleDateFormat")
  private fun onConvertClick() {
    updateConvertingState()
    if (converting) {
      saveCurrentGifConfig()
      if ((Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
            && ContextCompat.checkSelfPermission(this@GifActivity, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PERMISSION_GRANTED)
        || Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
      ) {
        gifUri = createNewGifFileAndReturnUri()
        startConversion()
      } else {
        updateConvertingState()
        materialToolbarSubtitle.text = getString(R.string.you_need_to_allow_storage_permission_to_save_your_gif)
        requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
      }
    }
  }

  private fun inputVideoDuration() = try {
    (((videoInformationSession.mediaInformation.streams.first { it.type == "video" }.getStringProperty("duration"))
      ?: (videoInformationSession.mediaInformation.duration)).toFloat() * 1000f).roundToInt()
  } catch (e: Exception) {
    e.printStackTrace()
    loadVideoFailed()
    UNKNOWN_INT
  }

  private fun inputVideoSaf() = FFmpegKitConfig.getSafParameterForRead(this, inputVideoUri)

  private fun outputGifPath() =
    when (gifUri.scheme) {
      "content" -> FFmpegKitConfig.getSafParameterForWrite(this, gifUri)
      "file" -> gifUri.path
      else -> throw IllegalArgumentException("gifUri.scheme = ${gifUri.scheme}")
    }

  private fun conversionUnsuccessfully(canceledByUser: Boolean, finishActivity: Boolean) {
    runOnUiThread {
      keepScreenOn(false)
      if (!canceledByUser) {
        Toast.makeText(this@GifActivity, getString(R.string.conversion_failed), Toast.LENGTH_LONG).show()
      }
      FFmpegKitConfig.clearSessions()
      FFmpegKit.cancel()
      if (::gifUri.isInitialized) {
        deleteGifUriFile()
      }
      if (finishActivity) {
        finishAndRemoveTask()
      }
    }
  }

  private fun deleteGifUriFile() {
    try {
      when (gifUri.scheme) {
        "content" -> contentResolver.delete(gifUri, null, null)
        "file" -> File(gifUri.path!!).delete()
        else -> throw IllegalArgumentException("gifUri.scheme = ${gifUri.scheme}")
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  private fun inputVideoFps(): Float {
    val fpsFraction = videoInformationSession.mediaInformation.streams.first { it.type == "video" }.averageFrameRate
    val numerator = fpsFraction.split("/").toTypedArray()[0].toInt()
    val denominator = fpsFraction.split("/").toTypedArray()[1].toInt()
    return numerator.toFloat() / denominator
  }

  private fun resolutionParams(): String = resolutionParams(null)

  private fun resolutionParams(shortLength: Int?): String {
    // val long = max(myCropParams.outW, myCropParams.outH)
    val short = min(myCropParams.outW, myCropParams.outH)
    val pixel = min(shortLength ?: cmivResolution.selectedValue(), short)
    return if ((myCropParams.outW > myCropParams.outH) == (rotation % 180 == 0)) {
      "-2:$pixel"
    } else {
      "$pixel:-2"
    }
  }

  private fun cropParams() = "crop=${myCropParams.outW}:${myCropParams.outH}:${myCropParams.x}:${myCropParams.y}"

  private fun transposeParams() =
    when (rotation) {
      0 -> ""
      90 -> ",transpose=1"
      180 -> ",transpose=1,transpose=1"
      270 -> ",transpose=2"
      else -> throw IllegalArgumentException("cropParams.rotatedDegrees = $rotation")
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
    fun start(context: Context, videoUri: Uri) {
      context.startActivity(Intent(context, GifActivity::class.java).putExtra(EXTRA_VIDEO_URI, videoUri))
    }
  }
}