package com.nht.gif

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import com.nht.gif.toolbox.NotificationHelper

class MyApplication : Application() {
  override fun onCreate() {
    super.onCreate()
    appContext = applicationContext
    NotificationHelper.createChannel(this)
  }

  companion object {
    @SuppressLint("StaticFieldLeak")
    lateinit var appContext: Context
  }
}
