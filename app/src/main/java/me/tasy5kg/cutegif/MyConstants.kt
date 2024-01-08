package me.tasy5kg.cutegif

import android.os.Build
import android.view.HapticFeedbackConstants
import me.tasy5kg.cutegif.MyApplication.Companion.appContext
import me.tasy5kg.cutegif.toolbox.Toolbox.appGetString

object MyConstants {

  const val TASK_BUILDER_VIDEO_TO_GIF = "TASK_BUILDER_VIDEO_TO_GIF"
  private val CACHE_DIR_PATH = appContext.cacheDir.canonicalPath
  val SLIDESHOW_DIR_PATH = "$CACHE_DIR_PATH/slideshow"
  val PALETTE_PATH = "$CACHE_DIR_PATH/palette.png"
  val INPUT_FILE_DIR = "$CACHE_DIR_PATH/input_file_dir/"
  val ADD_TEXT_RENDER_PNG_PATH = "$CACHE_DIR_PATH/add_text.png"
  val IMAGE_FALLBACK_JPG_PATH = "$CACHE_DIR_PATH/fallback.jpg"
  val GET_VIDEO_SINGLE_FRAME_WITH_FFMPEG_TEMP_PATH = "$CACHE_DIR_PATH/get_video_single_frame_with_ffmpeg_temp_path.jpg"
  val OUTPUT_GIF_TEMP_PATH = "$CACHE_DIR_PATH/output_temp.gif"
  val OUTPUT_SPLIT_DIR = "$CACHE_DIR_PATH/split_dir/"
  val VIDEO_TO_GIF_EXPORT_OPTIONS_PREVIEW_DIR = "$CACHE_DIR_PATH/video_to_gif_export_options_preview_dir/"
  val VIDEO_TO_GIF_EXPORT_OPTIONS_PREVIEW_INPUT_FRAME_PATH = "${VIDEO_TO_GIF_EXPORT_OPTIONS_PREVIEW_DIR}input.png"

  /** resulting in an action being performed **/
  val HAPTIC_FEEDBACK_TYPE_CONFIRM =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.LONG_PRESS

  /** finished a gesture */
  val HAPTIC_FEEDBACK_TYPE_GESTURE_END =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.GESTURE_END else HapticFeedbackConstants.CONTEXT_CLICK

  /** toggling a switch */
  const val HAPTIC_FEEDBACK_TYPE_SWITCH_TOGGLING = HapticFeedbackConstants.VIRTUAL_KEY_RELEASE

  const val EXTRA_TEXT_RENDER = "EXTRA_TEXT_RENDER"
  const val EXTRA_VIDEO_WH = "EXTRA_VIDEO_WH"
  const val EXTRA_VIDEO_POSITION = "EXTRA_VIDEO_POSITION"
  const val EXTRA_CROP_PARAMS = "EXTRA_CROP_PARAMS"
  const val EXTRA_NEED_FOR_FALLBACK = "EXTRA_NEED_FOR_FALLBACK"
  const val EXTRA_CROP_PARAMS_DEFAULT = "EXTRA_CROP_PARAMS_DEFAULT"
  const val EXTRA_TRIM_TIME = "EXTRA_TRIM_TIME"
  const val EXTRA_VIDEO_URI = "EXTRA_VIDEO_URI"
  const val EXTRA_VIDEO_PATH = "EXTRA_VIDEO_PATH"
  const val EXTRA_GIF_URI = "EXTRA_GIF_URI"
  const val EXTRA_GIF_PATH = "EXTRA_GIF_PATH"
  const val EXTRA_STACK_TRACE_STRING = "EXTRA_STACK_TRACE_STRING"
  const val EXTRA_TASK_BUILDER = "EXTRA_TASK_BUILDER"
  const val EXTRA_IMAGES_CLIP_DATA = "EXTRA_IMAGES_CLIP_DATA"
  const val EXTRA_CHECKED_CROP_RATIO_TEXT = "EXTRA_CHECKED_CROP_RATIO_TEXT"
  const val EXTRA_ADD_TEXT_RENDER = "EXTRA_ADD_TEXT_RENDER"
  const val EXTRA_SAVED_FILE_URI = "EXTRA_SAVED_FILE_URI"
  const val EXTRA_EULA_DIALOG_ACCEPTED = "EXTRA_EULA_DIALOG_ACCEPTED"

  const val MIME_TYPE_VIDEO_ANY = "video/*"
  const val MIME_TYPE_IMAGE_ANY = "image/*"
  const val MIME_TYPE_IMAGE_GIF = "image/gif"
  const val MIME_TYPE_VIDEO_ = "video/"
  const val MIME_TYPE_VIDEO_MP4 = "video/mp4"
  const val MIME_TYPE_IMAGE_ = "image/"

  const val TASK_TYPE_VIDEO_TO_GIF = "VIDEO_TO_GIF"

  const val UNKNOWN_FLOAT = -99999f
  const val UNKNOWN_INT = -99999

  const val BLUR_BEHIND_RADIUS_24 = 24
  const val BLUR_BEHIND_RADIUS_48 = 48

