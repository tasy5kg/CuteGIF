package me.tasy5kg.cutegif

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import me.tasy5kg.cutegif.databinding.ActivityAboutBinding
import me.tasy5kg.cutegif.toolbox.Toolbox.onClick
import me.tasy5kg.cutegif.toolbox.Toolbox.setupTextViewWithClickablePart

class AboutActivity : BaseActivity() {
  private val binding by lazy { ActivityAboutBinding.inflate(layoutInflater) }

  override fun onCreateIfEulaAccepted(savedInstanceState: Bundle?) {
    setContentView(binding.root)
    setFinishOnTouchOutside(true)
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
      binding.mbChooseFileWay13.visibility = View.GONE
    }
    binding.mbtgChooseFileWay.apply {
      check(
        when (MySettings.fileOpenWay) {
          MySettings.INT_FILE_OPEN_WAY_DOCUMENT -> binding.mbChooseFileWayDocument.id
          MySettings.INT_FILE_OPEN_WAY_GALLERY -> binding.mbChooseFileWayGallery.id
          MySettings.INT_FILE_OPEN_WAY_13 -> binding.mbChooseFileWay13.id
          else -> throw IllegalArgumentException()
        }
      )
      addOnButtonCheckedListener { group, checkedId, isChecked ->
        performHapticFeedback(HapticFeedbackType.SWITCH_TOGGLING)
        if (isChecked) {
          MySettings.fileOpenWay = when (checkedId) {
            binding.mbChooseFileWayDocument.id -> MySettings.INT_FILE_OPEN_WAY_DOCUMENT
            binding.mbChooseFileWayGallery.id -> MySettings.INT_FILE_OPEN_WAY_GALLERY
            binding.mbChooseFileWay13.id -> MySettings.INT_FILE_OPEN_WAY_13
            else -> throw IllegalArgumentException()
          }
        }
      }
    }
// TODO
    binding.mbDone.onClick {
      finish()
    }
    binding.mtvLegalInfo.setupTextViewWithClickablePart(
      "新功能介绍 | 用户协议与隐私政策 | 开源许可", listOf(
        "新功能介绍" to { WhatsNewActivity.start(this) },
        "用户协议与隐私政策" to { EulaActivity.start(this) },
        "开源许可" to { OpenSourceLicenseActivity.start(this) }
      ), true
    )/*  binding.mtvLegalInfo.setupTextViewWithClickablePart(
      "用户协议 | 隐私政策 | 开源许可",
      listOf("用户协议" to { toast("...") },
        "隐私政策" to { toast("...") },
        "开源许可" to { startActivity(Intent(this, OssLicensesMenuActivity::class.java)) }),
      true
    )*/
    binding.mtvVersionInfo.text = getString(R.string.version_X, BuildConfig.VERSION_NAME)
    /*binding.mtvJoinQqGroup.onClick {
      Toolbox.cmivJoinQqGroupLambda(this@AboutActivity)
    }*/
  }

  companion object {
    fun start(context: Context) {
      context.startActivity(Intent(context, AboutActivity::class.java))
    }
  }
}