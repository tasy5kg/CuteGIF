package com.nht.gif.model

/**
 * Quality preset for animated WebP export.
 *
 * [ffmpegQuality] maps to FFmpeg's `-quality` flag (null for lossless encoding).
 * [lossless] maps to FFmpeg's `-lossless 1` flag.
 */
enum class WebpQuality(val ffmpegQuality: Int?, val lossless: Boolean) {
  SMALL(50, false),
  MEDIUM(75, false),
  HIGH(90, false),
  LOSSLESS(null, true)
}
