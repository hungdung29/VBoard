package com.vboard.aac.ui.common

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import com.google.android.material.card.MaterialCardView
import com.vboard.aac.R
import com.vboard.aac.ui.common.extensions.animatePressIn
import com.vboard.aac.ui.common.extensions.animatePressOut

/**
 * Card view with category-aware tinting and tap-press animation.
 *
 * Set the category via the `app:vbCategory` XML attribute or [setCategoryCode]
 * at runtime. Adapters tint the background using [CategoryTinter] mapped to the
 * resolved category.
 */
class VBoardCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialCardView(context, attrs, defStyleAttr) {

    enum class Category(val code: String) {
        NONE("none"),
        FOOD("food"),
        FAMILY("family"),
        EMOTION("emotion"),
        ACTIVITY("activity"),
        OBJECT("object"),
        PLACE("place");

        companion object {
            fun from(code: String?): Category =
                values().firstOrNull { it.code == code } ?: NONE
        }
    }

    var category: Category = Category.NONE

    fun setCategoryCode(code: String?) {
        category = Category.from(code)
    }

    init {
        radius = resources.getDimension(R.dimen.vb_radius_md)
        cardElevation = resources.getDimension(R.dimen.vb_elevation_e1)
        useCompatPadding = true
        isClickable = true
        isFocusable = true

        if (attrs != null) {
            val ta = context.obtainStyledAttributes(attrs, R.styleable.VBoardCardView)
            try {
                val ord = ta.getInt(R.styleable.VBoardCardView_vbCategory, 0)
                category = Category.values().getOrNull(ord) ?: Category.NONE
            } finally {
                ta.recycle()
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> animatePressIn()
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> animatePressOut()
        }
        return super.onTouchEvent(event)
    }
}
