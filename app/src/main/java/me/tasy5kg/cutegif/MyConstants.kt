package me.tasy5kg.cutegif

import android.graphics.Color
import me.tasy5kg.cutegif.MyApplication.Companion.context

object MyConstants {
  val PALETTE_PATH = context.externalCacheDir!!.canonicalPath + "/palette.png"
  val FIRST_FRAME_PATH = context.externalCacheDir!!.canonicalPath + "/first_frame.jpg"
  val THUMBNAIL_PATH = context.externalCacheDir!!.canonicalPath + "/thumbnail.jpg"
  val TRANSPARENT_COLOR = Color.parseColor("#00000000")
  const val EXTRA_CROP_PARAMS = "EXTRA_CROP_PARAMS"
  const val EXTRA_CROP_PARAMS_DEFAULT = "EXTRA_CROP_PARAMS_DEFAULT"
  const val EXTRA_TRIM_START = "EXTRA_TRIM_START"
  const val EXTRA_TRIM_END = "EXTRA_TRIM_END"
  const val EXTRA_VIDEO_URI = "EXTRA_VIDEO_URI"
  const val EXTRA_CHECKED_CROP_RATIO_TEXT = "EXTRA_CHECKED_CROP_RATIO_TEXT"
  const val UNKNOWN_FLOAT = -99999f
  const val UNKNOWN_INT = -99999
  const val DOUBLE_BACK_TO_EXIT_DELAY = 2000L
  const val MATERIAL_TOOLBAR_SUBTITLE_TEMP_DISPLAY_DURATION = 3000L
  const val POPUP_WINDOW_ELEVATION = 8f
  const val CUSTOM_MENU_ITEM_VIEW_TYPE_GIF_CHIPS = 1
  const val CUSTOM_MENU_ITEM_VIEW_TYPE_GIF_SPEED = 2
  const val CUSTOM_MENU_ITEM_VIEW_TYPE_GIF_FINAL_DELAY = 3
  const val URL_GET_LATEST_VERSION_HU60 = "https://hu60.cn/q.php/bbs.topic.103545.html?floor=0#0"
  const val URL_GET_LATEST_VERSION_GITHUB = "https://github.com/tasy5kg/CuteGIF/releases/latest"
  const val URL_BROWSE_HELP_DOCUMENTATION_ZH_CN_KDOCS = "https://pub.kdocs.cn/r/paGFePg24YDlAB4"
  const val URL_OPEN_SOURCE_REPO_GITHUB = "https://github.com/tasy5kg/CuteGIF"
  const val FFMPEG_COMMAND_FOR_ALL = "-hwaccel auto -hide_banner -benchmark -an"
  const val URI_JOIN_QQ_GROUP =
    "mqqopensdkapi://bizAgent/qm/qr?url=http%3A%2F%2Fqm.qq.com%2Fcgi-bin%2Fqm%2Fqr%3Ffrom%3Dapp%26p%3Dandroid%26jump_from%3Dwebapi%26k%3D$5Frn_d8p0OxBX6NwtTAhAhDQh2FTCcI9"

  val GIF_RESOLUTION_MAP = linkedMapOf(
    context.getString(R.string._144p) to 144,
    context.getString(R.string._240p) to 240,
    context.getString(R.string._360p) to 360,
    context.getString(R.string._480p) to 480
  )

  val GIF_FRAME_RATE_MAP = linkedMapOf(
    context.getString(R.string.low) to 6,
    context.getString(R.string.medium) to 10,
    context.getString(R.string.high) to 15,
    context.getString(R.string._super) to 30
  )

  val GIF_COLOR_QUALITY_MAP = linkedMapOf(
    context.getString(R.string.distortion) to 32,
    context.getString(R.string.low) to 64,
    context.getString(R.string.medium) to 128,
    context.getString(R.string.high) to 192,
    context.getString(R.string.max) to 256
  )

  val GIF_FINAL_DELAY_MAP = linkedMapOf(
    context.getString(R.string.no_pause) to -1,
    context.getString(R.string._0_1_s) to 10,
    context.getString(R.string._0_2_s) to 20,
    context.getString(R.string._0_3_s) to 30,
    context.getString(R.string._0_4_s) to 40,
    context.getString(R.string._0_5_s) to 50,
    context.getString(R.string._0_6_s) to 60,
    context.getString(R.string._0_7_s) to 70,
    context.getString(R.string._0_8_s) to 80,
    context.getString(R.string._0_9_s) to 90,
    context.getString(R.string._1_0_s) to 100,
    context.getString(R.string._1_1_s) to 110,
    context.getString(R.string._1_2_s) to 120,
    context.getString(R.string._1_3_s) to 130,
    context.getString(R.string._1_4_s) to 140,
    context.getString(R.string._1_5_s) to 150,
    context.getString(R.string._1_6_s) to 160,
    context.getString(R.string._1_7_s) to 170,
    context.getString(R.string._1_8_s) to 180,
    context.getString(R.string._1_9_s) to 190,
    context.getString(R.string._2_0_s) to 200
  )

  const val GIF_SPEED_GLANCE_MODE = 99900100

  val GIF_SPEED_MAP =
    linkedMapOf(
      context.getString(R.string.normal) to 100,
      context.getString(R.string._1_5_x) to 150,
      context.getString(R.string._2_x) to 200,
      context.getString(R.string._3_x) to 300,
      context.getString(R.string._4_x) to 400,
      context.getString(R.string._8_x) to 800,
      context.getString(R.string.glance_mode) to GIF_SPEED_GLANCE_MODE
    )
}