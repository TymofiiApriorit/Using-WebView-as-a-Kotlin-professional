package com.professional.webview

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class Utils {
    companion object {

        fun urlActions(url: String, context: Context): Boolean {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(url)
            context.startActivity(intent)
            return true
        }

        fun getContentSelectionIntent(): Intent {
            val contentSelectionIntent = Intent(Intent.ACTION_GET_CONTENT)
            contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE)
            contentSelectionIntent.type = "*/*"
            contentSelectionIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            return contentSelectionIntent
        }

        @Throws(IOException::class)
        fun createImage(): File {
            @SuppressLint("SimpleDateFormat")
            val fileName = SimpleDateFormat("yyyy_mm_ss").format(Date())
            val newName = "file_" + fileName + "_"
            val directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            return File.createTempFile(newName, ".jpg", directory)
        }

        fun getChooserIntent(context: Context, contentSelectionIntent: Intent, takePictureIntent: Intent?): Intent {
            val intentArray: Array<Intent?> = if (takePictureIntent != null) {
                arrayOf(takePictureIntent)
            } else {
                arrayOfNulls(0)
            }

            val chooserIntent = Intent(Intent.ACTION_CHOOSER)
            chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent)
            chooserIntent.putExtra(Intent.EXTRA_TITLE, context.getString(R.string.file_chooser))
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray)
            return chooserIntent
        }
    }
}