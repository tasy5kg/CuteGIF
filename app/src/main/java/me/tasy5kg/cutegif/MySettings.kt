package me.tasy5kg.cutegif

import android.content.Context

object MySettings {
  private val PREFERENCES_SETTINGS = MyApplication.context.getSharedPreferences("settings", Context.MODE_PRIVATE)!!
  private const val INT_PREVIOUS_VERSION = "previous_version"
  const val INT_REMEMBER_GIF_CONFIG = "remember_gif_config"
  const val INT_PREVIOUS_GIF_CONFIG_SPEED = "previous_gif_config_speed"
  const val INT_PREVIOUS_GIF_CONFIG_RESOLUTION = "previous_gif_config_resolution"
  const val INT_PREVIOUS_GIF_CONFIG_FRAME_RATE = "previous_gif_config_frame_rate"
  const val INT_PREVIOUS_GIF_CONFIG_COLOR_QUALITY = "previous_gif_config_color_quality"

  fun getBoolean(key: String, defValue: Boolean) = PREFERENCES_SETTINGS.getBoolean(key, defValue)

  fun setBoolean(key: String, value: Boolean) {
    PREFERENCES_SETTINGS.edit().apply {
      putBoolean(key, value)
      apply()
    }
  }

  fun getInt(key: String, defValue: Int) = PREFERENCES_SETTINGS.getInt(key, defValue)

  fun setInt(key: String, value: Int) {
    PREFERENCES_SETTINGS.edit().apply {
      putInt(key, value)
      apply()
    }
  }

  fun firstStartCurrentVersion() = getInt(INT_PREVIOUS_VERSION, -1) < BuildConfig.VERSION_CODE

  fun setPreviousVersionToCurrent() = setInt(INT_PREVIOUS_VERSION, BuildConfig.VERSION_CODE)
}