package com.timely.msminutes.ui.alarm

import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.result.ActivityResultLauncher
import com.timely.msminutes.ui.view.SoundPickerDialog

/**
 * Helper to handle sound picking logic for Alarms.
 */
class SoundPickerHelper(
    private val context: Context,
    private val customSoundLauncher: ActivityResultLauncher<Intent>,
    private val onSoundSelected: (Uri?, String) -> Unit
) {

    private var activeDialog: SoundPickerDialog? = null

    fun openSoundPicker(currentUri: Uri?) {
        if (activeDialog?.isShowing == true) return
        
        val dialog = SoundPickerDialog(context, currentUri, object : SoundPickerDialog.OnSoundSelectedListener {
            override fun onSoundSelected(name: String?, uri: Uri?) {
                onSoundSelected(uri, name ?: getRingtoneName(uri))
            }

            override fun onAddCustom() {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "audio/*"
                }
                customSoundLauncher.launch(intent)
            }
        })
        activeDialog = dialog
        dialog.show()
    }

    fun onCustomSoundPicked(uri: Uri) {
        activeDialog?.let { dialog ->
            if (dialog.isShowing) {
                dialog.loadSounds() // Refresh list with new sound
                dialog.setSelectedUri(uri) // Mark it active
            }
        }
    }

    fun getFileName(uri: Uri): String {
        var name = "Custom"
        try {
            context.contentResolver.query(uri, null, null, null, null).use { cursor ->
                if (cursor != null && cursor.moveToFirst()) {
                    name = cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
                }
            }
        } catch (ignored: Exception) {
        }
        return cleanName(name)
    }

    fun getRingtoneName(uri: Uri?): String {
        if (uri == null) return "None"
        
        // Try to get name from ContentResolver first (for custom files)
        val fileName = try {
            var name: String? = null
            context.contentResolver.query(uri, null, null, null, null).use { cursor ->
                if (cursor != null && cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index != -1) name = cursor.getString(index)
                }
            }
            name
        } catch (e: Exception) {
            null
        }

        if (fileName != null) return cleanName(fileName)

        // Fallback to RingtoneManager
        return try {
            val ringtone = RingtoneManager.getRingtone(context, uri)
            val title = ringtone.getTitle(context)
            
            // If title looks like a path (contains /), try to get just the last part
            val cleanTitle = if (title.contains("/") || title.contains(":")) {
                title.substringAfterLast('/')
            } else {
                title
            }
            
            cleanName(cleanTitle)
        } catch (e: Exception) {
            cleanName(uri.lastPathSegment ?: "Unknown")
        }
    }

    private fun cleanName(name: String?): String {
        if (name == null) return "Unknown"
        val dot = name.lastIndexOf('.')
        return if (dot > 0) name.substring(0, dot) else name
    }
}
