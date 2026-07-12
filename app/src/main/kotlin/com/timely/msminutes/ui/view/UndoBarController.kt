package com.timely.msminutes.ui.view

import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.TextView
import com.timely.msminutes.util.ThemeApplier
import com.timely.msminutes.util.ThemeStore
import com.timely.msminutes.util.ThemeTokens

/**
 * Manages the visibility and theme of an undo bar UI component.
 */
class UndoBarController(
    private val undoBar: View,
    private val messageView: TextView,
    private val undoButton: TextView,
    private var onUndoClicked: (() -> Unit)? = null,
    private val onHide: () -> Unit
) {
    private val handler = Handler(Looper.getMainLooper())
    private var hideBarRunnable: Runnable? = null

    init {
        undoButton.setOnClickListener {
            hideBarRunnable?.let { handler.removeCallbacks(it) }
            onUndoClicked?.invoke()
            hide()
        }
    }

    fun setUndoListener(listener: () -> Unit) {
        this.onUndoClicked = listener
    }

    fun show(message: String, durationMs: Long) {
        messageView.text = message
        undoBar.visibility = View.VISIBLE
        applyTheme(ThemeStore.get().current())
        
        hideBarRunnable?.let { handler.removeCallbacks(it) }
        val hide = Runnable { 
            hide()
            onHide()
        }
        hideBarRunnable = hide
        handler.postDelayed(hide, durationMs)
    }

    fun hide() {
        undoBar.visibility = View.GONE
        hideBarRunnable?.let { handler.removeCallbacks(it) }
        hideBarRunnable = null
    }

    fun applyTheme(t: ThemeTokens?) {
        if (t == null) return
        
        ThemeApplier.applyDialogRoot(undoBar, t, 24)
        ThemeApplier.applyTextPrimary(messageView, t)
        
        if (undoButton.getTag(com.timely.msminutes.R.id.view_text_color) != t.accent) {
            undoButton.setTextColor(t.accent)
            undoButton.setTag(com.timely.msminutes.R.id.view_text_color, t.accent)
        }
    }
    
    fun isVisible(): Boolean = undoBar.visibility == View.VISIBLE

    fun shutdown() {
        handler.removeCallbacksAndMessages(null)
    }
}
