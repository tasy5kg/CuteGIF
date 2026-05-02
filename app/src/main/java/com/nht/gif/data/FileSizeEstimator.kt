package com.nht.gif.data

import com.nht.gif.CropParams
import com.nht.gif.model.WebpQuality

/** Parameters needed to run a file-size estimation sample encode. */
data class EstimationSettings(
    val inputVideoPath: String,
    /** Duration of the sample clip: min(fullDurationMs, 1000ms) */
    val sampleDurationMs: Long,
    val fullDurationMs: Long,
    val outputFps: Int,
    val shortLength: Int,
    val cropParams: CropParams,
    val outputSpeed: Float,
    val webpQuality: WebpQuality,
    /** GIF palette size (32 / 64 / 128 / 256 colors). Matches the Color Quality toggle. */
    val colorQuality: Int,
    /** Gifsicle lossy value (200 / 70 / 30), or null for lossless-max. Matches the Clarity toggle. */
    val lossy: Int?,
)

/**
 * Encodes a short sample of the clip as both GIF and WebP, then extrapolates
 * each output size to the full clip duration.
 * Returns (estimatedGifBytes, estimatedWebpBytes).
 */
interface FileSizeEstimator {
    suspend fun estimate(settings: EstimationSettings): Pair<Long, Long>
}
