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
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

object FileTools {

  enum class FileSizeUnit(val unitName: String, val multiple: Double) {
    B("B", 1.0),
    KB("KB", 1024.0),
    MB("MB", 1048576.0),
    GB("GB", 1073741824.0);
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
      "mp4" -> MyApplication.appContext.contentResolver.insert(
        MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
        ContentValues().apply {
          put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
          put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/${appName}")
          put(MediaStore.Video.Media.MIME_TYPE, MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileType))
        })

      "gif", "png" -> MyApplication.appContext.contentResolver.insert(
        MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
        ContentValues().apply {
          put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
          put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/${appName}")
          put(MediaStore.Images.Media.MIME_TYPE, MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileType))
        })

      else -> throw NotImplementedError("fileType = $fileType")
    }!!
  }

  fun copyFile(srcPath: String, destUri: Uri, deleteSrc: Boolean = false) {
    val destUriOutputStream = MyApplication.appContext.contentResolver.openOutputStream(destUri)!!
    val srcFile = File(srcPath)
    val srcPathInputStream = FileInputStream(srcFile)
    srcPathInputStream.copyTo(destUriOutputStream)
    destUriOutputStream.close()
    srcPathInputStream.close()
    if (deleteSrc) {
      srcFile.delete()
    }
  }

  fun copyFile(srcUri: Uri, destPath: String) {
    val destFile = File(destPath)
    val destFileOutputStream = FileOutputStream(destFile)
    val srcInputStream = MyApplication.appContext.contentResolver.openInputStream(srcUri)!!
    srcInputStream.copyTo(destFileOutputStream)
    srcInputStream.close()
    destFileOutputStream.close()
  }

  fun InputStream.copyToWithClose(outputStream: OutputStream) =
    try {
      copyTo(outputStream)
    } finally {
      close()
      outputStream.close()
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
          MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
          MediaStore.Images.Media.DATA + "=?",
          arrayOf(this.path)
        )

        else -> throw IllegalArgumentException("gifUri.scheme = ${this.scheme}")
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  fun Uri.fileSize() =
    when (scheme) {
      "content" -> {
        val assetFileDescriptor =
          MyApplication.appContext.contentResolver.openAssetFileDescriptor(this, "r")!!
        val fileSize = assetFileDescriptor.length
        assetFileDescriptor.close()
        fileSize
      }

      "file" -> File(path!!).length()
      else -> throw IllegalArgumentException("uri.scheme = $scheme")
    }

  fun Long.formattedFileSize(
    fileSizeUnit: FileSizeUnit = FileSizeUnit.KB,
    decimalPlaces: Int = 0,
    appendUnit: Boolean = true,
  ) =
    (this / fileSizeUnit.multiple).keepNDecimalPlaces(decimalPlaces) +
        if (appendUnit) {
          fileSizeUnit.unitName
        } else {
          ""
        }

  fun Uri.mimeType() =
    when (scheme) {
      "content" -> MyApplication.appContext.contentResolver.getType(this)
      "file" -> MimeTypeMap.getSingleton().getMimeTypeFromExtension(FileName(this).extension)
      else -> null
    }

  fun Uri.copyToInputFileDir(): String {
    makeDirEmpty(MyConstants.INPUT_FILE_DIR)
    val inputFilePath = MyConstants.INPUT_FILE_DIR + FileName(this).name
    copyFile(this, inputFilePath)
    return inputFilePath
  }

}