package me.tasy5kg.cutegif

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import me.tasy5kg.cutegif.Toolbox.onClick
import me.tasy5kg.cutegif.Toolbox.openWeChatQrScanner
import me.tasy5kg.cutegif.databinding.ActivityDonateBinding

class DonateActivity : AppCompatActivity() {
  private val binding by lazy { ActivityDonateBinding.inflate(layoutInflater) }
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setFinishOnTouchOutside(true)
    setContentView(binding.root)
    binding.mbClose.onClick { finish() }
    binding.mbBack.onClick { finish() }
    binding.mbStartDonating.onClick {
      val donateWechatInputStream = resources.openRawResource(R.raw.donate_wechat)
      val donateWechatOutputStream = contentResolver.openOutputStream(Toolbox.createNewFile("donate_wechat", "png"))!!
      donateWechatInputStream.copyTo(donateWechatOutputStream)
      donateWechatInputStream.close()
      donateWechatOutputStream.close()
      openWeChatQrScanner()
    }
  }

  companion object {
    fun start(context: Context) {
      context.startActivity(Intent(context, DonateActivity::class.java))
    }
  }
}