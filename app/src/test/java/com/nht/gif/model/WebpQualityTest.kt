package com.nht.gif.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WebpQualityTest {

  @Test
  fun `SMALL preset has ffmpegQuality 50 and lossless false`() {
    assertEquals(50, WebpQuality.SMALL.ffmpegQuality)
    assertFalse(WebpQuality.SMALL.lossless)
  }

  @Test
  fun `MEDIUM preset has ffmpegQuality 75 and lossless false`() {
    assertEquals(75, WebpQuality.MEDIUM.ffmpegQuality)
    assertFalse(WebpQuality.MEDIUM.lossless)
  }

  @Test
  fun `HIGH preset has ffmpegQuality 90 and lossless false`() {
    assertEquals(90, WebpQuality.HIGH.ffmpegQuality)
    assertFalse(WebpQuality.HIGH.lossless)
  }

  @Test
  fun `LOSSLESS preset has null ffmpegQuality and lossless true`() {
    assertNull(WebpQuality.LOSSLESS.ffmpegQuality)
    assertTrue(WebpQuality.LOSSLESS.lossless)
  }
}
