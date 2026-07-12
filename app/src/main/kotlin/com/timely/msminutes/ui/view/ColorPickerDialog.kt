package com.timely.msminutes.ui.view

import android.app.Dialog
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.BaseAdapter
import android.widget.GridView
import com.timely.msminutes.R

class ColorPickerDialog(context: Context, private val listener: OnColorSelectedListener) :
    Dialog(context) {
    interface OnColorSelectedListener {
        fun onColorSelected(color: Int)
    }

    private val colors = intArrayOf(
        -0x6800, -0xbbcca, -0x16e19d, -0x63d850,
        -0x98c549, -0xc0ae4b, -0xde690d, -0xfc560c,
        -0xff432c, -0xff6978, -0xb350b0, -0x743cb6,
        -0x3223c7, -0x14c5, -0x3ef9, -0xa8de,
        -0x86aab8, -0x616162, -0x9f8275, -0x1000000
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_color_picker)
        val gridView = findViewById<GridView>(R.id.grid_colors)
        gridView.setAdapter(ColorAdapter())
        gridView.setOnItemClickListener(OnItemClickListener { parent: AdapterView<*>?, view: View?, position: Int, id: Long ->
            listener.onColorSelected(colors[position])
            dismiss()
        })
    }

    private inner class ColorAdapter : BaseAdapter() {
        override fun getCount(): Int {
            return colors.size
        }

        override fun getItem(position: Int): Any {
            return colors[position]
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            var convertView = convertView
            if (convertView == null) {
                convertView = View(getContext())
                val size =
                    getContext().getResources().getDimensionPixelSize(R.dimen.color_swatch_size)
                convertView.setLayoutParams(ViewGroup.LayoutParams(size, size))
                convertView.setBackgroundResource(R.drawable.bg_color_preview)
            }
            val gd = convertView.getBackground() as GradientDrawable
            gd.setColor(colors[position])
            return convertView
        }
    }
}
