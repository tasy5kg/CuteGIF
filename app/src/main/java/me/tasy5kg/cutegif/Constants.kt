package me.tasy5kg.cutegif

import android.content.Context
import me.tasy5kg.cutegif.MyApplication.Companion.context

object Constants {
    val PALETTE_PATH = context.externalCacheDir!!.canonicalPath + "/palette.png"
    val THUMBNAIL_PATH = context.externalCacheDir!!.canonicalPath + "/thumbnail.gif"
    val SHARED_PREFERENCES_SETTING = context.getSharedPreferences("settings", Context.MODE_PRIVATE)!!

    //  val URI_EMAIL_TO_TASY5KG = "mailto:" + context.getString(R.string.email_address_tasy5kg)

    const val SHARED_PREFERENCES_KEY_IS_FIRST_START = "is_first_start"
    const val DOUBLE_BACK_TO_EXIT_DELAY = 2000L
    const val POPUP_WINDOW_ELEVATION = 8f
    const val QQ_GROUP_ID = 729757909
    const val URL_GET_LATEST_VERSION_HU60 = "https://hu60.cn/q.php/bbs.topic.103545.html?floor=0#0"
    const val URL_GET_LATEST_VERSION_GITHUB = "https://github.com/tasy5kg/CuteGIF/releases/latest"
    const val URL_BROWSE_HELP_DOCUMENTATION_ZH_CN_KDOCS = "https://pub.kdocs.cn/r/paGFePg24YDlAB4"
    const val URL_OPEN_SOURCE_REPO_GITHUB = "https://github.com/tasy5kg/CuteGIF"

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

    val GIF_SPEED_MAP =
        linkedMapOf(
            context.getString(R.string.normal) to 100,
            context.getString(R.string._1_5_x) to 150,
            context.getString(R.string._2_x) to 200,
            context.getString(R.string._3_x) to 300,
            context.getString(R.string._4_x) to 400,
            context.getString(R.string._8_x) to 800,
            context.getString(R.string._16_x) to 1600,
            context.getString(R.string._32_x) to 3200
        )
}