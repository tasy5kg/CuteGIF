package me.tasy5kg.cutegif.toolbox

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.MediaMetadataRetriever
import androidx.annotation.IntRange
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFprobeKit
import com.arthenica.ffmpegkit.MediaInformation
import me.tasy5kg.cutegif.MyApplication
import me.tasy5kg.cutegif.MyConstants
import me.tasy5kg.cutegif.toolbox.Toolbox.swapIf
import me.tasy5kg.cutegif.toolbox.Toolbox.toEmptyStringIf
import org.json.JSONArray
import org.json.JSONObject
import java.io.FileOutputStream
import kotlin.math.roundToInt


/**
 * 当能够访问媒体文件的绝对路径时，请勿将其通转换为 Uri 传入给 FFmpegKit，
 * 因为这样传入时，文件将不带有文件后辍名，可能导致 FFmpeg 无法正确读取媒体文件！
 */
object MediaTools {

  /**
   * a function to generate a transparent [Bitmap] with the given width and height.
   * @param w The width of the [Bitmap].
   * @param h The height of the [Bitmap].
   * @return The generated transparent [Bitmap].
   */
  fun generateTransparentBitmap(w: Int, h: Int) =
    Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.TRANSPARENT) }

  fun getVideoDurationByAndroidSystem(path: String) = with(MediaMetadataRetriever()) {
    try {
      setDataSource(path)
      val duration=extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)!!.toInt()
      if (duration==0) null else duration
    } catch (_: Exception) {
      null
    } finally {
      release()
    }
  }

  fun getVideoSingleFrame(path: String, timestamp_ms: Long) =
    with(MediaMetadataRetriever()) {
      try {
        setDataSource(path)
        getFrameAtTime(
          timestamp_ms * 1000L,
          MediaMetadataRetriever.OPTION_CLOSEST_SYNC // OPTION_CLOSEST is slower and may cause NullPointerException, avoid using it.
        )!!
      } catch (e: Exception) {
        /**
         * Even if it is set to OPTION_CLOSEST_SYNC, the getFrameAtTime() method still has the probability of NullPointerException.
         * Therefore, use FFmpeg as a fallback method.
         **/
        /**
         * Even if it is set to OPTION_CLOSEST_SYNC, the getFrameAtTime() method still has the probability of NullPointerException.
         * Therefore, use FFmpeg as a fallback method.
         **/
        getVideoSingleFrameWithFFmpeg(path, timestamp_ms, 5, MyConstants.GET_VIDEO_SINGLE_FRAME_WITH_FFMPEG_TEMP_PATH)
        BitmapFactory.decodeFile(MyConstants.GET_VIDEO_SINGLE_FRAME_WITH_FFMPEG_TEMP_PATH).copy(Bitmap.Config.ARGB_8888, true)!!
      } finally {
        release()
      }
    }

  fun getVideoSingleFrameWithFFmpeg(path: String, timestamp_ms: Long, @IntRange(2, 31) quality: Int, outputPath: String) =
    FFmpegKit.execute("${MyConstants.FFMPEG_COMMAND_PREFIX_FOR_ALL_AN} -ss ${timestamp_ms}ms -i $path -frames:v 1 -q:v $quality -y $outputPath")!!

  fun Bitmap.saveToPng(path: String) =
    FileOutputStream(path).let {
      this.compress(Bitmap.CompressFormat.PNG, 100, it)
      it.close()
    }

  fun Bitmap.saveToJpg(path: String, @IntRange(0, 100) quality: Int) =
    FileOutputStream(path).let {
      this.compress(Bitmap.CompressFormat.JPEG, quality, it)
      it.close()
    }

  fun getVideoFps(path: String) = try {
    val fpsFraction = mediaInformation(path)!!.streams.first { it.type == "video" }.averageFrameRate
    val numerator = fpsFraction.split("/").toTypedArray()[0].toInt()
    val denominator = fpsFraction.split("/").toTypedArray()[1].toInt()
    numerator.toDouble() / denominator
  } catch (_: Exception) {
    null
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
        Toolbox.logRed("rotation = $rotation", "rotation % 90 != 0")
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

  fun getVideoRotation(path: String) =
    getRotationFromProperties(mediaInformation(path)!!.firstVideoStream()!!.allProperties)

  fun getImageRotation(path: String) =
    getRotationFromProperties(mediaInformation(path, true)!!.allProperties.getJSONArray("frames").getJSONObject(0))

  /**
   * @param rotation can be obtained via getVideoRotation() or getImageRotation()
   */
  fun getRotatedWidthAndHeight(path: String, rotation: Int) =
    (mediaInformation(path)!!.firstVideoStream()!!).let {
      Pair(it.width.toInt(), it.height.toInt()).swapIf { rotation % 180 != 0 }
    }

  fun MediaInformation.firstVideoStream() = streams.firstOrNull { it.type == "video" }

  fun getVideoDurationMsByFFmpeg(path: String) =
    try {
      val mediaInformation = mediaInformation(path)!!
      (((mediaInformation.firstVideoStream()!!.getStringProperty("duration")) ?: (mediaInformation.duration)).toFloat() * 1000f).roundToInt()
    } catch (_: Exception) {
      null
    }

  fun mediaInformation(path: String, withFrames: Boolean = false): MediaInformation? =
    FFprobeKit.getMediaInformationFromCommand(
      "-v quiet -hide_banner -print_format json -show_format -show_streams ${("-show_frames ").toEmptyStringIf { !withFrames }}-i $path"
    ).mediaInformation

  fun getImageWidthHeight(path: String) =
    with(BitmapFactory.Options()) {
      this.inJustDecodeBounds = true
      BitmapFactory.decodeFile(path, this)
      Pair(this.outWidth, this.outHeight)
    }

  /** Slow operation: this function may takes at least 5s! */
  fun videoKeyFramesTimestampList(path: String) =
    FFprobeKit.execute(
      "-loglevel error -skip_frame nokey -select_streams v:0 -show_entries frame=pts_time $path"
    ).allLogsAsString
      .split("\n")
      .filter { it.startsWith("pts_time=") }
      .map { ((it.split('=')[1]).toFloat() * 1000f).toInt() }

  /**
   * lossy should >= 0 .
   * return true when succeed, false when failed.
   * if outputGifPath is null, then output will overwrite input file.
   */
  fun gifsicleLossy(
    lossy: Int,
    inputGifPath: String,
    outputGifPath: String?,
    enableO3: Boolean,
  ): Boolean {
    val nativeLibraryDir = MyApplication.appContext.applicationInfo.nativeLibraryDir
    val gifsiclePath = "${nativeLibraryDir}/libgifsicle.so"
    val gifsicleEnvp = arrayOf("LD_LIBRARY_PATH=${nativeLibraryDir}")
    val gifsicleCmd = when (outputGifPath) {
      null -> "$gifsiclePath -b -O3 --lossy=$lossy $inputGifPath"
      else -> "$gifsiclePath -O3 --lossy=$lossy --output $outputGifPath $inputGifPath"
    }
    return try {
      (Runtime.getRuntime().exec(gifsicleCmd, gifsicleEnvp).waitFor() == 0)
    } catch (e: Exception) {
      Toolbox.logRed("gifsicleLossy() failed", e.message)
      false
    }
  }

}