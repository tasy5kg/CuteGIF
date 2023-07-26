package me.tasy5kg.cutegif

import android.os.Build
import com.tencent.mmkv.MMKV
import me.tasy5kg.cutegif.MyApplication.Companion.appContext

object MySettings {
  private val defaultMMKV by lazy {
    MMKV.initialize(appContext)
    MMKV.defaultMMKV()!!
  }

  /** Note: This is accepted EULA license version code, not app version code.*/
  private const val INT_ACCEPTED_EULA_LICENSE_VERSION_CODE =
    "int_accepted_eula_license_version_code"
  private const val INT_EULA_LICENSE_VERSION_CODE_NOT_ACCEPTED = Int.MIN_VALUE
  private const val INT_EULA_LICENSE_VERSION_CODE_LATEST = 1
  private var acceptedEulaLicenseVersionCode: Int
    get() = getInt(INT_ACCEPTED_EULA_LICENSE_VERSION_CODE)
      ?: INT_EULA_LICENSE_VERSION_CODE_NOT_ACCEPTED
    set(value) = setInt(INT_ACCEPTED_EULA_LICENSE_VERSION_CODE, value)

  fun setEulaLicenseAcceptedLatestToTrue() {
    acceptedEulaLicenseVersionCode = INT_EULA_LICENSE_VERSION_CODE_LATEST
  }

  fun getIfEulaLicenseAcceptedLatest(): Boolean {
    return acceptedEulaLicenseVersionCode == INT_EULA_LICENSE_VERSION_CODE_LATEST
  }

  fun setEulaLicenseWithdraw() {
    acceptedEulaLicenseVersionCode = INT_EULA_LICENSE_VERSION_CODE_NOT_ACCEPTED
  }

  //
  private const val INT_GIF_FINAL_DELAY = "int_gif_final_delay"
  private const val INT_GIF_FINAL_DELAY_DEF_VALUE = -1
  var gifFinalDelay: Int
    get() = getInt(INT_GIF_FINAL_DELAY) ?: INT_GIF_FINAL_DELAY_DEF_VALUE
    set(value) = setInt(INT_GIF_FINAL_DELAY, value)


