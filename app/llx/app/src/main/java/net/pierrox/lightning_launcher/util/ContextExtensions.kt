@file:JvmName("ContextExtensions")

package net.pierrox.lightning_launcher.util

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns

fun Context.getNameForUri(uri: Uri): String? {
    var name: String? = null
    if (uri.scheme == "content") {
        val cursor: Cursor? = getContentResolver().query(
            uri,
            arrayOf<String>(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )
        try {
            if (cursor != null && cursor.moveToFirst()) {
                name = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
            }
        } finally {
            cursor?.close()
        }
    }
    if (name == null) {
        name = uri.lastPathSegment
    }
    return name
}