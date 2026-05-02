package com.nht.gif.ui.videotogif

import com.nht.gif.CropParams
import com.nht.gif.data.EstimationSettings
import com.nht.gif.data.FileSizeEstimator
import com.nht.gif.model.EstimationState
import com.nht.gif.model.OutputFormat
import com.nht.gif.model.WebpQuality
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VideoToGifExportOptionsViewModelTest {

  private val testDispatcher = StandardTestDispatcher()
  private val mockEstimator: FileSizeEstimator = mockk(relaxed = true)
  private val testCropParams = CropParams(640, 480, 0, 0)

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  private fun createViewModel(estimator: FileSizeEstimator = mockEstimator) =
    VideoToGifExportOptionsViewModel(
      inputVideoPath = "/data/test.mp4",
      duration = 5000,
      cropParams = testCropParams,
      outputSpeed = 1f,
      estimator = estimator,
    )

  // T1.8
  @Test
  fun `outputFormat defaults to GIF on creation`() {
    val viewModel = createViewModel()
    assertEquals(OutputFormat.GIF, viewModel.outputFormat.value)
  }

  // T1.9
  @Test
  fun `setting outputFormat to ANIMATED_WEBP updates state`() {
    val viewModel = createViewModel()
    viewModel.setOutputFormat(OutputFormat.ANIMATED_WEBP)
    assertEquals(OutputFormat.ANIMATED_WEBP, viewModel.outputFormat.value)
  }

  // T1.10
  @Test
  fun `setting outputFormat back to GIF updates state`() {
    val viewModel = createViewModel()
    viewModel.setOutputFormat(OutputFormat.ANIMATED_WEBP)
    viewModel.setOutputFormat(OutputFormat.GIF)
    assertEquals(OutputFormat.GIF, viewModel.outputFormat.value)
  }

  // T2.13
  @Test
  fun `webpQuality defaults to MEDIUM when outputFormat switches to ANIMATED_WEBP`() {
    val viewModel = createViewModel()
    viewModel.setOutputFormat(OutputFormat.ANIMATED_WEBP)
    assertEquals(WebpQuality.MEDIUM, viewModel.webpQuality.value)
  }

  // T2.14
  @Test
  fun `showLosslessWarning is true only when webpQuality is LOSSLESS`() {
    val viewModel = createViewModel()
    viewModel.setOutputFormat(OutputFormat.ANIMATED_WEBP)

    viewModel.setWebpQuality(WebpQuality.SMALL)
    assertFalse(viewModel.showLosslessWarning.value)

    viewModel.setWebpQuality(WebpQuality.MEDIUM)
    assertFalse(viewModel.showLosslessWarning.value)

    viewModel.setWebpQuality(WebpQuality.HIGH)
    assertFalse(viewModel.showLosslessWarning.value)

    viewModel.setWebpQuality(WebpQuality.LOSSLESS)
    assertTrue(viewModel.showLosslessWarning.value)
  }

  // T3.17
  @Test
  fun `estimationState transitions to Ready on successful estimation`() = runTest {
    val estimator = mockk<FileSizeEstimator>()
    coEvery { estimator.estimate(any()) } returns (100_000L to 80_000L)

    val viewModel = createViewModel(estimator)
    assertEquals(EstimationState.Loading, viewModel.estimationState.value)

    advanceUntilIdle()
    assertEquals(EstimationState.Ready(100_000L, 80_000L), viewModel.estimationState.value)
  }

  // T3.18
  @Test
  fun `estimationState transitions to Error when estimator throws`() = runTest {
    val estimator = mockk<FileSizeEstimator>()
    coEvery { estimator.estimate(any()) } throws RuntimeException("FFmpeg error")

    val viewModel = createViewModel(estimator)
    advanceUntilIdle()
    assertEquals(EstimationState.Error, viewModel.estimationState.value)
  }

  // T3.19
  @Test
  fun `setting change within 300ms debounce cancels pending estimation`() = runTest {
    var callCount = 0
    val estimator = mockk<FileSizeEstimator>()
    coEvery { estimator.estimate(any()) } coAnswers { callCount++; 1L to 2L }

    val viewModel = createViewModel(estimator)

    // Let initial combine emit and start the 300ms debounce, but don't fire it yet
    advanceTimeBy(100)

    // Change fps within the 300ms window → cancels the pending job, resets debounce
    viewModel.setFps(5)

    // Let everything settle (new 300ms debounce + estimation)
    advanceUntilIdle()

    // Only 1 estimation ran; the first (cancelled) job never reached estimate()
    assertEquals(1, callCount)
    assertTrue(viewModel.estimationState.value is EstimationState.Ready)
  }
}
