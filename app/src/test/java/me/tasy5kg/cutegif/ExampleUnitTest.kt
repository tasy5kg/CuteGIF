package me.tasy5kg.cutegif

import me.tasy5kg.cutegif.toolbox.Toolbox.msToMinSecDs
import org.junit.Assert.assertEquals
import org.junit.jupiter.api.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
  @Test
  fun msToMinSecDs_isCorrect() {
    assertEquals("2:03.4", msToMinSecDs(123456))
  }
}