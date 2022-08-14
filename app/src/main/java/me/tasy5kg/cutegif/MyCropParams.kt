package me.tasy5kg.cutegif

import android.graphics.Rect
import java.io.Serializable

class MyCropParams : Serializable {
  var outW: Int = 0
  var outH: Int = 0
  var x: Int = 0
  var y: Int = 0

  constructor(rect: Rect) {
    this.x = rect.left
    this.y = rect.top
    this.outW = rect.width()
    this.outH = rect.height()
  }

  constructor(outW: Int, outH: Int, x: Int, y: Int) {
    this.outW = outW
    this.outH = outH
    this.x = x
    this.y = y
  }

  fun toRect() = Rect(x, y, x + outW, y + outH)

}