  private const val INT_FILE_OPEN_WAY = "int_file_open_way"
  const val INT_FILE_OPEN_WAY_DOCUMENT = 1
  const val INT_FILE_OPEN_WAY_GALLERY = 2
  const val INT_FILE_OPEN_WAY_13 = 13
  private val INT_FILE_OPEN_WAY_DEFAULT =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) INT_FILE_OPEN_WAY_13 else INT_FILE_OPEN_WAY_GALLERY
  var fileOpenWay: Int
    get() = getInt(INT_FILE_OPEN_WAY) ?: INT_FILE_OPEN_WAY_DEFAULT
    set(value) = setInt(INT_FILE_OPEN_WAY, value)


  /*
    //
    private const val BOOLEAN_ANALYZE_VIDEO_SLOWLY = "boolean_analyze_video_slowly"
    private const val BOOLEAN_PREVIOUS_GIF_CONFIG_ANALYZE_VIDEO_SLOWLY_DEF_VALUE = false
    var previousGifConfigAnalyzeVideoSlowly: Boolean
      get() = getBoolean(BOOLEAN_ANALYZE_VIDEO_SLOWLY) ?: BOOLEAN_PREVIOUS_GIF_CONFIG_ANALYZE_VIDEO_SLOWLY_DEF_VALUE
      set(value) = setBoolean(BOOLEAN_ANALYZE_VIDEO_SLOWLY, value)
  */

  /*//
  private const val INT_PREVIOUS_GIF_CONFIG_SPEED = "int_previous_gif_config_speed"
  var previousGifConfigSpeed: Int?
    get() = getInt(INT_PREVIOUS_GIF_CONFIG_SPEED)
    set(value) = setInt(INT_PREVIOUS_GIF_CONFIG_SPEED, value)

  //
  private const val INT_PREVIOUS_GIF_CONFIG_RESOLUTION = "int_previous_gif_config_resolution"
  var previousGifConfigResolution: Int?
    get() = getInt(INT_PREVIOUS_GIF_CONFIG_RESOLUTION)
    set(value) = setInt(INT_PREVIOUS_GIF_CONFIG_RESOLUTION, value)

  //
  private const val INT_PREVIOUS_GIF_CONFIG_FRAME_RATE = "int_previous_gif_config_frame_rate"
  var previousGifConfigFrameRate: Int?
    get() = getInt(INT_PREVIOUS_GIF_CONFIG_FRAME_RATE)
    set(value) = setInt(INT_PREVIOUS_GIF_CONFIG_FRAME_RATE, value)

  //
  private const val INT_PREVIOUS_GIF_CONFIG_COLOR_QUALITY =
    "int_previous_gif_config_color_quality"
  var previousGifConfigColorQuality: Int?
    get() = getInt(INT_PREVIOUS_GIF_CONFIG_COLOR_QUALITY)
    set(value) = setInt(INT_PREVIOUS_GIF_CONFIG_COLOR_QUALITY, value)

  //
  private const val INT_PREVIOUS_GIF_CONFIG_LOSSY = "int_previous_gif_config_lossy"
  var previousGifConfigLossy: Int?
    get() = getInt(INT_PREVIOUS_GIF_CONFIG_LOSSY)
    set(value) = setInt(INT_PREVIOUS_GIF_CONFIG_LOSSY, value)

  //
  private const val FLOAT_PREVIOUS_ADD_TEXT_SIZE = "float_previous_add_text_size"
  var previousAddTextSize: Float?
    get() = getFloat(FLOAT_PREVIOUS_ADD_TEXT_SIZE)
    set(value) = setFloat(FLOAT_PREVIOUS_ADD_TEXT_SIZE, value)

  //
  private const val INT_PREVIOUS_ADD_TEXT_POSITION = "int_previous_add_text_position"
  var previousAddTextPosition: Int?
    get() = getInt(INT_PREVIOUS_ADD_TEXT_POSITION)
    set(value) = setInt(INT_PREVIOUS_ADD_TEXT_POSITION, value)
*/
  //
  private const val INT_CHANGELOG_HAS_BEEN_READ_APP_VERSION_CODE =
    "int_changelog_has_been_read_app_version_code"
  private const val INT_CHANGELOG_HAS_BEEN_READ_APP_VERSION_CODE_DEF_VALUE = Int.MIN_VALUE
  private var changelogHasBeenReadAppVersionCode: Int
    get() = getInt(INT_CHANGELOG_HAS_BEEN_READ_APP_VERSION_CODE)
      ?: INT_CHANGELOG_HAS_BEEN_READ_APP_VERSION_CODE_DEF_VALUE
    set(value) = setInt(INT_CHANGELOG_HAS_BEEN_READ_APP_VERSION_CODE, value)

  var changelogHasBeenReadInCurrentAppVersion: Boolean
    get() = (changelogHasBeenReadAppVersionCode == BuildConfig.VERSION_CODE)
    set(value) {
      changelogHasBeenReadAppVersionCode =
        if (value) BuildConfig.VERSION_CODE else INT_CHANGELOG_HAS_BEEN_READ_APP_VERSION_CODE_DEF_VALUE
    }

  /* //
   fun restoreAllUserOptionsToDefault() {
     previousGifConfigSpeed = null
     previousGifConfigResolution = null
     previousGifConfigFrameRate = null
     previousGifConfigColorQuality = null
     previousAddTextPosition = null
     previousAddTextSize = null
     gifFinalDelay = INT_GIF_FINAL_DELAY_DEF_VALUE
   }*/

  //
  private const val INT_NULL_VALUE = Int.MIN_VALUE
  private const val FLOAT_NULL_VALUE = Float.MIN_VALUE

  private fun getBoolean(key: String) =
    when (getInt(key)) {
      0 -> false
      1 -> true
      else -> null
    }

  private fun getInt(key: String) =
    defaultMMKV.decodeInt(key, INT_NULL_VALUE).toNullable()

  private fun getFloat(key: String) =
    defaultMMKV.decodeFloat(key, FLOAT_NULL_VALUE).toNullable()

  private fun getString(key: String) =
    defaultMMKV.decodeString(key)

  private fun setBoolean(key: String, value: Boolean?) {
    setInt(
      key,
      when (value) {
        false -> 0
        true -> 1
        null -> INT_NULL_VALUE
      }
    )
  }

  private fun setInt(key: String, value: Int?) {
    defaultMMKV.encode(key, value ?: INT_NULL_VALUE)
  }

  private fun setFloat(key: String, value: Float?) {
    defaultMMKV.encode(key, value ?: FLOAT_NULL_VALUE)
  }

  private fun setString(key: String, value: String?) {
    defaultMMKV.encode(key, value)
  }

  private fun Int.toNullable() = if (this == INT_NULL_VALUE) null else this

  private fun Float.toNullable() = if (this == FLOAT_NULL_VALUE) null else this
}