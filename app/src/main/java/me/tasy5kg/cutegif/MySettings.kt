package me.tasy5kg.cutegif

import android.content.Context

object MySettings {
  private val PREFERENCES_SETTINGS = MyApplication.context.getSharedPreferences("settings", Context.MODE_PRIVATE)!!
  private const val INT_PREVIOUS_VERSION = "int_previous_version"
  private const val BOOLEAN_REMEMBER_GIF_OPTIONS = "boolean_remember_gif_options"
  private const val BOOLEAN_ANALYZE_VIDEO_SLOWLY = "boolean_analyze_video_slowly"
  private const val BOOLEAN_ALWAYS_SHOW_MORE_OPTIONS_WHEN_CONVERTING_GIF = "boolean_always_show_more_options_when_converting_gif"
  private const val INT_GIF_FINAL_DELAY = "int_gif_final_delay"
  private const val INT_PREVIOUS_GIF_CONFIG_SPEED = "int_previous_gif_config_speed"
  private const val INT_PREVIOUS_GIF_CONFIG_RESOLUTION = "int_previous_gif_config_resolution"
  private const val INT_PREVIOUS_GIF_CONFIG_FRAME_RATE = "int_previous_gif_config_frame_rate"
  private const val INT_PREVIOUS_GIF_CONFIG_COLOR_QUALITY = "int_previous_gif_config_color_quality"
  private const val BOOLEAN_ALWAYS_SHOW_MORE_OPTIONS_WHEN_CONVERTING_GIF_DEFAULT = false
  private const val BOOLEAN_REMEMBER_GIF_OPTIONS_DEFAULT = false
  private const val BOOLEAN_ANALYZE_VIDEO_SLOWLY_DEFAULT = false
  private const val INT_GIF_FINAL_DELAY_DEFAULT = -1
  private const val INT_PREVIOUS_VERSION_NEW_INSTALL = -1
  const val INT_PREVIOUS_GIF_CONFIG_UNKNOWN_VALUE = -99999

  private var previousVersion
    get() = get(INT_PREVIOUS_VERSION, INT_PREVIOUS_VERSION_NEW_INSTALL)
    set(value) = set(INT_PREVIOUS_VERSION, value)

  var rememberGifOptions
    get() = get(BOOLEAN_REMEMBER_GIF_OPTIONS, BOOLEAN_REMEMBER_GIF_OPTIONS_DEFAULT)
    set(value) = set(BOOLEAN_REMEMBER_GIF_OPTIONS, value)

  var analyzeVideoSlowly
    get() = get(BOOLEAN_ANALYZE_VIDEO_SLOWLY, BOOLEAN_ANALYZE_VIDEO_SLOWLY_DEFAULT)
    set(value) = set(BOOLEAN_ANALYZE_VIDEO_SLOWLY, value)

  var alwaysShowMoreOptionsWhenConvertingGif
    get() = get(BOOLEAN_ALWAYS_SHOW_MORE_OPTIONS_WHEN_CONVERTING_GIF, BOOLEAN_ALWAYS_SHOW_MORE_OPTIONS_WHEN_CONVERTING_GIF_DEFAULT)
    set(value) = set(BOOLEAN_ALWAYS_SHOW_MORE_OPTIONS_WHEN_CONVERTING_GIF, value)

  var gifFinalDelay
    get() = get(INT_GIF_FINAL_DELAY, INT_GIF_FINAL_DELAY_DEFAULT)
    set(value) = set(INT_GIF_FINAL_DELAY, value)

  var previousGifConfigSpeed
    get() = get(INT_PREVIOUS_GIF_CONFIG_SPEED, INT_PREVIOUS_GIF_CONFIG_UNKNOWN_VALUE)
    set(value) = set(INT_PREVIOUS_GIF_CONFIG_SPEED, value)

  var previousGifConfigResolution
    get() = get(INT_PREVIOUS_GIF_CONFIG_RESOLUTION, INT_PREVIOUS_GIF_CONFIG_UNKNOWN_VALUE)
    set(value) = set(INT_PREVIOUS_GIF_CONFIG_RESOLUTION, value)

  var previousGifConfigFrameRate
    get() = get(INT_PREVIOUS_GIF_CONFIG_FRAME_RATE, INT_PREVIOUS_GIF_CONFIG_UNKNOWN_VALUE)
    set(value) = set(INT_PREVIOUS_GIF_CONFIG_FRAME_RATE, value)

  var previousGifConfigColorQuality
    get() = get(INT_PREVIOUS_GIF_CONFIG_COLOR_QUALITY, INT_PREVIOUS_GIF_CONFIG_UNKNOWN_VALUE)
    set(value) = set(INT_PREVIOUS_GIF_CONFIG_COLOR_QUALITY, value)

  @Suppress("UNCHECKED_CAST")
  fun <T> get(key: String, defValue: T) =
    when (defValue) {
      is Boolean -> PREFERENCES_SETTINGS.getBoolean(key, defValue)
      is Int -> PREFERENCES_SETTINGS.getInt(key, defValue)
      else -> throw IllegalArgumentException("key = $key, defValue = $defValue")
    } as T

  fun <T> set(key: String, value: T) {
    PREFERENCES_SETTINGS.edit().apply {
      when (value) {
        is Boolean -> putBoolean(key, value)
        is Int -> putInt(key, value)
        else -> throw IllegalArgumentException("key = $key, value = $value")
      }
      apply()
    }
  }

  fun firstStartCurrentVersion() = previousVersion < BuildConfig.VERSION_CODE

  fun setPreviousVersionToCurrent() {
    previousVersion = BuildConfig.VERSION_CODE
  }
}