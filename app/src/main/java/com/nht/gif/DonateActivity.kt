package com.nht.gif

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.nht.gif.databinding.ActivityDonateBinding
import com.nht.gif.toolbox.FileTools.createNewFile
import com.nht.gif.toolbox.Toolbox.onClick
import com.nht.gif.toolbox.Toolbox.openWeChatQrScanner
import com.nht.gif.toolbox.Toolbox.toast

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
      toast(if (openWeChatQrScanner()) R.string.wechat_donate_qrcode_saved_please_scan_first_image else R.string.wechat_donate_qrcode_saved_wechat_launch_failed)
    }
  }

  companion object {
    fun start(context: Context) {
      context.startActivity(Intent(context, DonateActivity::class.java))
    }
  }
}