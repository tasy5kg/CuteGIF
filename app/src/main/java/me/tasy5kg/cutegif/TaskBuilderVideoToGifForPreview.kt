package me.tasy5kg.cutegif

import me.tasy5kg.cutegif.toolbox.Toolbox.toEmptyStringIf
import java.io.Serializable

data class TaskBuilderVideoToGifForPreview(
  val shortLength: Int,
  val colorQuality: Int,
  val lossy: Int?,
  val videoWH: Pair<Int, Int>,
  val colorKey: Triple<String, Int, Int>?
) : Serializable {

  val cache_shortLength = "${MyConstants.VIDEO_TO_GIF_PREVIEW_CACHE_DIR}${shortLength}.png"
  val cache_shortLength_colorKey = cache_shortLength + (".${
    colorKey.toString().replace(" ", "")
  }.png").toEmptyStringIf { colorKey == null }
  val cache_shortLength_colorKey_palettegen = "$cache_shortLength_colorKey.${colorQuality}.png"
  val cache_shortLength_colorKey_paletteuse = "$cache_shortLength_colorKey.${colorQuality}.gif"
  val cache_shortLength_colorKey_paletteuse_lossy =
    cache_shortLength_colorKey_paletteuse + (".$lossy.gif").toEmptyStringIf { lossy == null }
}