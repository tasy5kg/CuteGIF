package me.tasy5kg.cutegif

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import me.tasy5kg.cutegif.databinding.ActivityDonateBinding
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
      resources.openRawResource(R.raw.donate_wechat).use { wechatQrCodeImg ->
        contentResolver.openOutputStream(createNewFile("donate_wechat", "png"))!!.use { dest ->
          wechatQrCodeImg.copyTo(dest)
        }
      }
      toast(if (openWeChatQrScanner()) getString(R.string.wechat_donate_qrcode_saved_please_scan_first_image) else getString(R.string.wechat_donate_qrcode_saved_wechat_launch_failed))
    }
  }

  companion object {
    fun start(context: Context) {
      context.startActivity(Intent(context, DonateActivity::class.java))
    }
  }
}