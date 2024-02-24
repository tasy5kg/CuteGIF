package me.tasy5kg.cutegif.toolbox

import android.annotation.SuppressLint
import android.app.Activity
import android.content.*
import android.content.res.ColorStateList
import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.icu.text.SimpleDateFormat
import android.net.Uri
import android.text.SpannableString
import android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.Log
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.ColorInt
import androidx.annotation.StringRes
import androidx.core.app.ActivityOptionsCompat
import androidx.core.graphics.ColorUtils
import androidx.core.view.ContentInfoCompat
import androidx.core.view.OnReceiveContentListener
import androidx.draganddrop.DropHelper.Options
import androidx.draganddrop.DropHelper.configureView
import androidx.lifecycle.LifecycleEventObserver
import com.google.android.material.slider.RangeSlider
import me.tasy5kg.cutegif.BuildConfig
import me.tasy5kg.cutegif.MyApplication.Companion.appContext
import me.tasy5kg.cutegif.MyConstants
import me.tasy5kg.cutegif.R
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.Serializable
import java.util.*
import kotlin.Pair
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import androidx.core.util.Pair as UtilPair

object Toolbox {

  /** returns a Boolean that indicates whether the WeChat QR scanner was opened successfully or not **/
  fun Context.openWeChatQrScanner(): Boolean {
    return try {
      startActivity(Intent().apply {
        component = ComponentName("com.tencent.mm", "com.tencent.mm.ui.LauncherUI")
        flags = Intent.FLAG_RECEIVER_FOREGROUND or Intent.FLAG_ACTIVITY_CLEAR_TOP
        action = "android.intent.action.VIEW"
        putExtra("LauncherUI.From.Scaner.Shortcut", true)
      })
      true
    } catch (_: Exception) {
      false
    }
  }

  inline fun newRunnableWithSelf(crossinline lambda: (Runnable) -> Unit): Runnable {
    return object : Runnable {
      override fun run() {
        lambda(this)
      }
    }
  }

  inline fun View.enableDropFile(
    activity: Activity, mimeType: String, crossinline onReceiveContentListener: (Uri) -> Unit
  ) {
    configureView(activity, this, arrayOf(mimeType), Options.Builder().build(), OnReceiveContentListener { _, payload ->
      if (payload.source == ContentInfoCompat.SOURCE_DRAG_AND_DROP && payload.clip.itemCount == 1 && payload.clip.description.getMimeType(
          0
        ).contains(mimeType.replace("*", ""))
      ) {
        onReceiveContentListener(payload.clip.getItemAt(0).uri)
        return@OnReceiveContentListener null
      } else {
        return@OnReceiveContentListener payload
      }
    })
  }

  // Usage: Add ```lifecycle.logRedOnLifecycleEvent()``` to onCreate() method of a activity
  fun logRedOnLifecycleEvent(): LifecycleEventObserver {
    return LifecycleEventObserver { source, event ->
      logRed("LifecycleEvent", "${source::class.java.simpleName} ${event.name}")
    }
  }

  data class WidthHeight(val w: Int, val h: Int) {
    val short = min(w, h)
    val long = max(w, h)
  }

  data class UriWrapper(val uriString: String) : Serializable {
    constructor(uri: Uri) : this(uri.toString())

    fun getUri() = Uri.parse(uriString)!!
  }


  inline fun <V : View> V.onClick(
    hapticFeedbackType: Int? = null,
    crossinline lambda: V.() -> Unit,
  ) = setOnClickListener { _ ->
    hapticFeedbackType?.let { performHapticFeedback(it) }
    lambda(this)
  }

  inline fun RangeSlider.onSliderTouch(
    crossinline onStartTrackingTouch: RangeSlider.() -> Unit,
    crossinline onStopTrackingTouch: RangeSlider.() -> Unit,
  ) {
    addOnSliderTouchListener(object : RangeSlider.OnSliderTouchListener {
      override fun onStartTrackingTouch(slider: RangeSlider) = onStartTrackingTouch(slider)
      override fun onStopTrackingTouch(slider: RangeSlider) = onStopTrackingTouch(slider)
    })
  }

  fun <T : Comparable<T>> T.constraintBy(range: ClosedRange<T>) = when {
    this < range.start -> range.start
    this > range.endInclusive -> range.endInclusive
    else -> this
  }

  /** ColorInt */
  @get:ColorInt
  var View.backgroundColor
    get() = (background as ColorDrawable).color
    set(@ColorInt colorInt: Int) = setBackgroundColor(colorInt)

