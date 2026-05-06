package com.vboard.aac.ui.common

import com.vboard.aac.R
import org.junit.Assert.assertEquals
import org.junit.Test

class CategoryTinterTest {

    @Test fun `food maps to food colors`() {
        val pair = CategoryTinter.colorsFor("food")
        assertEquals(R.color.vb_category_food_bg, pair.bgColorRes)
        assertEquals(R.color.vb_category_food_label, pair.labelColorRes)
    }

    @Test fun `family maps to family colors`() {
        val pair = CategoryTinter.colorsFor("family")
        assertEquals(R.color.vb_category_family_bg, pair.bgColorRes)
        assertEquals(R.color.vb_category_family_label, pair.labelColorRes)
    }

    @Test fun `emotion maps to emotion colors`() {
        val pair = CategoryTinter.colorsFor("emotion")
        assertEquals(R.color.vb_category_emotion_bg, pair.bgColorRes)
        assertEquals(R.color.vb_category_emotion_label, pair.labelColorRes)
    }

    @Test fun `unknown code falls back to neutral`() {
        val pair = CategoryTinter.colorsFor("xyz")
        assertEquals(R.color.vb_secondary_system_background, pair.bgColorRes)
        assertEquals(R.color.vb_label, pair.labelColorRes)
    }

    @Test fun `null code falls back to neutral`() {
        val pair = CategoryTinter.colorsFor(null)
        assertEquals(R.color.vb_secondary_system_background, pair.bgColorRes)
        assertEquals(R.color.vb_label, pair.labelColorRes)
    }
}
