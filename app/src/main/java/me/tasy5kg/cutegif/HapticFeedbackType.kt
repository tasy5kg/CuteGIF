package me.tasy5kg.cutegif

import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES
import android.view.HapticFeedbackConstants

object HapticFeedbackType {
  /** resulting in an action being performed **/
  val CONFIRM = if (SDK_INT >= VERSION_CODES.R) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.LONG_PRESS

  /** finished a gesture */
  val GESTURE_END =
    if (SDK_INT >= VERSION_CODES.R) HapticFeedbackConstants.GESTURE_END else HapticFeedbackConstants.CONTEXT_CLICK

  /** toggling a slider */
  const val SWITCH_TOGGLING = HapticFeedbackConstants.KEYBOARD_TAP
}