  enum class ColorStringType { ARGB, RGBA, RGB }

  fun Int.colorIntToHex(): String {
    val format = "%02x"
    val r = format.format(Color.red(this))
    val g = format.format(Color.green(this))
    val b = format.format(Color.blue(this))
    return r + g + b
  }

  fun Boolean.toInt() = if (this) 1 else 0

  fun View.flipVisibility() {
    visibility = if (visibility == VISIBLE) GONE else VISIBLE
  }

  inline fun <T : View> T.visibleIf(condition: T.() -> Boolean) {
    visibility = if (condition(this)) VISIBLE else GONE
  }

  /**
   * listOf("apple", "banana", "cherry", "dragon fruit").joinToStringSpecial(", ", " and ")
   * -> "apple, banana, cherry and dragon fruit"
   */
  fun <T> Iterable<T>.joinToStringSpecial(separator: String, lastSeparator: String) = when (count()) {
    0 -> ""
    1 -> elementAt(0).toString()
    else -> with(joinToString(separator)) {
      substringBeforeLast(separator) + lastSeparator + substringAfterLast(separator)
    }
  }

  @Suppress("UNCHECKED_CAST", "DEPRECATION")
  fun <T> Intent.getExtra(key: String) = this.extras!!.get(key) as T

  fun appGetString(@StringRes resId: Int, vararg formatArgs: Any) = appContext.getString(resId, formatArgs)

  fun <K, V> LinkedHashMap<K, V>.getKeyByValue(value: V): K = this.filter { it.value == value }.keys.first()

  fun pathToUri(path: String) = Uri.fromFile(File(path))

