package me.tasy5kg.cutegif

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import me.tasy5kg.cutegif.databinding.ActivityEulaBinding
import me.tasy5kg.cutegif.toolbox.Toolbox.onClick

class EulaActivity : AppCompatActivity() {
  private val binding by lazy { ActivityEulaBinding.inflate(layoutInflater) }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(binding.root)
    binding.mtvVersionInfo.text = getString(R.string.version_X, BuildConfig.VERSION_NAME)
    if (MySettings.eulaAccepted) {
      binding.mbDisagree.visibility = View.GONE
      binding.mbAgree.text = "关闭"
      binding.mtvPleaseAccept.text = "您已经阅读并同意了以上协议。要撤回同意，请卸载本软件。"
      binding.mbAgree.onClick(HapticFeedbackType.CONFIRM) {
        finish()
      }
    } else {
      binding.mbAgree.onClick(HapticFeedbackType.CONFIRM) {
        MySettings.eulaAccepted = true
        MainActivity.start(this@EulaActivity)
        finish()
      }
    }
    binding.mbDisagree.onClick {
      finish()
    }


  }


  companion object {
    fun start(context: Context) = context.startActivity(Intent(context, EulaActivity::class.java))
  }
}