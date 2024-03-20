package me.tasy5kg.cutegif

import android.app.Activity

object ActivityCollector {
  private val activities = ArrayList<Activity>()

  fun addActivity(activity: Activity) {
    activities.add(activity)
  }

  fun removeActivity(activity: Activity) {
    activities.remove(activity)
  }

  fun isEmpty() = activities.isEmpty()
}