  const val DOUBLE_BACK_TO_EXIT_DELAY = 2000L
  const val MATERIAL_TOOLBAR_SUBTITLE_TEMP_DISPLAY_DURATION = 3000L
  const val POPUP_WINDOW_ELEVATION = 8f
  const val CUSTOM_MENU_ITEM_VIEW_TYPE_GIF_CHIPS = 1
  const val CUSTOM_MENU_ITEM_VIEW_TYPE_GIF_SPEED = 2
  const val CUSTOM_MENU_ITEM_VIEW_TYPE_GIF_FINAL_DELAY = 3
  const val CUSTOM_MENU_ITEM_VIEW_TYPE_GIF_CHIPS_COLOR_QUALITY = 4

  const val URL_GET_LATEST_VERSION_HU60 = "https://hu60.cn/q.php/bbs.topic.103545.html?floor=0#0"
  const val URL_GET_LATEST_VERSION_GITHUB = "https://github.com/tasy5kg/CuteGIF/releases/latest"
  const val URL_BROWSE_HELP_DOCUMENTATION_ZH_CN_KDOCS = "https://pub.kdocs.cn/r/paGFePg24YDlAB4"
  const val URL_OPEN_SOURCE_REPO_GITHUB = "https://github.com/tasy5kg/CuteGIF"
  const val FFMPEG_COMMAND_PREFIX_FOR_ALL_AN = "-hwaccel auto -hide_banner -benchmark -an"
  const val FFMPEG_COMMAND_PREFIX_FOR_ALL = "-hwaccel auto -hide_banner -benchmark"
  const val URI_JOIN_QQ_GROUP =
    "mqqopensdkapi://bizAgent/qm/qr?url=http%3A%2F%2Fqm.qq.com%2Fcgi-bin%2Fqm%2Fqr%3Ffrom%3Dapp%26p%3Dandroid%26jump_from%3Dwebapi%26k%3D$5Frn_d8p0OxBX6NwtTAhAhDQh2FTCcI9"

  val GIF_RESOLUTION_MAP = linkedMapOf(
    appGetString(R.string._144p) to 144,
    appGetString(R.string._240p) to 240,
    appGetString(R.string._360p) to 360,
    appGetString(R.string._480p) to 480
  )

  val GIF_FRAME_RATE_MAP = linkedMapOf(
    "5帧" to 5,
    "10帧" to 10,
    "15帧" to 15,
    "30帧" to 30
  )

  val GIF_COLOR_QUALITY_MAP = linkedMapOf(
    "低" to 32,
    "中" to 128,
    "高" to 256
  )

  val GIF_LOSSY_MAP = linkedMapOf(
    "体积更小" to 200,
    "中等" to 70,
    "画质更好" to 30
  )

  val GIF_FINAL_DELAY_MAP = linkedMapOf(
    appGetString(R.string.no_pause) to -1,
    appGetString(R.string._0_1_s) to 10,
    appGetString(R.string._0_2_s) to 20,
    appGetString(R.string._0_3_s) to 30,
    appGetString(R.string._0_4_s) to 40,
    appGetString(R.string._0_5_s) to 50,
    appGetString(R.string._0_6_s) to 60,
    appGetString(R.string._0_7_s) to 70,
    appGetString(R.string._0_8_s) to 80,
    appGetString(R.string._0_9_s) to 90,
    appGetString(R.string._1_0_s) to 100,
    appGetString(R.string._1_1_s) to 110,
    appGetString(R.string._1_2_s) to 120,
    appGetString(R.string._1_3_s) to 130,
    appGetString(R.string._1_4_s) to 140,
    appGetString(R.string._1_5_s) to 150,
    appGetString(R.string._1_6_s) to 160,
    appGetString(R.string._1_7_s) to 170,
    appGetString(R.string._1_8_s) to 180,
    appGetString(R.string._1_9_s) to 190,
    appGetString(R.string._2_0_s) to 200
  )

  val GIF_FRAME_INTERVAL_MAP = linkedMapOf(
    appGetString(R.string._0_1_s) to 10,
    appGetString(R.string._0_2_s) to 20,
    appGetString(R.string._0_3_s) to 30,
    appGetString(R.string._0_4_s) to 40,
    appGetString(R.string._0_5_s) to 50,
    appGetString(R.string._0_6_s) to 60,
    appGetString(R.string._0_7_s) to 70,
    appGetString(R.string._0_8_s) to 80,
    appGetString(R.string._0_9_s) to 90,
    appGetString(R.string._1_0_s) to 100,
    appGetString(R.string._1_1_s) to 110,
    appGetString(R.string._1_2_s) to 120,
    appGetString(R.string._1_3_s) to 130,
    appGetString(R.string._1_4_s) to 140,
    appGetString(R.string._1_5_s) to 150,
    appGetString(R.string._1_6_s) to 160,
    appGetString(R.string._1_7_s) to 170,
    appGetString(R.string._1_8_s) to 180,
    appGetString(R.string._1_9_s) to 190,
    appGetString(R.string._2_0_s) to 200
  )

  val GIF_SPEED_MAP =
    linkedMapOf(
      appGetString(R.string.normal) to 100,
      appGetString(R.string._1_5_x) to 150,
      appGetString(R.string._2_x) to 200,
      appGetString(R.string._3_x) to 300,
      appGetString(R.string._4_x) to 400,
      appGetString(R.string._8_x) to 800,
      appGetString(R.string._16_x) to 1600
    )

  val ADD_TEXT_POSITION_MAP =
    linkedMapOf(
      appGetString(R.string.center) to 1,
      appGetString(R.string.corner) to 2
    )
}