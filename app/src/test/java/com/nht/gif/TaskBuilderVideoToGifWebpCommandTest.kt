package com.nht.gif

import com.nht.gif.model.WebpQuality
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskBuilderVideoToGifWebpCommandTest {

  private val framesPath = "/cache/frames/"
  private val outputPath = "/cache/output_temp.webp"
  private val fps = 10

  private fun cmd(quality: WebpQuality) =
    TaskBuilderVideoToGif.buildWebpCommand(framesPath, outputPath, fps, quality)

  @Test
  fun `SMALL preset contains -quality 50 -compression_level 6`() {
    val command = cmd(WebpQuality.SMALL)
    assertTrue(command.contains("-quality 50 -compression_level 6"))
  }

  @Test
  fun `MEDIUM preset contains -quality 75 -compression_level 6`() {
    val command = cmd(WebpQuality.MEDIUM)
    assertTrue(command.contains("-quality 75 -compression_level 6"))
  }

  @Test
  fun `HIGH preset contains -quality 90 -compression_level 6`() {
    val command = cmd(WebpQuality.HIGH)
    assertTrue(command.contains("-quality 90 -compression_level 6"))
  }

  @Test
  fun `LOSSLESS preset contains -lossless 1 -compression_level 6 and no -quality flag`() {
    val command = cmd(WebpQuality.LOSSLESS)
    assertTrue(command.contains("-lossless 1 -compression_level 6"))
    assertFalse(command.contains("-quality"))
  }

  @Test
  fun `command ends with -loop 0 -y followed by output path`() {
    val command = cmd(WebpQuality.MEDIUM)
    assertTrue(command.trimEnd().endsWith("-loop 0 -y \"$outputPath\""))
  }
}
