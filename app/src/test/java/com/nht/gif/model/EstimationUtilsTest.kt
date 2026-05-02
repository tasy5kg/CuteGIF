package com.nht.gif.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EstimationUtilsTest {

  // T3.13 — below 1 MB returns "~X KB"
  @Test
  fun `formatEstimatedSize below 1MB returns KB format`() {
    val result = formatEstimatedSize(512 * 1024L) // 512 KB
    assertEquals("~512 KB", result)
  }

  // T3.14 — at or above 1 MB returns "~X.X MB"
  @Test
  fun `formatEstimatedSize at 1MB returns MB format`() {
    val result = formatEstimatedSize(1_048_576L) // exactly 1 MB
    assertEquals("~1.0 MB", result)
  }

  @Test
  fun `formatEstimatedSize above 1MB returns MB format with one decimal`() {
    val result = formatEstimatedSize(2_621_440L) // 2.5 MB
    assertEquals("~2.5 MB", result)
  }

  // T3.15 — output always starts with "~"
  @Test
  fun `formatEstimatedSize always starts with tilde`() {
    assertTrue(formatEstimatedSize(1024L).startsWith("~"))
    assertTrue(formatEstimatedSize(2_000_000L).startsWith("~"))
  }

  // T3.16 — extrapolation formula
  @Test
  fun `extrapolateSize returns sampleSize times fullDuration divided by sampleDuration`() {
    val sampleSizeBytes = 100_000L
    val sampleDurationMs = 1000L
    val fullDurationMs = 5000L
    assertEquals(500_000L, extrapolateSize(sampleSizeBytes, sampleDurationMs, fullDurationMs))
  }

  @Test
  fun `extrapolateSize with equal durations returns sample size unchanged`() {
    assertEquals(200_000L, extrapolateSize(200_000L, 1000L, 1000L))
  }
}
