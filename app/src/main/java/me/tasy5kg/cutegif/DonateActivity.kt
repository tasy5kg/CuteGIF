package me.tasy5kg.cutegif

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import me.tasy5kg.cutegif.databinding.ActivityDonateBinding
import me.tasy5kg.cutegif.toolbox.FileTools.copyToWithClose
import me.tasy5kg.cutegif.toolbox.FileTools.createNewFile
import me.tasy5kg.cutegif.toolbox.Toolbox.onClick
import me.tasy5kg.cutegif.toolbox.Toolbox.openWeChatQrScanner
import me.tasy5kg.cutegif.toolbox.Toolbox.toast

class DonateActivity : AppCompatActivity() {
  private val binding by lazy { ActivityDonateBinding.inflate(layoutInflater) }
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setFinishOnTouchOutside(true)
    setContentView(binding.root)
    binding.mbClose.onClick { finish() }
    binding.mbBack.onClick { finish() }
    binding.mbStartDonating.onClick {
      resources.openRawResource(R.raw.donate_wechat)
        .copyToWithClose(contentResolver.openOutputStream(createNewFile("donate_wechat", "png"))!!)
      toast(
        when (openWeChatQrScanner()) {
          true -> "捐赠二维码已保存，请扫描相册内的第一张图片"
          false -> "微信扫一扫跳转失败，捐赠二维码已保存"
        }
      )
    }
  }

  companion object {
    fun start(context: Context) {
      context.startActivity(Intent(context, DonateActivity::class.java))
    }
  }
}