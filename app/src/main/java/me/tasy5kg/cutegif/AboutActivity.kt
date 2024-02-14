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
      addOnButtonCheckedListener { _, checkedId, isChecked ->
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
    binding.mbDone.onClick {
      finish()
    }
    binding.mtvLegalInfo.setupTextViewWithClickablePart(
      "${getString(R.string.whats_new)} | ${getString(R.string.eula_and_privacy_policy)} | ${getString(R.string.open_source_license)}", listOf(
        getString(R.string.whats_new) to { WhatsNewActivity.start(this) },
        getString(R.string.eula_and_privacy_policy) to { EulaActivity.start(this) },
        getString(R.string.open_source_license) to { OpenSourceLicenseActivity.start(this) }
      ), true
    )
    binding.mtvVersionInfo.text = getString(R.string.version_X, BuildConfig.VERSION_NAME)
  }

  companion object {
    fun start(context: Context) {
      context.startActivity(Intent(context, AboutActivity::class.java))
    }
  }
}