  @SuppressLint("SimpleDateFormat")
  fun getTimeYMDHMS(): String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date(System.currentTimeMillis()))


  fun getScreenWH() = with(Resources.getSystem().displayMetrics) { Pair(widthPixels, heightPixels) }

  fun openLink(context: Context, url: String) = try {
    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
  } catch (_: Exception) {
  }

  fun logRed(tag: Any?, msg: Any?) {
    if (BuildConfig.DEBUG) {
      Log.e(tag.toString(), msg.toString())
    }
  }

  fun Double.closestEven() = (this / 2).roundToInt() * 2

  fun toast(text: String) = Toast.makeText(appContext, text, Toast.LENGTH_LONG).show()


  /**
   *  Usage example:
   *    createColorStateListFromColorResource(arrayOf(
   *      android.R.attr.state_checked to R.color.green_light,
   *      android.R.attr.state_checkable to R.color.light))
   */
  fun createColorStateListFromColorResource(list: Array<Pair<Int, Int>>) =
    ColorStateList(
      list.map { intArrayOf(it.first) }.toTypedArray(),
      list.map { appContext.resources.getColor(it.second, appContext.theme) }.toIntArray()
    )

  /**
   *  Usage example:
   *    createColorStateListFromColorParsed(arrayOf(
   *      android.R.attr.state_checked to Color.parseColor("#000000"),
   *      android.R.attr.state_checkable to Color.parseColor("#FFFFFF")))
   */
  fun createColorStateListFromColorParsed(list: Array<Pair<Int, Int>>) =
    ColorStateList(list.map { intArrayOf(it.first) }.toTypedArray(), list.map { it.second }.toIntArray()
    )

  fun copyTextToClipboard(@StringRes stringId: Int, @StringRes toastStringId: Int) =
    copyTextToClipboard(appGetString(stringId), appGetString(toastStringId))

  fun copyTextToClipboard(text: String, toastText: String?) {
    appContext.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText(null, text))
    toastText?.let { toast(it) }
  }


  fun Double.keepNDecimalPlaces(n: Int) = String.format("%.${n}f", this)

  fun Float.keepNDecimalPlaces(n: Int) = String.format("%.${n}f", this)

  fun Activity.keepScreenOn(keepScreenOn: Boolean) = runOnUiThread {
    if (keepScreenOn) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) else window.clearFlags(
      WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
    )
  }

  inline fun String.toEmptyStringIf(condition: String.() -> Boolean) = if (condition(this)) "" else this

  fun <T> Pair<T, T>.swap() = Pair(second, first)

  inline fun <T> Pair<T, T>.swapIf(condition: Pair<T, T>.() -> Boolean) = if (condition(this)) this.swap() else this

  fun ClipData.toUriList(): MutableList<Uri> {
    val uriList = mutableListOf<Uri>()
    for (i in 0 until this.itemCount) {
      uriList.add(this.getItemAt(i).uri)
    }
    return uriList
  }

  fun cmivJoinQqGroupLambda(context: Context) {
    val intent = Intent().apply {
      data = Uri.parse(MyConstants.URI_JOIN_QQ_GROUP)
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
      context.startActivity(intent)
    } catch (e: Exception) {
      copyTextToClipboard(
        appGetString(R.string.qq_group_id), appGetString(R.string.join_qq_group_toast)
      )
    }
  }

  fun TextView.setupTextViewWithClickablePart(
    @StringRes fullText: Int,
    listOfSubStringIdAndOnClick: List<Pair<Int, () -> Unit>>,
    isUnderlineSubText: Boolean = false,
  ) = this.setupTextViewWithClickablePart(
    appGetString(fullText), listOfSubStringIdAndOnClick.map {
      Pair(appGetString(it.first), it.second)
    }, isUnderlineSubText
  )

  fun TextView.setupTextViewWithClickablePart(
    fullText: String,
    listOfSubStringAndOnClick: List<Pair<String, () -> Unit>>,
    isUnderlineSubText: Boolean = false,
  ) {
    this.text = SpannableString(fullText).apply {
      for (subStringAndOnClick in listOfSubStringAndOnClick) {
        val subStringIndex = fullText.indexOf(subStringAndOnClick.first)
        this.setSpan(
          object : ClickableSpan() {
            override fun onClick(widget: View) {
              subStringAndOnClick.second()
            }

            override fun updateDrawState(ds: TextPaint) {
              super.updateDrawState(ds)
              ds.isUnderlineText = isUnderlineSubText
            }
          }, subStringIndex, subStringIndex + subStringAndOnClick.first.length, SPAN_EXCLUSIVE_EXCLUSIVE
        )
      }
    }
    this.movementMethod = LinkMovementMethod.getInstance()
  }

  @ColorInt
  fun Int.getContrastColor(reverse: Boolean = false) =
    if (ColorUtils.calculateContrast(this, Color.BLACK) > ColorUtils.calculateContrast(
        this, Color.WHITE
      )
    ) Color.BLACK else Color.WHITE

  fun sceneTransitionAnimationOptionBuilder(activity: Activity, vararg view: View) =
    ActivityOptionsCompat.makeSceneTransitionAnimation(
      activity, *(view.map { UtilPair.create(it, it.transitionName) }.toTypedArray())
    )

  fun RangeSlider.boundRange() = valueFrom..valueTo

  fun RangeSlider.valueRange() = values[0]..values[1]

  fun exec(command: String, envp: Array<String>? = null): Triple<Int, String, String> {
    val proc = if (envp == null) Runtime.getRuntime().exec(command) else Runtime.getRuntime().exec(command, envp)
    val stdInput = BufferedReader(InputStreamReader(proc.inputStream))
    val stdError = BufferedReader(InputStreamReader(proc.errorStream))

    val stdO = StringBuilder()
    var s: String?
    while (stdInput.readLine().also { s = it } != null) {
      stdO.append("\n").append(s)
    }

    val stdE = StringBuilder()
    while (stdError.readLine().also { s = it } != null) {
      stdE.append("\n").append(s)
    }

    return Triple(proc.waitFor(), stdO.toString(), stdE.toString())
  }

  fun exec(command: String, envp: Array<String>? = null, output: ((String) -> Unit)? = null, outputError: ((String) -> Unit)? = null): Int {
    val process = if (envp == null) Runtime.getRuntime().exec(command) else Runtime.getRuntime().exec(command, envp)
    val stdInput = BufferedReader(InputStreamReader(process.inputStream))
    val stdError = BufferedReader(InputStreamReader(process.errorStream))
    var stdO: String?
    while (stdInput.readLine().also { stdO = it } != null) {
      output?.invoke(stdO!!)
    }
    var stdE: String?
    while (stdError.readLine().also { stdE = it } != null) {
      outputError?.invoke(stdE!!)
    }
    return process.waitFor()
  }

  /** Example: 123456L -> "2:03.4" */
  fun msToMinSecDs(ms: Int) = with(ms / 1000f) {
    "${(this / 60).toInt()}:${
      String.format(
        "%02d", (this % 60).toInt()
      )
    }.${((this - floor(this)) * 10).toInt()}"
  }
}
