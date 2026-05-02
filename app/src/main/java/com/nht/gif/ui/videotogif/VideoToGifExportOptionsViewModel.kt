package com.nht.gif.ui.videotogif

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nht.gif.CropParams
import com.nht.gif.data.EstimationSettings
import com.nht.gif.data.FileSizeEstimator
import com.nht.gif.data.FileSizeEstimatorImpl
import com.nht.gif.model.EstimationState
import com.nht.gif.model.OutputFormat
import com.nht.gif.model.WebpQuality
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/** Holds UI state for the export-options dialog (format selection, quality presets, size estimates). */
class VideoToGifExportOptionsViewModel(
  private val inputVideoPath: String,
  private val duration: Int,
  private val cropParams: CropParams,
  private val outputSpeed: Float,
  private val estimator: FileSizeEstimator,
) : ViewModel() {

  private val _outputFormat = MutableStateFlow(OutputFormat.GIF)
  val outputFormat: StateFlow<OutputFormat> = _outputFormat.asStateFlow()

  private val _webpQuality = MutableStateFlow(WebpQuality.MEDIUM)
  val webpQuality: StateFlow<WebpQuality> = _webpQuality.asStateFlow()

  private val _showLosslessWarning = MutableStateFlow(false)
  val showLosslessWarning: StateFlow<Boolean> = _showLosslessWarning.asStateFlow()

  private val _fps = MutableStateFlow(10)
  val fps: StateFlow<Int> = _fps.asStateFlow()

  private val _shortLength = MutableStateFlow(240)
  val shortLength: StateFlow<Int> = _shortLength.asStateFlow()

  // Defaults match the UI initial selections: Color Quality = High (128 colors), Clarity = High (lossy 30)
  private val _colorQuality = MutableStateFlow(128)
  val colorQuality: StateFlow<Int> = _colorQuality.asStateFlow()

  private val _lossy: MutableStateFlow<Int?> = MutableStateFlow(30)
  val lossy: StateFlow<Int?> = _lossy.asStateFlow()

  private val _estimationState = MutableStateFlow<EstimationState>(EstimationState.Loading)
  val estimationState: StateFlow<EstimationState> = _estimationState.asStateFlow()

  private var estimationJob: Job? = null

  init {
    viewModelScope.launch {
      combine(
        combine(outputFormat, webpQuality, fps, shortLength) { _, _, _, _ -> Unit },
        combine(colorQuality, lossy) { _, _ -> Unit },
      ) { _, _ -> Unit }.collect { scheduleEstimation() }
    }
  }

  fun setOutputFormat(format: OutputFormat) {
    _outputFormat.value = format
    if (format == OutputFormat.ANIMATED_WEBP) setWebpQuality(WebpQuality.MEDIUM)
  }

  fun setWebpQuality(quality: WebpQuality) {
    _webpQuality.value = quality
    _showLosslessWarning.value = quality == WebpQuality.LOSSLESS
  }

  fun setFps(fps: Int) { _fps.value = fps }

  fun setShortLength(shortLength: Int) { _shortLength.value = shortLength }

  fun setColorQuality(colorQuality: Int) { _colorQuality.value = colorQuality }

  fun setLossy(lossy: Int?) { _lossy.value = lossy }

  private fun scheduleEstimation() {
    estimationJob?.cancel()
    _estimationState.value = EstimationState.Loading
    estimationJob = viewModelScope.launch {
      delay(ESTIMATION_DEBOUNCE_MS)
      _estimationState.value = try {
        val (gif, webp) = estimator.estimate(buildSettings())
        EstimationState.Ready(gif, webp)
      } catch (e: CancellationException) {
        throw e
      } catch (_: Exception) {
        EstimationState.Error
      }
    }
  }

  private fun buildSettings() = EstimationSettings(
    inputVideoPath = inputVideoPath,
    sampleDurationMs = minOf(duration.toLong(), 1000L),
    fullDurationMs = maxOf(duration.toLong(), 1L),
    outputFps = fps.value,
    shortLength = shortLength.value,
    cropParams = cropParams,
    outputSpeed = outputSpeed,
    webpQuality = webpQuality.value,
    colorQuality = colorQuality.value,
    lossy = lossy.value,
  )

  override fun onCleared() {
    super.onCleared()
    estimationJob?.cancel()
  }

  companion object {
    private const val ESTIMATION_DEBOUNCE_MS = 300L

    fun factory(
      inputVideoPath: String,
      duration: Int,
      cropParams: CropParams,
      outputSpeed: Float,
      estimator: FileSizeEstimator = FileSizeEstimatorImpl(),
    ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
      @Suppress("UNCHECKED_CAST")
      override fun <T : ViewModel> create(modelClass: Class<T>): T =
        VideoToGifExportOptionsViewModel(
          inputVideoPath, duration, cropParams, outputSpeed, estimator
        ) as T
    }
  }
}
