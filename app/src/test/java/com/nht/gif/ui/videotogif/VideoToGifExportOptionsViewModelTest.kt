package com.nht.gif.ui.videotogif

import com.nht.gif.model.OutputFormat
import org.junit.Assert.assertEquals
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
}
