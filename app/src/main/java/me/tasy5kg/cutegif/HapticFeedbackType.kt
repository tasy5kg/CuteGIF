package me.tasy5kg.cutegif

import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES
import android.view.HapticFeedbackConstants

enum class HapticFeedbackType(val value: Int) {
  /** resulting in an action being performed **/
  CONFIRM(if (SDK_INT >= VERSION_CODES.R) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.LONG_PRESS),

  /** resulting in an action being performed **/
  REJECT(HapticFeedbackConstants.REJECT),

  /** finished a gesture */
  GESTURE_END(if (SDK_INT >= VERSION_CODES.R) HapticFeedbackConstants.GESTURE_END else HapticFeedbackConstants.CONTEXT_CLICK),

  /** toggling a slider */
  SWITCH_TOGGLING(if (SDK_INT >= VERSION_CODES.O_MR1) HapticFeedbackConstants.KEYBOARD_TAP else HapticFeedbackConstants.CONTEXT_CLICK)
}