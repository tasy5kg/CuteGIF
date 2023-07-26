package me.tasy5kg.cutegif

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.floor

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals("2:03.4", msToMinSecDs(123456L))
  }

  private fun msToMinSecDs(ms: Long) =
    with(ms / 1000f) {
      "${(this / 60).toInt()}:${String.format("%02d", (this % 60).toInt())}.${
        ((this - floor(
          this
        )) * 10).toInt()
      }"
    }
}