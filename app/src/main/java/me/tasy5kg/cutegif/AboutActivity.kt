package me.tasy5kg.cutegif

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import me.tasy5kg.cutegif.Toolbox.onClick
import me.tasy5kg.cutegif.Toolbox.setupTextViewWithClickablePart
import me.tasy5kg.cutegif.Toolbox.toast
import me.tasy5kg.cutegif.databinding.ActivityAboutBinding

class AboutActivity : BaseActivity() {
  val binding by lazy { ActivityAboutBinding.inflate(layoutInflater) }
  override fun onCreateIfEulaAccepted(savedInstanceState: Bundle?) {
    setContentView(binding.root)
    setFinishOnTouchOutside(true)
    val mbChooseFileWayDocument by lazy { binding.mbChooseFileWayDocument }
    val mbChooseFileWayGallery by lazy { binding.mbChooseFileWayGallery }
    val mbChooseFileWay13 by lazy { binding.mbChooseFileWay13 }
    val mbtgChooseFileWay by lazy { binding.mbtgChooseFileWay }
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
      mbChooseFileWay13.visibility = View.GONE
    }
    mbtgChooseFileWay.apply {
      check(
        when (MySettings.fileOpenWay) {
          MySettings.INT_FILE_OPEN_WAY_DOCUMENT -> mbChooseFileWayDocument.id
          MySettings.INT_FILE_OPEN_WAY_GALLERY -> mbChooseFileWayGallery.id
          MySettings.INT_FILE_OPEN_WAY_13 -> mbChooseFileWay13.id
          else -> throw IllegalArgumentException()
        }
      )
      addOnButtonCheckedListener { group, checkedId, isChecked ->
        if (isChecked) {
          MySettings.fileOpenWay = when (checkedId) {
            mbChooseFileWayDocument.id -> MySettings.INT_FILE_OPEN_WAY_DOCUMENT
            mbChooseFileWayGallery.id -> MySettings.INT_FILE_OPEN_WAY_GALLERY
            mbChooseFileWay13.id -> MySettings.INT_FILE_OPEN_WAY_13
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
      "用户协议 | 隐私政策 | 开源许可",
      listOf("用户协议" to { toast("...") },
        "隐私政策" to { toast("...") },
        "开源许可" to { toast("...") }),
      true
    )
    binding.mtvVersionInfo.text = getString(R.string.version_X, BuildConfig.VERSION_NAME)
    binding.mtvJoinQqGroup.onClick {
      Toolbox.cmivJoinQqGroupLambda(this@AboutActivity)
    }
    binding.mbDonateAlipay.onClick {
      toast("我做得还不够好...")
    }
  }

  companion object {
    fun start(context: Context) {
      context.startActivity(Intent(context, AboutActivity::class.java))
    }
  }
}