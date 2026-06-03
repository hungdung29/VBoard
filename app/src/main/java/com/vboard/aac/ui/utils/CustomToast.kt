package com.vboard.aac.ui.utils

import android.content.Context
import android.view.LayoutInflater
import android.widget.TextView
import android.widget.Toast
import com.vboard.aac.R

object CustomToast {
    fun show(context: Context, message: String, duration: Int = Toast.LENGTH_SHORT) {
        try {
            val inflater = LayoutInflater.from(context)
            val layout = inflater.inflate(R.layout.layout_custom_toast, null)
            val text = layout.findViewById<TextView>(R.id.toastMessage)
            text.text = message

            val toast = Toast(context.applicationContext).apply {
                this.duration = duration
                view = layout
                setGravity(android.view.Gravity.BOTTOM or android.view.Gravity.CENTER_HORIZONTAL, 0, 150)
            }
            toast.show()
        } catch (e: Exception) {
            // Fallback to standard toast if custom toast fails
            Toast.makeText(context, message, duration).show()
        }
    }
}
