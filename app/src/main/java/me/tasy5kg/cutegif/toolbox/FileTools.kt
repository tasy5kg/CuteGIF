package me.tasy5kg.cutegif.toolbox

import android.content.ContentValues
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import me.tasy5kg.cutegif.MyApplication
import me.tasy5kg.cutegif.MyConstants
import me.tasy5kg.cutegif.R
import me.tasy5kg.cutegif.toolbox.Toolbox.keepNDecimalPlaces
import me.tasy5kg.cutegif.toolbox.Toolbox.toEmptyStringIf
import me.tasy5kg.cutegif.toolbox.Toolbox.toast
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import kotlin.math.nextUp

object FileTools {

  enum class FileSizeUnit(val unitName: String, val multiple: Double) {
    B("B", 1.0), KB("KB", 1024.0), MB("MB", 1048576.0), GB("GB", 1073741824.0);
  }

  class FileName(private val filePathOrName: String) {
    constructor(uri: Uri) : this(when (uri.scheme) {
      "content" -> {
        MyApplication.appContext.contentResolver.query(uri, null, null, null, null)!!.use { cursor ->
          val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
          cursor.moveToFirst()
          cursor.getString(nameIndex)
        }
      }

      "file" -> uri.lastPathSegment
      else -> throw IllegalArgumentException("uri.scheme = $uri.scheme")
    }!!)

    /**
     * "/sdcard/video.mp4" -> "video.mp4"
     * "video.mp4" -> "video.mp4"
     **/
    val name get() = filePathOrName.substringAfterLast('/')

    /** "/sdcard/video.mp4" -> "mp4" */
    val extension get() = name.substringAfterLast('.')

    /** "/sdcard/video.mp4" -> "video" */
    val nameWithoutExtension get() = name.substringBeforeLast('.')

  }

  fun createNewFile(fileNamePrefix: String?, fileType: String): Uri {
    val appName = Toolbox.appGetString(R.string.app_name)
    val fileName =
      ("${fileNamePrefix}_").toEmptyStringIf { fileNamePrefix.isNullOrBlank() } + "${appName}_${Toolbox.getTimeYMDHMS()}.$fileType"
    return when (fileType) {
      "mp4" -> MyApplication.appContext.contentResolver.insert(MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
        ContentValues().apply {
          put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
          put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/${appName}")
          put(
            MediaStore.Video.Media.MIME_TYPE, MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileType)
          )
        })

      "gif", "png" -> MyApplication.appContext.contentResolver.insert(MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
        ContentValues().apply {
          put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
          put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/${appName}")
          put(
            MediaStore.Images.Media.MIME_TYPE, MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileType)
          )
        })

      else -> throw NotImplementedError("fileType = $fileType")
    }!!
  }

  fun copyFile(srcPath: String, destUri: Uri, deleteSrc: Boolean = false) {
    val srcFile = File(srcPath)
    MyApplication.appContext.contentResolver.openOutputStream(destUri)!!.use { outputStream ->
      FileInputStream(srcFile).use { inputStream ->
        copyStream(inputStream, outputStream)
      }
    }
    if (deleteSrc) srcFile.delete()
  }

  fun copyFile(srcUri: Uri, destPath: String) {
    FileOutputStream(File(destPath)).use { outputStream ->
      MyApplication.appContext.contentResolver.openInputStream(srcUri)!!.use { inputStream ->
        copyStream(inputStream, outputStream)
      }
    }
  }

  private const val BUFFER_SIZE = 131072
  private fun copyStream(
    inputStream: InputStream, outputStream: OutputStream, onCopy: ((bytesCopied: Long) -> Unit)? = null
  ) {
    var bytesCopied: Long = 0
    val buffer = ByteArray(BUFFER_SIZE)
    var bytes = inputStream.read(buffer)
    while (bytes >= 0) {
      outputStream.write(buffer, 0, bytes)
      bytesCopied += bytes
      if (onCopy != null) onCopy(bytesCopied)
      bytes = inputStream.read(buffer)
    }
  }

  fun Uri.copyToInputFileDir(): String {
    makeDirEmpty(MyConstants.INPUT_FILE_DIR)
    val inputFilePath = MyConstants.INPUT_FILE_DIR + FileName(this).name
    MyApplication.appContext.contentResolver.openInputStream(this)!!.use { inputStream ->
      FileOutputStream(inputFilePath).use { outputStream ->
        var toastedPleaseWait = false
        val startTime = System.nanoTime()
        copyStream(inputStream, outputStream) { totalBytesCopied ->
          if (System.nanoTime() - startTime > 1000000000L && !toastedPleaseWait) {
            toast(
              "正在读取文件，大约还需${
                ((System.nanoTime() - startTime) * fileSize() / 1000000000f / totalBytesCopied).nextUp().toInt()
              }秒，请稍等..."
            )
            toastedPleaseWait = true
          }
        }
      }
    }
    return inputFilePath
  }

  fun makeDirEmpty(dir: String) = File(dir).apply {
    mkdirs()
    deleteRecursively()
    mkdirs()
  }

  fun Uri.deleteFile() {
    try {
      when (this.scheme) {
        "content" -> MyApplication.appContext.contentResolver.delete(this, null, null)
        "file" -> MyApplication.appContext.contentResolver.delete(
          MediaStore.Images.Media.EXTERNAL_CONTENT_URI, MediaStore.Images.Media.DATA + "=?", arrayOf(this.path)
        )

        else -> throw IllegalArgumentException("gifUri.scheme = ${this.scheme}")
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  fun Uri.fileSize() = when (scheme) {
    "content" -> MyApplication.appContext.contentResolver.openAssetFileDescriptor(this, "r")!!.use { it.length }

    "file" -> File(path!!).length()
    else -> throw IllegalArgumentException("uri.scheme = $scheme")
  }

  fun Long.formattedFileSize(
    fileSizeUnit: FileSizeUnit = FileSizeUnit.KB,
    decimalPlaces: Int = 0,
    appendUnit: Boolean = true,
  ) = (this / fileSizeUnit.multiple).keepNDecimalPlaces(decimalPlaces) + if (appendUnit) {
    fileSizeUnit.unitName
  } else {
    ""
  }

  fun Uri.mimeType() = when (scheme) {
    "content" -> MyApplication.appContext.contentResolver.getType(this)
    "file" -> MimeTypeMap.getSingleton().getMimeTypeFromExtension(FileName(this).extension)
    else -> null
  }

}