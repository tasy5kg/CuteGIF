package me.tasy5kg.cutegif

import android.annotation.SuppressLint
import android.app.Activity
import android.content.*
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.icu.text.SimpleDateFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.text.SpannableString
import android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.Log
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.WindowManager
import android.webkit.MimeTypeMap
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.ColorInt
import androidx.annotation.StringRes
import androidx.core.graphics.ColorUtils
import com.arthenica.ffmpegkit.FFmpegKitConfig
import com.arthenica.ffmpegkit.FFprobeKit
import com.arthenica.ffmpegkit.MediaInformation
import com.google.android.material.slider.RangeSlider
import me.tasy5kg.cutegif.MyApplication.Companion.appContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.Serializable
import java.util.*
import kotlin.math.roundToInt

object Toolbox {
  inline fun newRunnableWithSelf(crossinline lambda: (Runnable) -> Unit) =
    object : Runnable {
      override fun run() {
        lambda(this)
      }
    }

  data class UriWrapper(val uriString: String) : Serializable {
    constructor(uri: Uri) : this(uri.toString())

    fun getUri() = Uri.parse(uriString)!!
  }

  fun generateTransparentBitmap(w: Int, h: Int) =
    Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.TRANSPARENT) }!!

  inline fun <V : View> V.onClick(
    hapticFeedbackType: HapticFeedbackType? = null,
    crossinline lambda: V.() -> Unit
  ) =
    setOnClickListener { _ ->
      hapticFeedbackType?.let { performHapticFeedback(it.value) }
      lambda(this)
    }

  inline fun RangeSlider.onSliderTouch(
    crossinline onStartTrackingTouch: RangeSlider.() -> Unit,
    crossinline onStopTrackingTouch: RangeSlider.() -> Unit
  ) {
    addOnSliderTouchListener(object : RangeSlider.OnSliderTouchListener {
      override fun onStartTrackingTouch(slider: RangeSlider) = onStartTrackingTouch(slider)
      override fun onStopTrackingTouch(slider: RangeSlider) = onStopTrackingTouch(slider)
    })
  }

  fun <A, B> Collection<Pair<A, B>>.getB(a: A) = first { it.first == a }.second

  fun <T : Comparable<T>> T.constraintBy(range: ClosedRange<T>) =
    when {
      this < range.start -> range.start
      this > range.endInclusive -> range.endInclusive
      else -> this
    }

  fun Boolean.toInt() = if (this) 1 else 0

  fun View.flipVisibility() {
    visibility = if (visibility == VISIBLE) GONE else VISIBLE
  }

  inline fun <T : View> T.visibleIf(condition: T.() -> Boolean) {
    visibility = if (condition(this)) VISIBLE else GONE
  }

  @Suppress("UNCHECKED_CAST", "DEPRECATION")
  fun <T> Intent.getExtra(key: String) = this.extras!!.get(key) as T

  fun appGetString(@StringRes resId: Int) = appContext.getString(resId)

  fun <K, V> LinkedHashMap<K, V>.getKeyByValue(value: V): K =
    this.filter { it.value == value }.keys.first()

  @SuppressLint("SimpleDateFormat")
  fun getTimeYMDHMS(): String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())

  fun getVideoSingleFrame(uri: Uri, timestamp_ms: Long, accurate: Boolean) =
    with(MediaMetadataRetriever()) {
      setDataSource(appContext, uri)
      val bitmap = getFrameAtTime(
        timestamp_ms * 1000L,
        if (accurate) MediaMetadataRetriever.OPTION_CLOSEST else MediaMetadataRetriever.OPTION_CLOSEST_SYNC
      )!!
      release()
      bitmap
    }

  fun getImageWidthHeight(path: String) =
    with(BitmapFactory.Options()) {
      this.inJustDecodeBounds = true
      BitmapFactory.decodeFile(path, this)
      Pair(this.outWidth, this.outHeight)
    }

  fun Bitmap.saveToPng(path: String) {
    val out = FileOutputStream(path)
    this.compress(Bitmap.CompressFormat.PNG, 100, out)
    out.close()
  }

  fun openLink(context: Context, url: String) =
    try {
      context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (_: Exception) {
    }

  fun view3rdPartyOSSLicenses(context: Context) {
    TODO("//context.startActivity(Intent(context, OssLicensesMenuActivity::class.java))")
  }

  fun firstLocaleLikeZhHansCn() =
    with(appContext.resources.configuration.locales[0]) {
      language == "zh" || script == "Hans" || country == "CN"
    }

  fun logRed(tag: Any?, msg: Any?) {
    if (BuildConfig.DEBUG) {
      Log.e(tag.toString(), msg.toString())
    }
  }

  fun Double.closestEven() = (this / 2).roundToInt() * 2

  fun toast(text: String) = Toast.makeText(appContext, text, Toast.LENGTH_LONG).show()

  fun createNewFile(inputFileUri: Uri, fileType: String) =
    createNewFile(inputFileUri.fileName().removeFileNameExtension(), fileType)

  fun createNewFile(fileNamePrefix: String?, fileType: String): Uri {
    val appName = appGetString(R.string.app_name)
    val fileName =
      ("${fileNamePrefix}_").toEmptyStringIf { fileNamePrefix.isNullOrBlank() } + "${appName}_${getTimeYMDHMS()}.$fileType"
    return when (fileType) {
      "mp4" -> appContext.contentResolver.insert(
        MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
        ContentValues().apply {
          put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
          put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/${appName}")
          put(MediaStore.Video.Media.MIME_TYPE, MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileType))
        })
      "gif", "png" -> appContext.contentResolver.insert(
        MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
        ContentValues().apply {
          put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
          put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/${appName}")
          put(MediaStore.Images.Media.MIME_TYPE, MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileType))
        })
      else -> throw NotImplementedError("fileType = $fileType")
    }!!
  }

  fun copyFile(srcPath: String, destUri: Uri, deleteSrc: Boolean = false) {
    val destUriOutputStream = appContext.contentResolver.openOutputStream(destUri)!!
    val srcFile = File(srcPath)
    val srcPathInputStream = FileInputStream(srcFile)
    srcPathInputStream.copyTo(destUriOutputStream)
    destUriOutputStream.close()
    srcPathInputStream.close()
    if (deleteSrc) {
      srcFile.delete()
    }
  }

  fun Uri.deleteFile() {
    try {
      when (this.scheme) {
        "content" -> appContext.contentResolver.delete(this, null, null)
        "file" -> appContext.contentResolver.delete(
          MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
          MediaStore.Images.Media.DATA + "=?",
          arrayOf(this.path)
        )

        else -> throw IllegalArgumentException("gifUri.scheme = ${this.scheme}")
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  /**
   *  Usage example:
   *    createColorStateListFromColorResource(arrayOf(
   *      android.R.attr.state_checked to R.color.green_light,
   *      android.R.attr.state_checkable to R.color.light))
   */
  fun createColorStateListFromColorResource(list: Array<Pair<Int, Int>>) =
    ColorStateList(
      list.map { intArrayOf(it.first) }.toTypedArray(),
      list.map { appContext.resources.getColor(it.second, appContext.theme) }.toIntArray()
    )

  /**
   *  Usage example:
   *    createColorStateListFromColorParsed(arrayOf(
   *      android.R.attr.state_checked to Color.parseColor("#000000"),
   *      android.R.attr.state_checkable to Color.parseColor("#FFFFFF")))
   */
  fun createColorStateListFromColorParsed(list: Array<Pair<Int, Int>>) =
    ColorStateList(
      list.map { intArrayOf(it.first) }.toTypedArray(),
      list.map { it.second }.toIntArray()
    )

  fun copyTextToClipboard(@StringRes stringId: Int, @StringRes toastStringId: Int) =
    copyTextToClipboard(appGetString(stringId), appGetString(toastStringId))

  fun copyTextToClipboard(text: String, toastText: String?) {
    appContext.getSystemService(ClipboardManager::class.java)
      .setPrimaryClip(ClipData.newPlainText(null, text))
    toastText?.let { toast(it) }
  }

  /** @throws NullPointerException **/
  fun Uri.videoDuration(): Int {
    val mediaInformation = this.mediaInformation()!!
    return (((mediaInformation.firstVideoStream()!!.getStringProperty("duration"))
      ?: (mediaInformation.duration)).toFloat() * 1000f).roundToInt()
  }

  fun Uri.videoFps(): Double {
    val fpsFraction =
      this.mediaInformation()!!.streams.first { it.type == "video" }.averageFrameRate
    val numerator = fpsFraction.split("/").toTypedArray()[0].toInt()
    val denominator = fpsFraction.split("/").toTypedArray()[1].toInt()
    return numerator.toDouble() / denominator
  }

  enum class FileSizeUnit(val unitName: String, val multiple: Double) {
    B("B", 1.0),
    KB("KB", 1024.0),
    MB("MB", 1048576.0),
    GB("GB", 1073741824.0);
  }

  fun Uri.fileSize() =
    when (scheme) {
      "content" -> {
        val assetFileDescriptor =
          appContext.contentResolver.openAssetFileDescriptor(this, "r")!!
        val fileSize = assetFileDescriptor.length
        assetFileDescriptor.close()
        fileSize
      }

      "file" -> File(path!!).length()
      else -> throw IllegalArgumentException("uri.scheme = $scheme")
    }

  fun Long.formatFileSize(
    fileSizeUnit: FileSizeUnit = FileSizeUnit.KB,
    decimalPlaces: Int = 0,
    appendUnit: Boolean = true
  ) =
    (this / fileSizeUnit.multiple).keepNDecimalPlaces(decimalPlaces) +
        if (appendUnit) {
          fileSizeUnit.unitName
        } else {
          ""
        }

  fun interface CopyFileProgressUpdatedListener {
    fun onCopyFileProgressUpdated(valueCode: Int)
  }

  /* inline fun copyFile(srcUri: Uri, destPath: String, listener: CopyFileProgressUpdatedListener? = null) {
      try {
        val fileSize = srcUri.fileSize().toInt()
        val input = appContext.contentResolver.openInputStream(srcUri)!!
        val output = FileOutputStream(File(destPath))
        val buf = ByteArray(4096)
        var bytesRead: Int
        while (input.read(buf).also { bytesRead = it } > 0) {
          output.write(buf, 0, bytesRead)
          listener?.onCopyFileProgressUpdated(min(bytesRead * 100 / fileSize, 99))
        }
        input.close()
        output.close()
      } catch (e: Exception) {
        e.printStackTrace()
        listener?.onCopyFileProgressUpdated(COPY_FILE_PROGRESS_ERROR)
        return
      }
      listener?.onCopyFileProgressUpdated(COPY_FILE_PROGRESS_SUCCESS)
    }*/

  fun Double.keepNDecimalPlaces(n: Int) = String.format("%.${n}f", this)

  fun Float.keepNDecimalPlaces(n: Int) = String.format("%.${n}f", this)

  fun Activity.keepScreenOn(keepScreenOn: Boolean) =
    runOnUiThread {
      if (keepScreenOn) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) else window.clearFlags(
        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
      )
    }

  fun getRotationFromProperties(properties: JSONObject) =
    try {
      var rotation = 0
      val sideDataListJSONArray = (properties.get("side_data_list") as JSONArray)
      (0 until sideDataListJSONArray.length()).forEach {
        try {
          rotation = -sideDataListJSONArray.getJSONObject(it).getInt("rotation")
        } catch (_: Exception) {
        }
      }
      if (rotation % 90 != 0) {
        logRed("rotation = $rotation", "rotation % 90 != 0")
        rotation = 0
      }
      while (rotation < 0) {
        rotation += 360
      }
      while (rotation >= 360) {
        rotation -= 360
      }
      rotation
    } catch (_: Exception) {
      0
    }

  fun Uri.createFfSafForRead() = FFmpegKitConfig.getSafParameter(appContext, this, "r")!!

  fun Uri.createFfSafForWrite() = FFmpegKitConfig.getSafParameter(appContext, this, "w")!!

  fun Uri.createFfSafForRW() = FFmpegKitConfig.getSafParameter(appContext, this, "rw")!!

  fun MediaInformation.firstVideoStream() = streams.firstOrNull { it.type == "video" }

  fun Uri.mediaInformation(withFrames: Boolean = false): MediaInformation? =
    FFprobeKit.getMediaInformationFromCommand(
      "-v quiet -hide_banner -print_format json -show_format -show_streams ${("-show_frames ").toEmptyStringIf { !withFrames }}-i ${createFfSafForRead()}"
    ).mediaInformation

  fun Uri.getVideoRotation() =
    getRotationFromProperties(this.mediaInformation()!!.firstVideoStream()!!.allProperties)

  fun Uri.getImageRotation() =
    getRotationFromProperties(
      this.mediaInformation(true)!!.allProperties.getJSONArray("frames").getJSONObject(0)
    )

  fun Uri.getImageRotatedWidthAndHeight() =
    (this.mediaInformation()!!.firstVideoStream()!!).let {
      Pair(it.width.toInt(), it.height.toInt()).swapIf { getImageRotation() % 180 != 0 }
    }

  fun Uri.getVideoRotatedWidthAndHeight() =
    (this.mediaInformation()!!.firstVideoStream()!!).let {
      Pair(it.width.toInt(), it.height.toInt()).swapIf { getVideoRotation() % 180 != 0 }
    }

  inline fun String.toEmptyStringIf(condition: String.() -> Boolean) =
    if (condition(this)) "" else this

  fun <T> Pair<T, T>.swap() = Pair(second, first)

  inline fun <T> Pair<T, T>.swapIf(condition: Pair<T, T>.() -> Boolean) =
    if (condition(this)) this.swap() else this

  fun ClipData.toUriList(): MutableList<Uri> {
    val uriList = mutableListOf<Uri>()
    for (i in 0 until this.itemCount) {
      uriList.add(this.getItemAt(i).uri)
    }
    return uriList
  }

  fun Uri.fileName() =
    when (this.scheme) {
      "content" -> {
        appContext.contentResolver.query(this, null, null, null, null)!!.use { cursor ->
          val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
          cursor.moveToFirst()
          cursor.getString(nameIndex)
        }
      }

      "file" -> this.lastPathSegment
      else -> throw IllegalArgumentException("uri.scheme = $scheme")
    }!!

  fun String.fileNameExtension() = substringAfterLast(".")

  fun String.removeFileNameExtension() = substringBeforeLast(".")

  fun Uri.mimeType() =
    when (scheme) {
      "content" -> appContext.contentResolver.getType(this)
      "file" -> MimeTypeMap.getSingleton().getMimeTypeFromExtension(this.fileName().fileNameExtension())
      else -> null
    }

  fun cmivJoinQqGroupLambda(context: Context) {
    val intent = Intent().apply {
      data = Uri.parse(MyConstants.URI_JOIN_QQ_GROUP)
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
      context.startActivity(intent)
    } catch (e: Exception) {
      copyTextToClipboard(
        appGetString(R.string.qq_group_id),
        appGetString(R.string.join_qq_group_toast)
      )
    }
  }

  fun TextView.setupTextViewWithClickablePart(
    @StringRes fullText: Int,
    listOfSubStringIdAndOnClick: List<Pair<Int, () -> Unit>>,
    isUnderlineSubText: Boolean = false,
  ) = this.setupTextViewWithClickablePart(
    appGetString(fullText),
    listOfSubStringIdAndOnClick.map {
      Pair(appGetString(it.first), it.second)
    },
    isUnderlineSubText
  )

  fun TextView.setupTextViewWithClickablePart(
    fullText: String,
    listOfSubStringAndOnClick: List<Pair<String, () -> Unit>>,
    isUnderlineSubText: Boolean = false,
  ) {
    this.text = SpannableString(fullText).apply {
      for (subStringAndOnClick in listOfSubStringAndOnClick) {
        val subStringIndex = fullText.indexOf(subStringAndOnClick.first)
        this.setSpan(
          object : ClickableSpan() {
            override fun onClick(widget: View) {
              subStringAndOnClick.second()
            }

            override fun updateDrawState(ds: TextPaint) {
              super.updateDrawState(ds)
              ds.isUnderlineText = isUnderlineSubText
            }
          },
          subStringIndex,
          subStringIndex + subStringAndOnClick.first.length,
          SPAN_EXCLUSIVE_EXCLUSIVE
        )
      }
    }
    this.movementMethod = LinkMovementMethod.getInstance()
  }

  @ColorInt
  fun Int.getContrastColor() =
    if (ColorUtils.calculateContrast(this, Color.BLACK) > ColorUtils.calculateContrast(
        this,
        Color.WHITE
      )
    )
      Color.BLACK
    else
      Color.WHITE

  inline fun SpannableString.setSpan(what: Any?) =
    setSpan(what, 0, length, SPAN_EXCLUSIVE_EXCLUSIVE)
}
