package com.nht.gif.model

/** Represents the lifecycle of a file-size estimation run. */
sealed class EstimationState {
  data object Loading : EstimationState()
  data class Ready(val gifSizeBytes: Long, val webpSizeBytes: Long) : EstimationState()
  data object Error : EstimationState()
}

/**
 * Formats a byte count as a human-readable size string.
 * Returns "~X KB" for values below 1 MB, "~X.X MB" otherwise.
 */
fun formatEstimatedSize(bytes: Long): String =
  if (bytes < 1_048_576L) "~${bytes / 1024} KB"
  else "~${"%.1f".format(bytes / 1_048_576.0)} MB"

/**
 * Extrapolates a sample file size to the full clip duration.
 * Formula: sampleSizeBytes × (fullDurationMs / sampleDurationMs)
 */
fun extrapolateSize(sampleSizeBytes: Long, sampleDurationMs: Long, fullDurationMs: Long): Long =
  sampleSizeBytes * fullDurationMs / sampleDurationMs
