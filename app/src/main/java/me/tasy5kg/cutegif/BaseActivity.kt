package me.tasy5kg.cutegif

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import kotlin.system.exitProcess

abstract class BaseActivity() : AppCompatActivity() {

  abstract fun onCreateIfEulaAccepted(savedInstanceState: Bundle?)

  final override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    //  if (!MySettings.getIfEulaLicenseAcceptedLatest()) {
    if (false) {//TODO
      /** Ask user to accept EULA */
      EulaActivity.start(this)
      finish()
    } else {
      if (BuildConfig.DEBUG) {
        /** Show a dialog with logs when app crashed */
        Thread.setDefaultUncaughtExceptionHandler { _, e ->
          AppCrashedActivity.start(this, e.stackTraceToString())
          exitProcess(1)
        }
      }
      window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
      onCreateIfEulaAccepted(savedInstanceState)
    }
  }
}