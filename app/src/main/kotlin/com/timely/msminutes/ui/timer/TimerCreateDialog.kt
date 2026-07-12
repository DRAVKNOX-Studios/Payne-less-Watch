package com.timely.msminutes.ui.timer

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.media.RingtoneManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.EditText
import android.widget.NumberPicker
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import com.timely.msminutes.R
import com.timely.msminutes.data.Prefs
import com.timely.msminutes.data.TimerItem
import com.timely.msminutes.ui.view.SoundPickerDialog
import com.timely.msminutes.util.AppExecutors
import com.timely.msminutes.util.ThemeApplier
import com.timely.msminutes.util.ThemeStore
import com.timely.msminutes.util.ThemeTokens
import com.timely.msminutes.util.ThemeUtil

class TimerCreateDialog(private val context: Context, private val listener: OnCreateListener) {

    interface OnCreateListener {
        fun onCreate(item: TimerItem?)
        fun onPickCustomSound()
    }

    private var selectedSound: Uri? = null
    private var selectedSoundName: String? = null

    private var soundLabel: TextView? = null
    private var dialog: Dialog? = null
    private var onDismissListener: (() -> Unit)? = null
    private var activeSoundPicker: SoundPickerDialog? = null

    fun setOnDismissListener(l: () -> Unit) {
        onDismissListener = l
    }

    fun setSelectedSound(uri: Uri?, name: String?) {
        selectedSound     = uri
        selectedSoundName = name
        soundLabel?.text  = name
    }

    fun show() {
        if (dialog != null && dialog?.isShowing == true) return

        // Re-use the existing dialog window if it is already created (hidden state).
        // Only inflate + wire up views on the very first show().
        if (dialog != null) {
            revealWindow()
            return
        }

        val view       = LayoutInflater.from(context).inflate(R.layout.dialog_timer_create, null)
        val hours      = view.findViewById<NumberPicker>(R.id.picker_hours)
        val minutes    = view.findViewById<NumberPicker>(R.id.picker_minutes)
        val seconds    = view.findViewById<NumberPicker>(R.id.picker_seconds)
        val labelInput = view.findViewById<EditText>(R.id.input_timer_label)
        val vibSwitch  = view.findViewById<SwitchCompat>(R.id.switch_timer_vibrate)
        soundLabel     = view.findViewById(R.id.text_timer_sound)

        hours.minValue   = 0;  hours.maxValue   = 23
        minutes.minValue = 0;  minutes.maxValue = 59
        seconds.minValue = 0;  seconds.maxValue = 59
        minutes.value    = 5

        selectedSound    = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        soundLabel?.text = "Default"
        AppExecutors.get().diskIO {
            val uri = RingtoneManager.getActualDefaultRingtoneUri(
                context.applicationContext, RingtoneManager.TYPE_ALARM
            )
            val name = try {
                RingtoneManager.getRingtone(context, uri)?.getTitle(context) ?: "Default"
            } catch (_: Exception) { "Default" }
            Handler(Looper.getMainLooper()).post {
                if (dialog?.isShowing == true) {
                    selectedSound     = uri
                    selectedSoundName = name
                    soundLabel?.text  = name
                }
            }
        }

        view.findViewById<View>(R.id.text_timer_sound_row).setOnClickListener {
            if (activeSoundPicker?.isShowing == true) return@setOnClickListener

            // Hide this window (GONE visibility + FLAG_NOT_TOUCHABLE) so its
            // ViewRootImpl and GL buffer are retained in memory but not composited
            // by SurfaceFlinger. Then show SoundPickerDialog alone.
            // This means only 1 window is ever being rendered at a time, but
            // there is no re-inflate / re-wire on return — zero transition jank.
            concealWindow()

            val picker = SoundPickerDialog(
                context, selectedSound,
                object : SoundPickerDialog.OnSoundSelectedListener {
                    override fun onSoundSelected(name: String?, uri: Uri?) {
                        selectedSound     = uri
                        selectedSoundName = name
                        soundLabel?.text  = name
                        revealWindow()          // restore timer dialog
                    }
                    override fun onAddCustom() {
                        // Custom picker is handled by the host via Activity result.
                        // Timer window stays hidden; host calls resumeAfterCustomPicker().
                        listener.onPickCustomSound()
                    }
                }
            )
            activeSoundPicker = picker
            picker.apply {
                // When the sound picker is cancelled (no selection made),
                // re-show the timer dialog so the user is not left stranded.
                setOnDismissListener { if (dialog?.window?.decorView?.visibility == View.GONE) revealWindow() }
            }.show()
        }

        val d = Dialog(context)
        dialog = d
        d.requestWindowFeature(Window.FEATURE_NO_TITLE)
        d.setContentView(view)
        d.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        d.setOnDismissListener {
            // Full dismiss (Start / Cancel buttons) — release everything.
            soundLabel        = null
            dialog            = null
            selectedSound     = null
            selectedSoundName = null
            onDismissListener?.invoke()
        }

        applyTheme(view, vibSwitch, labelInput, hours, minutes, seconds)

        view.findViewById<View>(R.id.btn_timer_start).setOnClickListener {
            val totalMs = (hours.value * 3600L + minutes.value * 60L + seconds.value) * 1000L
            if (totalMs > 0) {
                val item = TimerItem().apply {
                    this.totalMillis     = totalMs
                    this.remainingMillis = totalMs
                    this.isVibrate       = vibSwitch.isChecked
                    this.soundUri        = selectedSound?.toString()
                    this.label           = labelInput.text.toString().trim()
                }
                listener.onCreate(item)
            }
            d.dismiss()
        }
        view.findViewById<View>(R.id.btn_timer_cancel).setOnClickListener { d.dismiss() }
        d.show()
    }

    /**
     * Hides the timer dialog's window surface from SurfaceFlinger without
     * calling dismiss().  The ViewRootImpl and view tree stay intact so
     * re-showing is instant with no re-inflation.
     *
     * FLAG_NOT_TOUCHABLE prevents accidental touch events reaching the
     * invisible window while the sound picker is on top.
     */
    private fun concealWindow() {
        val win = dialog?.window ?: return
        win.decorView.visibility = View.GONE
        val lp = win.attributes
        lp.flags = lp.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        win.attributes = lp
    }

    /**
     * Reverses [concealWindow] — makes the window visible and interactive again.
     */
    private fun revealWindow() {
        val win = dialog?.window ?: return
        win.decorView.visibility = View.VISIBLE
        val lp = win.attributes
        lp.flags = lp.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
        win.attributes = lp
    }

    /**
     * Call from the host Activity/Fragment after the custom-sound file picker
     * returns so the timer dialog becomes visible again.
     */
    fun resumeAfterCustomPicker() {
        revealWindow()
    }

    fun dismiss() {
        dialog?.dismiss()
    }

    private fun applyTheme(
        root: View, vibrateSwitch: SwitchCompat?, labelInput: EditText,
        hours: NumberPicker?, minutes: NumberPicker?, seconds: NumberPicker?
    ) {
        TimerCreateThemeHelper.applyTheme(context, root, vibrateSwitch, labelInput, hours, minutes, seconds)
    }
}
