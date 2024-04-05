package me.tasy5kg.cutegif

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import me.tasy5kg.cutegif.databinding.ActivityFollowWechatBinding
import me.tasy5kg.cutegif.toolbox.FileTools
import me.tasy5kg.cutegif.toolbox.Toolbox
import me.tasy5kg.cutegif.toolbox.Toolbox.onClick
import me.tasy5kg.cutegif.toolbox.Toolbox.openWeChatQrScanner

class FollowWechatActivity : AppCompatActivity() {
  private val binding by lazy { ActivityFollowWechatBinding.inflate(layoutInflater) }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setFinishOnTouchOutside(true)
    setContentView(binding.root)
    binding.mbClose.onClick { finish() }
    binding.mbBack.onClick { finish() }
    binding.mbStartFollowing.onClick {
      resources.openRawResource(R.raw.follow_wechat).use { wechatQrCodeImg ->
        contentResolver.openOutputStream(FileTools.createNewFile("follow_wechat", "png"))!!.use { dest ->
          wechatQrCodeImg.copyTo(dest)
        }
      }
      Toolbox.toast(if (openWeChatQrScanner()) R.string.wechat_follow_qrcode_saved_please_scan_first_image else R.string.wechat_follow_qrcode_saved_wechat_launch_failed)
    }
  }

  companion object {
    fun start(context: Context) {
      context.startActivity(Intent(context, FollowWechatActivity::class.java))
    }
  }

}