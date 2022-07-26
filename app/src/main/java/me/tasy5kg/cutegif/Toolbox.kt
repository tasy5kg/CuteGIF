package me.tasy5kg.cutegif

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.*
import android.content.res.ColorStateList
import android.database.Cursor
import android.icu.text.SimpleDateFormat
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat.getSystemService
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import me.tasy5kg.cutegif.Constants.SHARED_PREFERENCES_KEY_IS_FIRST_START
import me.tasy5kg.cutegif.Constants.SHARED_PREFERENCES_SETTING
import me.tasy5kg.cutegif.MyApplication.Companion.context
import java.util.*


class Toolbox {
    companion object {

        private fun getMemInfo(): ActivityManager.MemoryInfo {
            val memInfo = ActivityManager.MemoryInfo()
            (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(memInfo)
            return memInfo
        }

        fun debugInfo() =
            "[System Info]\n" +
                    "Android SDK Version = ${Build.VERSION.SDK_INT}\n" +
                    "Supported ABIs = ${Build.SUPPORTED_ABIS.joinToString(separator = ",")}\n" +
                    "Manufacturer = ${Build.MANUFACTURER}\n" +
                    "Brand = ${Build.BRAND}\n" +
                    "Model = ${Build.MODEL}\n" +
                    "Languages = ${context.resources.configuration.locales.toLanguageTags()}\n" +
                    "Current Timestamp = ${System.currentTimeMillis()}\n" +
                    "Total Memory = ${getMemInfo().totalMem}\n" +
                    "Available Memory = ${getMemInfo().availMem}\n\n" +
                    "[Application Info]\n" +
                    "Application ID = ${BuildConfig.APPLICATION_ID}\n" +
                    "Version Code = ${BuildConfig.VERSION_CODE}\n" +
                    "Version Name = ${BuildConfig.VERSION_NAME}\n" +
                    "Build Type = ${BuildConfig.BUILD_TYPE}\n" +
                    "Debug = ${BuildConfig.DEBUG}"


        fun openLink(context: Context, url: String) = context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))

        fun view3rdPartyOSSLicenses(context: Context) = context.startActivity(Intent(context, OssLicensesMenuActivity::class.java))

        fun isFirstStart() = SHARED_PREFERENCES_SETTING.getBoolean(SHARED_PREFERENCES_KEY_IS_FIRST_START, true)

        fun setFirstStart(firstStart: Boolean) {
            SHARED_PREFERENCES_SETTING.edit().apply {
                putBoolean(SHARED_PREFERENCES_KEY_IS_FIRST_START, firstStart)
                apply()
            }
        }

        fun localeEqualsZhOrCn(): Boolean {
            val firstLocale = context.resources.configuration.locales[0]
            return firstLocale.country.equals("CN") || firstLocale.language.equals("zh")
        }

        fun logging(tag: String, msg: String) {
            if (BuildConfig.DEBUG) {
                Log.e(tag, msg)
            }
        }


        fun createColorStateList(list: Array<Pair<Int, Int>>) =
            ColorStateList(
                list.map { intArrayOf(it.first) }.toTypedArray(),
                list.map { context.resources.getColor(it.second, context.theme) }.toIntArray()
            )

        fun copyToClipboard(context: Context, @StringRes stringId: Int, @StringRes toastStringId: Int) =
            copyToClipboard(context, context.getString(stringId), context.getString(toastStringId))

        fun copyToClipboard(context: Context, text: String, toastText: String) {
            getSystemService(context, ClipboardManager::class.java)!!.setPrimaryClip(ClipData.newPlainText(null, text))
            Toast.makeText(context, toastText, Toast.LENGTH_LONG).show()
        }

        fun keepNDecimalPlaces(double: Double, n: Int) = String.format("%.${n}f", double)

        fun getFileSizeFromUri(uri: Uri) = context.contentResolver.openAssetFileDescriptor(uri, "r")!!.length

        @SuppressLint("SimpleDateFormat")
        fun createNewGifFileAndReturnUri(fileName: String) =
            context.contentResolver.insert(MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), ContentValues().apply {
                // on API 29, insert a media with a existed DISPLAY_NAME sometimes lead to NullPointException
                put(MediaStore.Images.Media.DISPLAY_NAME, "${fileName}${SimpleDateFormat("_yyyyMMdd_HHmmss").format(Date())}.gif")
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CuteGif")
                put(MediaStore.Images.Media.MIME_TYPE, "image/gif")
            })!!

        @SuppressLint("Range")
        fun getFileNameFromUri(uri: Uri, removeSuffix: Boolean): String {
            val fileName: String
            val cursor: Cursor = context.contentResolver.query(uri, null, null, null, null)!!
            cursor.moveToFirst()
            fileName = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
            cursor.close()
            return if (!fileName.contains('.') || !removeSuffix) {
                fileName
            } else {
                with(fileName) { substring(0, lastIndexOf('.')) }
            }
        }
    }

}