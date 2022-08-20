package me.tasy5kg.cutegif

import android.graphics.Rect
import android.os.Parcel
import android.os.Parcelable
import java.io.Serializable

class MyCropParams() : Parcelable {
  var outW: Int = 0
  var outH: Int = 0
  var x: Int = 0
  var y: Int = 0

  constructor(parcel: Parcel) : this() {
    outW = parcel.readInt()
    outH = parcel.readInt()
    x = parcel.readInt()
    y = parcel.readInt()
  }

  constructor(rect: Rect) : this() {
    this.x = rect.left
    this.y = rect.top
    this.outW = rect.width()
    this.outH = rect.height()
  }

  constructor(outW: Int, outH: Int, x: Int, y: Int) : this() {
    this.outW = outW
    this.outH = outH
    this.x = x
    this.y = y
  }

  fun toRect() = Rect(x, y, x + outW, y + outH)
  override fun writeToParcel(parcel: Parcel, flags: Int) {
    parcel.writeInt(outW)
    parcel.writeInt(outH)
    parcel.writeInt(x)
    parcel.writeInt(y)
  }

  override fun describeContents(): Int {
    return 0
  }

  companion object CREATOR : Parcelable.Creator<MyCropParams> {
    override fun createFromParcel(parcel: Parcel): MyCropParams {
      return MyCropParams(parcel)
    }

    override fun newArray(size: Int): Array<MyCropParams?> {
      return arrayOfNulls(size)
    }
  }

}
