package me.tasy5kg.cutegif

import android.os.Build
import com.tencent.mmkv.MMKV
import me.tasy5kg.cutegif.MyApplication.Companion.appContext

object MySettings {
  private val defaultMMKV by lazy {
    MMKV.initialize(appContext)
    MMKV.defaultMMKV()!!
  }

  private const val INT_NULL_VALUE = Int.MIN_VALUE
  private const val FLOAT_NULL_VALUE = Float.MIN_VALUE

  private fun getBoolean(key: String) =
    when (getInt(key)) {
      0 -> false
      1 -> true
      else -> null
    }

  private fun setBoolean(key: String, value: Boolean?) =
    setInt(
      key, when (value) {
        false -> 0
        true -> 1
        null -> INT_NULL_VALUE
      }
    )

  private fun getInt(key: String) = defaultMMKV.decodeInt(key, INT_NULL_VALUE).toNullable()

  private fun setInt(key: String, value: Int?) {
    defaultMMKV.encode(key, value ?: INT_NULL_VALUE)
  }

  private fun getFloat(key: String) = defaultMMKV.decodeFloat(key, FLOAT_NULL_VALUE).toNullable()

  private fun setFloat(key: String, value: Float?) {
    defaultMMKV.encode(key, value ?: FLOAT_NULL_VALUE)
  }

  private fun getString(key: String) = defaultMMKV.decodeString(key)

  private fun setString(key: String, value: String?) {
    defaultMMKV.encode(key, value)
  }

  private fun Int.toNullable() = if (this == INT_NULL_VALUE) null else this

  private fun Float.toNullable() = if (this == FLOAT_NULL_VALUE) null else this

  /** Note: This is accepted EULA license version code, not app version code.*/
  private const val KEY_INT_ACCEPTED_EULA_VERSION = "int_accepted_eula_version"
  private const val INT_EULA_VERSION_LATEST = 202400000
  var eulaAccepted
    get() = INT_EULA_VERSION_LATEST == getInt(KEY_INT_ACCEPTED_EULA_VERSION)
    set(value) = setInt(KEY_INT_ACCEPTED_EULA_VERSION, if (value) INT_EULA_VERSION_LATEST else null)

  private const val KEY_INT_FILE_OPEN_WAY = "int_file_open_way"
  const val INT_FILE_OPEN_WAY_DOCUMENT = 1
  const val INT_FILE_OPEN_WAY_GALLERY = 2
  const val INT_FILE_OPEN_WAY_13 = 13
  private val INT_FILE_OPEN_WAY_DEFAULT =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) INT_FILE_OPEN_WAY_13 else INT_FILE_OPEN_WAY_GALLERY
  var fileOpenWay: Int
    get() = getInt(KEY_INT_FILE_OPEN_WAY) ?: INT_FILE_OPEN_WAY_DEFAULT
    set(value) = setInt(KEY_INT_FILE_OPEN_WAY, value)

  private const val KEY_INT_WHATS_NEW_READ_VERSION = "int_read_changelog_version"
  private const val INT_WHATS_NEW_VERSION_LATEST = 202403000
  var whatsNewRead
    get() = INT_WHATS_NEW_VERSION_LATEST == getInt(KEY_INT_WHATS_NEW_READ_VERSION)
    set(value) = setInt(KEY_INT_WHATS_NEW_READ_VERSION, if (value) INT_WHATS_NEW_VERSION_LATEST else null)
}