package me.tasy5kg.cutegif

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.widget.doAfterTextChanged
import com.arthenica.ffmpegkit.FFmpegKit
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import me.tasy5kg.cutegif.MyConstants.EXTRA_IMAGES_CLIP_DATA
import me.tasy5kg.cutegif.MyConstants.FFMPEG_COMMAND_PREFIX_FOR_ALL_AN
import me.tasy5kg.cutegif.MyConstants.IMAGE_FALLBACK_JPG_PATH
import me.tasy5kg.cutegif.MyConstants.PALETTE_PATH
import me.tasy5kg.cutegif.MyConstants.SLIDESHOW_DIR_PATH
import me.tasy5kg.cutegif.Toolbox.closestEven
import me.tasy5kg.cutegif.Toolbox.createFfSafForRead
import me.tasy5kg.cutegif.Toolbox.createFfSafForWrite
import me.tasy5kg.cutegif.Toolbox.fileSize
import me.tasy5kg.cutegif.Toolbox.firstVideoStream
import me.tasy5kg.cutegif.Toolbox.formatFileSize
import me.tasy5kg.cutegif.Toolbox.getImageRotatedWidthAndHeight
import me.tasy5kg.cutegif.Toolbox.keepScreenOn
import me.tasy5kg.cutegif.Toolbox.logRed
import me.tasy5kg.cutegif.Toolbox.makeDirEmpty
import me.tasy5kg.cutegif.Toolbox.mediaInformation
import me.tasy5kg.cutegif.Toolbox.toUriList
import me.tasy5kg.cutegif.Toolbox.toast
import me.tasy5kg.cutegif.databinding.ActivitySlideshowBinding
import java.io.File
import java.io.FileOutputStream
import kotlin.concurrent.thread
import kotlin.math.min

class SlideshowActivity : BaseActivity() {
  private val shortLength = 360 // TODO
  private val maxColors = 256 // TODO
  private val binding by lazy { ActivitySlideshowBinding.inflate(layoutInflater) }
  private val materialToolbar by lazy { binding.materialToolbar }
  private val materialToolbarSubtitle by lazy { materialToolbar.getChildAt(1) as AppCompatTextView }
  private val imageUriList by lazy {
    ((intent.getParcelableExtra(EXTRA_IMAGES_CLIP_DATA) ?: intent.clipData))?.toUriList()
      ?: mutableListOf()
  }
  private lateinit var gifWidthAndHeight: Pair<Int, Int>

  private lateinit var outputGifUri: Uri

  override fun onCreateIfEulaAccepted(savedInstanceState: Bundle?) {
    setContentView(binding.root)
    setFinishOnTouchOutside(false)
    setSupportActionBar(materialToolbar)
    materialToolbarSubtitle.apply {
      doAfterTextChanged {
        visibility = if (text.isNullOrBlank()) GONE else VISIBLE
      }
    }
    if (imageUriList.isEmpty()) {
      loadImageFailed()
    }
    /*
    //TODO
    binding.cmivImageInterval.apply {
      setUpWithDropDownConfig(GIF_FRAME_INTERVAL_MAP, CUSTOM_MENU_ITEM_VIEW_TYPE_GIF_FINAL_DELAY)
      setSelectedValue(100) //TODO remember user's choice
    }*/
    binding.mbConvert.setOnClickListener {
      onConvertClick()
    }
    makeDirEmpty(SLIDESHOW_DIR_PATH)
  }

