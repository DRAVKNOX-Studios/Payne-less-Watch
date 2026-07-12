package com.timely.msminutes.ui.settings

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.appcompat.app.AlertDialog

/**
 * Guides the user through granting the permissions that OEM battery-killers
 * (Funtouch OS, MIUI, ColorOS, HyperOS, OneUI, etc.) require for alarms and
 * the widget to work while the app is in the background.
 * 
 * All intents fall back gracefully: if an OEM-specific action is absent the
 * system Settings app is opened instead, so this never crashes on AOSP.
 * 
 * Call [.showGuideIfNeeded] from SettingsActivity.onResume().
 */
object BackgroundPermissionHelper {
    /** @return true if any background-critical permission appears ungranted.
     */
    fun needsAttention(activity: Activity): Boolean {
        return !isBatteryOptimizationExempt(activity)
                || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                && !Settings.canDrawOverlays(activity))
    }

    /**
     * Shows a plain dialog listing every permission the user should grant.
     * Tapping each item opens the relevant settings screen directly.
     */
    fun showGuideIfNeeded(activity: Activity) {
        if (!needsAttention(activity)) return
        showGuide(activity)
    }

    /** Always shows the guide — use for a dedicated "Background Permissions" button.  */
    fun showGuide(activity: Activity) {
        val message = buildMessage(activity)
        AlertDialog.Builder(activity)
            .setTitle("Background permissions")
            .setMessage(message)
            .setPositiveButton(
                "Battery optimisation",
                DialogInterface.OnClickListener { d: DialogInterface?, w: Int ->
                    openBatteryOptimisation(
                        activity
                    )
                })
            .setNeutralButton(
                "Overlay permission",
                DialogInterface.OnClickListener { d: DialogInterface?, w: Int ->
                    openOverlayPermission(
                        activity
                    )
                })
            .setNegativeButton("Close", null)
            .show()
    }

    // ── Private helpers ──────────────────────────────────────────────────────
    private fun buildMessage(activity: Activity): String {
        val sb = StringBuilder()
        sb.append("Some OEM systems (Vivo Funtouch OS, MIUI, ColorOS, etc.) aggressively ")
        sb.append("kill background apps, which prevents alarms from ringing and the widget ")
        sb.append("from updating.\n\nPlease grant the following:\n\n")

        if (!isBatteryOptimizationExempt(activity)) {
            sb.append("\u2022 Battery Optimisation: disable / set to \'Unrestricted\' for Timely.\n")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
            && !Settings.canDrawOverlays(activity)
        ) {
            sb.append("\u2022 Display over other apps (overlay): required to show the ring screen ")
            sb.append("when the phone is locked.\n")
        }
        sb.append("\nOn Vivo Funtouch OS: also go to Settings \u2192 Battery \u2192 High background ")
        sb.append("power consumption \u2192 enable Timely.")
        return sb.toString()
    }

    private fun isBatteryOptimizationExempt(activity: Activity): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true
        val pm = activity.getSystemService(Activity.POWER_SERVICE) as PowerManager?
        return pm != null && pm.isIgnoringBatteryOptimizations(activity.getPackageName())
    }

    private fun openBatteryOptimisation(activity: Activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        try {
            val i = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            i.setData(Uri.parse("package:" + activity.getPackageName()))
            activity.startActivity(i)
        } catch (e: Exception) {
            // Fallback: open general battery settings (works on all ROMs).
            try {
                activity.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            } catch (ignored: Exception) {
                openAppDetails(activity)
            }
        }
    }

    private fun openOverlayPermission(activity: Activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        try {
            val i = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
            i.setData(Uri.parse("package:" + activity.getPackageName()))
            activity.startActivity(i)
        } catch (e: Exception) {
            openAppDetails(activity)
        }
    }

    private fun openAppDetails(activity: Activity) {
        try {
            val i = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            i.setData(Uri.parse("package:" + activity.getPackageName()))
            activity.startActivity(i)
        } catch (ignored: Exception) {
        }
    }
}
