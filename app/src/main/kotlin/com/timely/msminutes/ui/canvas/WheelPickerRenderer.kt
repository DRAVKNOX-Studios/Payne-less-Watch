package com.timely.msminutes.ui.canvas

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.TypedValue
import android.view.GestureDetector
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.OverScroller
import com.timely.msminutes.util.ThemeTokens
import kotlin.math.abs
import kotlin.math.roundToInt

class WheelPickerRenderer(
    context: Context,
    private val host: android.view.View,
    private val list: CanvasListView,
    var minValue: Int,
    var maxValue: Int,
    var value: Int,
    var labels: Array<String>? = null,
    var isLooping: Boolean = true,
    private val onValueChange: (Int) -> Unit
) {
    private val density = context.resources.displayMetrics.density
    val bounds = RectF()
    private val scroller = OverScroller(context).apply {
        setFriction(0.002f)
    }
    private val itemHeight = 48f * density
    private var scrollY = 0f
    
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 24f * density
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }

    private var inlineEditText: EditText? = null
    private var isEditing = false
    private var scrollListener: ((Float) -> Unit)? = null

    var itemTop: Float = 0f

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean {
            scroller.forceFinished(true)
            return true
        }

        override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            scrollY += distanceY * 1.2f
            host.invalidate()
            return true
        }

        override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
            val minScroll = if (isLooping) Int.MIN_VALUE else 0
            val maxScroll = if (isLooping) Int.MAX_VALUE else ((maxValue - minValue) * itemHeight).toInt()
            scroller.fling(0, scrollY.toInt(), 0, (-velocityY * 1.65f).toInt(), 0, 0, minScroll, maxScroll)
            host.invalidate()
            return true
        }

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            startInlineEditing()
            return true
        }
    })

    init {
        scrollY = (value - minValue) * itemHeight
    }

    fun onLayout(left: Float, top: Float, right: Float, bottom: Float) {
        bounds.set(left, top, right, bottom)
    }

    fun draw(canvas: Canvas, tokens: ThemeTokens) {
        if (scroller.computeScrollOffset()) {
            scrollY = scroller.currY.toFloat()
            host.postInvalidateOnAnimation()
        }

        canvas.save()
        canvas.clipRect(bounds)
        
        val centerY = bounds.centerY()
        val count = (maxValue - minValue) + 1
        
        val firstVisibleItem = (scrollY / itemHeight).toInt() - 3
        val lastVisibleItem = (scrollY / itemHeight).toInt() + 3
        
        for (i in firstVisibleItem..lastVisibleItem) {
            if (!isLooping && (i !in 0 until count)) continue
            
            val displayIndex = if (isLooping) (i % count + count) % count else i
            val itemY = bounds.top + i * itemHeight - scrollY + centerY - bounds.top - itemHeight / 2f
            
            val itemCenterY = itemY + itemHeight / 2f
            val dist = abs(itemCenterY - centerY)
            
            // Hide the item if it's being edited (centered and isEditing is true)
            if (isEditing && dist < itemHeight * 0.8f) continue
            
            val alpha = (1f - (dist / (bounds.height() / 2f))).coerceIn(0.2f, 1f)
            
            textPaint.color = tokens.textPrimary
            textPaint.alpha = (alpha * 255).toInt()
            textPaint.textSize = (18f + 6f * alpha) * density
            
            val label = labels?.getOrNull(displayIndex) ?: (minValue + displayIndex).toString().padStart(2, '0')
            canvas.drawText(label, bounds.centerX(), itemY + itemHeight / 2f + textPaint.textSize / 3f, textPaint)
        }
        
        canvas.restore()
    }

    fun onTouchEvent(event: MotionEvent, x: Float, y: Float): Boolean {
        if (!bounds.contains(x, y)) return false
        
        val localEvent = MotionEvent.obtain(event)
        localEvent.setLocation(x, y)
        gestureDetector.onTouchEvent(localEvent)
        localEvent.recycle()

        if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
            snapToItem()
        }
        return true
    }

    private fun snapToItem() {
        val count = maxValue - minValue + 1
        var targetItem = (scrollY / itemHeight).roundToInt()
        if (!isLooping) {
            targetItem = targetItem.coerceIn(0, count - 1)
        }
        
        val targetScrollY = targetItem * itemHeight
        scroller.startScroll(0, scrollY.toInt(), 0, (targetScrollY - scrollY).toInt())
        host.invalidate()
        
        val displayIndex = if (isLooping) (targetItem % count + count) % count else targetItem
        val newValue = minValue + displayIndex
        if (newValue != value) {
            value = newValue
            onValueChange(value)
        }
    }
    
    fun setValueInternal(v: Int) {
        value = v
        scrollY = (value - minValue) * itemHeight
    }

    private fun startInlineEditing() {
        if (labels != null) return
        if (isEditing) return
        isEditing = true
        host.invalidate()

        val et = EditText(host.context).apply {
            inputType = EditorInfo.TYPE_CLASS_NUMBER
            setText(value.toString())
            setSelection(text.length)
            val t = com.timely.msminutes.util.ThemeStore.get().current()
            val bgColor = t?.surface ?: t?.background ?: 0xFF000000.toInt()
            setBackgroundColor(bgColor)
            setTextColor(t?.textPrimary ?: 0xFFFFFFFF.toInt())
            
            setTextSize(TypedValue.COMPLEX_UNIT_PX, textPaint.textSize)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 0)
            
            imeOptions = EditorInfo.IME_ACTION_DONE
            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    stopInlineEditing()
                    true
                } else false
            }
            
            setOnKeyListener { _, keyCode, event ->
                if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                    stopInlineEditing()
                    true
                } else false
            }
        }

        inlineEditText = et
        if (host is ViewGroup) {
            host.addView(et)
            updateEtPosition(et)
            et.requestFocus()
            et.post {
                val imm = host.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(et, InputMethodManager.SHOW_IMPLICIT)
            }
        }

        val l: (Float) -> Unit = { _: Float ->
            val curEt = inlineEditText
            if (curEt != null) {
                updateEtPosition(curEt)
            }
        }
        scrollListener = l
        list.addOnScrollListener(l)
    }

    private fun updateEtPosition(et: EditText) {
        val lp = et.layoutParams as? FrameLayout.LayoutParams ?: FrameLayout.LayoutParams(0, 0)
        lp.width = bounds.width().toInt()
        lp.height = itemHeight.toInt()
        
        val etTop = list.bounds.top + itemTop + bounds.centerY() - list.scrollY - itemHeight / 2f
        lp.leftMargin = (list.bounds.left + bounds.left).toInt()
        lp.topMargin = etTop.toInt()
        et.layoutParams = lp

        et.visibility = if (etTop + itemHeight < list.bounds.top || etTop > list.bounds.bottom) {
            android.view.View.GONE
        } else {
            android.view.View.VISIBLE
        }
    }

    private fun stopInlineEditing() {
        if (!isEditing) return
        isEditing = false
        
        scrollListener?.let { list.removeOnScrollListener(it) }
        scrollListener = null
        
        inlineEditText?.let { et ->
            val newVal = et.text.toString().toIntOrNull()
            if (newVal != null && newVal in minValue..maxValue) {
                setValueInternal(newVal)
                onValueChange(newVal)
            }
            val imm = host.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(et.windowToken, 0)
            if (host is ViewGroup) {
                host.removeView(et)
            }
        }
        inlineEditText = null
        host.invalidate()
    }
}
