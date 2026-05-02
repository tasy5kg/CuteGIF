package com.nht.gif.data

import com.arthenica.ffmpegkit.FFmpegKit
import com.nht.gif.CropParams
import com.nht.gif.MyConstants.CACHE_DIR_PATH
import com.nht.gif.MyConstants.FFMPEG_COMMAND_PREFIX_FOR_ALL_AN
import com.nht.gif.model.extrapolateSize
import com.nht.gif.toolbox.FileTools.resetDirectory
import com.nht.gif.toolbox.MediaTools.gifsicleLossy
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.min

/**
 * Encodes a short sample clip as both GIF and WebP into a private temp directory,
 * measures each output file size, and extrapolates to the full clip duration.
 * The temp directory is deleted after each estimation (success or failure).
 */
class FileSizeEstimatorImpl(
  private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : FileSizeEstimator {

  private val tempDir = "$CACHE_DIR_PATH/estimation/"
  private val framesDir = "${tempDir}frames/"
  private val palettePath = "${tempDir}palette.png"
  private val gifPath = "${tempDir}sample.gif"
  private val webpPath = "${tempDir}sample.webp"

  override suspend fun estimate(settings: EstimationSettings): Pair<Long, Long> =
    withContext(ioDispatcher) {
      try {
        resetDirectory(framesDir)
        extractFrames(settings)
        val gifSize = encodeGif(settings)
        val webpSize = encodeWebp(settings)
        extrapolateSize(gifSize, settings.sampleDurationMs, settings.fullDurationMs) to
          extrapolateSize(webpSize, settings.sampleDurationMs, settings.fullDurationMs)
      } finally {
        File(tempDir).deleteRecursively()
      }
    }

  private fun extractFrames(s: EstimationSettings) {
    val scale = scaleParam(s.cropParams, s.shortLength)
    FFmpegKit.execute(
      "$FFMPEG_COMMAND_PREFIX_FOR_ALL_AN -to ${s.sampleDurationMs}ms -i \"${s.inputVideoPath}\" " +
        "-vf \"setpts=PTS/${s.outputSpeed},fps=fps=${s.outputFps}," +
        "${s.cropParams.toFFmpegCropCommand()}$scale\" \"${framesDir}%06d.bmp\""
    )
  }

  private fun encodeGif(s: EstimationSettings): Long {
    FFmpegKit.execute(
      "$FFMPEG_COMMAND_PREFIX_FOR_ALL_AN -i \"${framesDir}%06d.bmp\" " +
        "-vf \"palettegen=max_colors=${s.colorQuality}:stats_mode=diff\" -y \"$palettePath\""
    )
    FFmpegKit.execute(
      "$FFMPEG_COMMAND_PREFIX_FOR_ALL_AN -framerate ${s.outputFps} -i \"${framesDir}%06d.bmp\" " +
        "-i \"$palettePath\" -filter_complex \"paletteuse=dither=bayer\" -final_delay 0 -y \"$gifPath\""
    )
    if (s.lossy != null) gifsicleLossy(s.lossy, gifPath, null, false)
    return File(gifPath).length()
  }

  private fun encodeWebp(s: EstimationSettings): Long {
    val qualityFlags = if (s.webpQuality.lossless) "-lossless 1"
    else "-quality ${s.webpQuality.ffmpegQuality}"
    FFmpegKit.execute(
      "$FFMPEG_COMMAND_PREFIX_FOR_ALL_AN -framerate ${s.outputFps} -i \"${framesDir}%06d.bmp\" " +
        "$qualityFlags -compression_level 6 -loop 0 -y \"$webpPath\""
    )
    return File(webpPath).length()
  }

  private fun scaleParam(cropParams: CropParams, shortLength: Int): String {
    val short = cropParams.shortLength()
    val pixel = min(shortLength, short)
    if (shortLength == 0 || shortLength >= short) return ""
    val dims = if (cropParams.outW > cropParams.outH) "-2:$pixel" else "$pixel:-2"
    return ",scale=$dims:flags=lanczos"
  }
}
