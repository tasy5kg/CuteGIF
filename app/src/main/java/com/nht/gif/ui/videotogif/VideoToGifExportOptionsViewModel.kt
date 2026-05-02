package com.nht.gif.ui.videotogif

import androidx.lifecycle.ViewModel
import com.nht.gif.model.OutputFormat
import com.nht.gif.model.WebpQuality
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Holds UI state for the export-options dialog (format selection, quality presets, etc.). */
class VideoToGifExportOptionsViewModel : ViewModel() {

  private val _outputFormat = MutableStateFlow(OutputFormat.GIF)
  val outputFormat: StateFlow<OutputFormat> = _outputFormat.asStateFlow()

  private val _webpQuality = MutableStateFlow(WebpQuality.MEDIUM)
  val webpQuality: StateFlow<WebpQuality> = _webpQuality.asStateFlow()

  private val _showLosslessWarning = MutableStateFlow(false)
  val showLosslessWarning: StateFlow<Boolean> = _showLosslessWarning.asStateFlow()

  fun setOutputFormat(format: OutputFormat) {
    _outputFormat.value = format
    if (format == OutputFormat.ANIMATED_WEBP) {
      setWebpQuality(WebpQuality.MEDIUM)
    }
  }

  fun setWebpQuality(quality: WebpQuality) {
    _webpQuality.value = quality
    _showLosslessWarning.value = quality == WebpQuality.LOSSLESS
  }
}
