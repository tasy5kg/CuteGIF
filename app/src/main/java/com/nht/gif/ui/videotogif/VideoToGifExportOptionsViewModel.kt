package com.nht.gif.ui.videotogif

import androidx.lifecycle.ViewModel
import com.nht.gif.model.OutputFormat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Holds UI state for the export-options dialog (format selection, quality presets, etc.). */
class VideoToGifExportOptionsViewModel : ViewModel() {

  private val _outputFormat = MutableStateFlow(OutputFormat.GIF)
  val outputFormat: StateFlow<OutputFormat> = _outputFormat.asStateFlow()

  fun setOutputFormat(format: OutputFormat) {
    _outputFormat.value = format
  }
}
