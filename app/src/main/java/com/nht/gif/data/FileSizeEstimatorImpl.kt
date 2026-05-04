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
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.min

/**
 * Encodes a short sample clip as both GIF and WebP into a private temp directory,
 * measures each output file size, and extrapolates to the full clip duration.
 *
 * Each [estimate] call gets its own uniquely-numbered subdirectory under
 * `<cache>/estimation/<runId>/`, so concurrent or overlapping invocations
 * (e.g. rapid quality switches before prior FFmpeg calls finish) never
 * corrupt each other's files. The directory is deleted in [estimate]'s
 * finally block regardless of success or failure.
 */
class FileSizeEstimatorImpl(
  private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : FileSizeEstimator {

  private val runCounter = AtomicLong()

  override suspend fun estimate(settings: EstimationSettings): Pair<Long, Long> =
    withContext(ioDispatcher) {
      val runDir = "$CACHE_DIR_PATH/estimation/${runCounter.getAndIncrement()}/"
      val framesDir = "${runDir}frames/"
      val palettePath = "${runDir}palette.png"
      val gifPath = "${runDir}sample.gif"
      val webpPath = "${runDir}sample.webp"
      try {
        resetDirectory(framesDir)
        extractFrames(settings, framesDir)
        val gifSize = encodeGif(settings, framesDir, palettePath, gifPath)
        val webpSize = encodeWebp(settings, framesDir, webpPath)
        extrapolateSize(gifSize, settings.sampleDurationMs, settings.fullDurationMs) to
          extrapolateSize(webpSize, settings.sampleDurationMs, settings.fullDurationMs)
      } finally {
        File(runDir).deleteRecursively()
      }
    }

  private fun extractFrames(s: EstimationSettings, framesDir: String) {
    val scale = scaleParam(s.cropParams, s.shortLength)
    FFmpegKit.execute(
      "$FFMPEG_COMMAND_PREFIX_FOR_ALL_AN -to ${s.sampleDurationMs}ms -i \"${s.inputVideoPath}\" " +
        "-vf \"setpts=PTS/${s.outputSpeed},fps=fps=${s.outputFps}," +
        "${s.cropParams.toFFmpegCropCommand()}$scale\" \"${framesDir}%06d.bmp\""
    )
  }

  private fun encodeGif(s: EstimationSettings, framesDir: String, palettePath: String, gifPath: String): Long {
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

  private fun encodeWebp(s: EstimationSettings, framesDir: String, webpPath: String): Long {
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
