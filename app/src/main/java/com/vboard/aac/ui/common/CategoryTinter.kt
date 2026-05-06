package com.vboard.aac.ui.common

import androidx.annotation.ColorRes
import com.vboard.aac.R

/**
 * Pure-function map from a category code (e.g. "food", "family") to the
 * (background color, label color) resource IDs defined in the design tokens.
 *
 * Adapters call [colorsFor] in onBind to tint vocab cards / chips / list-row
 * icons. Unknown codes fall back to neutral surface + label color.
 */
object CategoryTinter {

    data class Colors(
        @ColorRes val bgColorRes: Int,
        @ColorRes val labelColorRes: Int,
    )

    fun colorsFor(code: String?): Colors = when (code) {
        "food"     -> Colors(R.color.vb_category_food_bg,     R.color.vb_category_food_label)
        "family"   -> Colors(R.color.vb_category_family_bg,   R.color.vb_category_family_label)
        "emotion"  -> Colors(R.color.vb_category_emotion_bg,  R.color.vb_category_emotion_label)
        "activity" -> Colors(R.color.vb_category_activity_bg, R.color.vb_category_activity_label)
        "object"   -> Colors(R.color.vb_category_object_bg,   R.color.vb_category_object_label)
        "place"    -> Colors(R.color.vb_category_place_bg,    R.color.vb_category_place_label)
        else       -> Colors(R.color.vb_secondary_system_background, R.color.vb_label)
    }
}
