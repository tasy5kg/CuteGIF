package com.nht.gif.ui.videotogif

import com.nht.gif.model.OutputFormat
import com.nht.gif.model.WebpQuality
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoToGifExportOptionsViewModelTest {

  private fun createViewModel() = VideoToGifExportOptionsViewModel()

  @Test
  fun `outputFormat defaults to GIF on creation`() {
    val viewModel = createViewModel()
    assertEquals(OutputFormat.GIF, viewModel.outputFormat.value)
  }

  @Test
  fun `setting outputFormat to ANIMATED_WEBP updates state`() {
    val viewModel = createViewModel()
    viewModel.setOutputFormat(OutputFormat.ANIMATED_WEBP)
    assertEquals(OutputFormat.ANIMATED_WEBP, viewModel.outputFormat.value)
  }

  @Test
  fun `setting outputFormat back to GIF updates state`() {
    val viewModel = createViewModel()
    viewModel.setOutputFormat(OutputFormat.ANIMATED_WEBP)
    viewModel.setOutputFormat(OutputFormat.GIF)
    assertEquals(OutputFormat.GIF, viewModel.outputFormat.value)
  }

  @Test
  fun `webpQuality defaults to MEDIUM when outputFormat switches to ANIMATED_WEBP`() {
    val viewModel = createViewModel()
    viewModel.setOutputFormat(OutputFormat.ANIMATED_WEBP)
    assertEquals(WebpQuality.MEDIUM, viewModel.webpQuality.value)
  }

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
}
