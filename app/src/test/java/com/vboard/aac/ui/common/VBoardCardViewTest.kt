package com.vboard.aac.ui.common

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class VBoardCardViewTest {

    @Test fun `default category is none`() {
        val view = VBoardCardView(ApplicationProvider.getApplicationContext())
        assertEquals(VBoardCardView.Category.NONE, view.category)
    }

    @Test fun `setting category updates state`() {
        val view = VBoardCardView(ApplicationProvider.getApplicationContext())
        view.category = VBoardCardView.Category.FOOD
        assertEquals(VBoardCardView.Category.FOOD, view.category)
    }

    @Test fun `setCategoryCode maps known code to enum`() {
        val view = VBoardCardView(ApplicationProvider.getApplicationContext())
        view.setCategoryCode("emotion")
        assertEquals(VBoardCardView.Category.EMOTION, view.category)
    }

    @Test fun `setCategoryCode falls back to NONE for unknown`() {
        val view = VBoardCardView(ApplicationProvider.getApplicationContext())
        view.setCategoryCode("xyz")
        assertEquals(VBoardCardView.Category.NONE, view.category)
    }

    @Test fun `setCategoryCode handles null`() {
        val view = VBoardCardView(ApplicationProvider.getApplicationContext())
        view.setCategoryCode(null)
        assertEquals(VBoardCardView.Category.NONE, view.category)
    }
}
