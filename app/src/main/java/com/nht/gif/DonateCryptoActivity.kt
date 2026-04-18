package com.nht.gif

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.nht.gif.databinding.ActivityDonateCryptoBinding
import com.nht.gif.toolbox.FileTools
import com.nht.gif.toolbox.Toolbox
import com.nht.gif.toolbox.Toolbox.onClick

class DonateCryptoActivity : AppCompatActivity() {
  private val binding by lazy { ActivityDonateCryptoBinding.inflate(layoutInflater) }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setFinishOnTouchOutside(true)
    setContentView(binding.root)
    binding.mbClose.onClick { finish() }
    binding.mbBack.onClick { finish() }
    binding.mbStartFollowing.onClick {
      resources.openRawResource(R.raw.donate_erc20_address).use { erc20QrCodeImg ->
        contentResolver.openOutputStream(FileTools.createNewFile("donate_erc20", "png"))!!.use { dest ->
          erc20QrCodeImg.copyTo(dest)
        }
      }
      Toolbox.toast(R.string.donation_erc20_qrcode_saved_please_scan_first_image)
    }
  }

  companion object {
    fun start(context: Context) {
      context.startActivity(Intent(context, DonateCryptoActivity::class.java))
    }
  }

}