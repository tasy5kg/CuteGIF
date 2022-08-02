package me.tasy5kg.cutegif

import android.graphics.Rect
import java.io.Serializable

class MyCropParams : Serializable {
  var outW: Int = 0
  var outH: Int = 0
  var x: Int = 0
  var y: Int = 0
  var rotatedDegrees: Int = 0

  constructor(rect: Rect, rotatedDegrees: Int) {
    this.x = rect.left
    this.y = rect.top
    this.outW = rect.width()
    this.outH = rect.height()
    this.rotatedDegrees = rotatedDegrees
  }

  constructor(outW: Int, outH: Int, x: Int, y: Int, rotatedDegrees: Int) {
    this.outW = outW
    this.outH = outH
    this.x = x
    this.y = y
    this.rotatedDegrees = rotatedDegrees
  }

  fun rect() = Rect(x, y, x + outW, y + outH)

}