  private fun onConvertClick() {
    binding.llcGoneWhenConversionStarted.visibility = GONE
    runOnUiThread { keepScreenOn(true) }
    thread {
      // Step (1/3): Crop images
      var imageIndex = 0
      try {
        gifWidthAndHeight =
          imageUriList[0].requiresFallback().getImageRotatedWidthAndHeight()
        gifWidthAndHeight =
          with(gifWidthAndHeight) {
            if (first < second) Pair(
              shortLength,
              (second.toDouble() / first * shortLength).closestEven()
            ) else Pair(
              (first.toDouble() / second * shortLength).closestEven(),
              shortLength
            )
          }
        imageUriList.forEachIndexed { index, uri ->
          runOnUiThread {
            materialToolbarSubtitle.text = "正在载入图片（${index}/${imageUriList.size}）"
          }
          imageIndex = index
          val command =
            "$FFMPEG_COMMAND_PREFIX_FOR_ALL_AN -i ${
              uri.requiresFallback().createFfSafForRead()
            } -vframes 1 -vf \"scale=${gifWidthAndHeight.toResolutionPara()}:force_original_aspect_ratio=decrease,pad=${gifWidthAndHeight.toResolutionPara()}:-2:-2:color=black\" -pix_fmt yuvj420p -f mjpeg -y ${
              importedImagePath(
                index
              )
            }"
          logRed("command_index$index", command)
          if (!FFmpegKit.execute(command).returnCode.isValueSuccess
          ) {
            throw Exception()
          }
        }
      } catch (e: Exception) {
        loadImageFailed("载入第 ${imageIndex + 1} 张图片时出错")
        return@thread
      }
      // Step (2/3): Analyse images
      try {
        runOnUiThread {
          materialToolbarSubtitle.text = "正在分析图片..."
        }
        val commandAnalyseImage =
          "$FFMPEG_COMMAND_PREFIX_FOR_ALL_AN -f image2 -i ${importedImagePath(null)} -filter_complex \"palettegen=max_colors=$maxColors:stats_mode=diff\" -y $PALETTE_PATH"
        logRed("commandAnalyseImage", commandAnalyseImage)
        if (!FFmpegKit.execute(commandAnalyseImage).returnCode.isValueSuccess
        ) {
          throw Exception()
        }
      } catch (e: Exception) {
        loadImageFailed("分析图片时出错")
        return@thread
      }
      // Step (3/3): Convert images to GIF
      //outputGifUri = Toolbox.createNewFile() TODO
      // todo   val commandImagesToGif = "$FFMPEG_COMMAND_PREFIX_FOR_ALL -r ${100.0 / binding.cmivImageInterval.selectedValue()} -f image2 -i ${importedImagePath(null)} -i $PALETTE_PATH -filter_complex \"[0:v][1:v] paletteuse=dither=bayer\" -final_delay ${MySettings.gifFinalDelay} -y ${outputGifUri.createFfSafForWrite()}"
      val commandImagesToGif =
        "$FFMPEG_COMMAND_PREFIX_FOR_ALL_AN -r ${100.0} -f image2 -i ${importedImagePath(null)} -i $PALETTE_PATH -filter_complex \"[0:v][1:v] paletteuse=dither=bayer\" -final_delay ${TODO()} -y ${outputGifUri.createFfSafForWrite()}"
      FFmpegKit.executeAsync(commandImagesToGif, { ffmpegSession ->
        runOnUiThread { keepScreenOn(false) }
        when {
          ffmpegSession.returnCode.isValueSuccess -> conversionSuccessfully()
          ffmpegSession.returnCode.isValueError -> loadImageFailed("转换失败")
        }
      }, { log -> logRed("logcallback", log.message.toString()) }, {
        runOnUiThread {
          val progress = min(it.videoFrameNumber * 100 / imageUriList.size, 99)
          //linearProgressIndicator.isIndeterminate = false
          //linearProgressIndicator.setProgress(progress, true)
          materialToolbar.subtitle =
            getString(R.string.converting_s_s, progress, it.size.formatFileSize())
        }
      })
    }
  }

  private fun conversionSuccessfully() {
    makeDirEmpty(SLIDESHOW_DIR_PATH)
    runOnUiThread {
      binding.relativeLayout.visibility = VISIBLE
      sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE).apply {
        data = outputGifUri
      })
      materialToolbar.subtitle =
        getString(R.string.gif_saved_s, outputGifUri.fileSize().formatFileSize())
      Glide.with(this)
        .load(
          when (outputGifUri.scheme) {
            "content" -> outputGifUri
            "file" -> outputGifUri.path
            else -> throw IllegalArgumentException("gifUri.scheme = ${outputGifUri.scheme}")
          }
        )
        .fitCenter()
        .diskCacheStrategy(DiskCacheStrategy.NONE)
        .skipMemoryCache(true)
        .into(binding.aciv)
    }
  }

  private fun Pair<Int, Int>.toResolutionPara() = "$first:$second"

  private fun importedImagePath(imageIndex: Int?) =
    if (imageIndex == null) "${SLIDESHOW_DIR_PATH}/image_%04d.jpg" else "${SLIDESHOW_DIR_PATH}/image_${
      String.format(
        "%04d",
        imageIndex + 1
      )
    }.jpg"

  /** throw Exception if failed to decode this image */
  private fun Uri.requiresFallback(): Uri {
    return if (this.mediaInformation()?.firstVideoStream() == null) {
      // FFmpeg does not support this image
      logRed("image fallback", "enabled: Bitmap convert to JPEG")
      val inputStream = contentResolver.openInputStream(this)!!
      BitmapFactory.decodeStream(inputStream)
        .compress(
          Bitmap.CompressFormat.JPEG,
          100,
          FileOutputStream(IMAGE_FALLBACK_JPG_PATH)
        )
      inputStream.close()
      Uri.fromFile(File(IMAGE_FALLBACK_JPG_PATH))
    } else {
      this // no fallback needed
    }
  }

  private fun loadImageFailed(toastText: String = "载入图片失败") {
    runOnUiThread {
      toast(toastText)
      keepScreenOn(false)
      makeDirEmpty(SLIDESHOW_DIR_PATH)
      finish()
    }
  }

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    menuInflater.inflate(R.menu.toolbar_close, menu)
    return true
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    when (item.itemId) {
      R.id.menu_item_close -> {
        finish()
        // TODO  conversionUnsuccessfullyAndDelete(canceledByUser = true, finishActivity = true)
      }
    }
    return true
  }

  companion object {
    private val EMPTY_CLIP_DATA = ClipData.newPlainText(null, null)
    fun start(context: Context, imagesClipData: ClipData) =
      context.startActivity(
        Intent(context, SlideshowActivity::class.java)
          .putExtra(EXTRA_IMAGES_CLIP_DATA, imagesClipData)
      )
  }
}