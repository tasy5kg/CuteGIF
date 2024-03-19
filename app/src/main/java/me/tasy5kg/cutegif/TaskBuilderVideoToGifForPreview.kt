package me.tasy5kg.cutegif

import me.tasy5kg.cutegif.toolbox.Toolbox.toEmptyStringIf
import java.io.Serializable

data class TaskBuilderVideoToGifForPreview(
  val shortLength: Int,
  val colorQuality: Int,
  val lossy: Int?,
  val videoWH: Pair<Int, Int>,
  val colorKey: Pair<String, Int>?
) : Serializable {
  fun getCache_shortLength() = "${MyConstants.VIDEO_TO_GIF_PREVIEW_CACHE_DIR}${shortLength}.png"
  fun getCache_shortLength_colorKey() = getCache_shortLength() + (".${colorKey.toString().replace(" ", "")}.png").toEmptyStringIf { colorKey == null }
  fun getCache_shortLength_colorKey_palettegen() = "${getCache_shortLength_colorKey()}.${colorQuality}.png"
  fun getCache_shortLength_colorKey_paletteuse() = "${getCache_shortLength_colorKey()}.${colorQuality}.gif"
  fun getCache_shortLength_colorKey_paletteuse_lossy() = getCache_shortLength_colorKey_paletteuse() + (".$lossy.gif").toEmptyStringIf { lossy == null }
}