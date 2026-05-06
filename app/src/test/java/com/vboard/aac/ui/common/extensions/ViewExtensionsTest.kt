package com.vboard.aac.ui.common.extensions

import android.view.View
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ViewExtensionsTest {

    @Test fun `animatePressIn scales the view to 0_96`() {
        val view = View(ApplicationProvider.getApplicationContext())
        view.animatePressIn()
        assertEquals(0.96f, view.scaleX, 0.001f)
        assertEquals(0.96f, view.scaleY, 0.001f)
    }

    @Test fun `animatePressOut returns scale to 1_0`() {
        val view = View(ApplicationProvider.getApplicationContext()).apply {
            scaleX = 0.96f
            scaleY = 0.96f
        }
        view.animatePressOut()
        assertEquals(1f, view.scaleX, 0.001f)
        assertEquals(1f, view.scaleY, 0.001f)
    }

    @Test fun `fadeSlideInFromRight sets translationX and alpha pre-animation`() {
        val view = View(ApplicationProvider.getApplicationContext())
        view.fadeSlideInFromRight()
        // The animator hasn't started yet — initial state is set synchronously.
        // After animation completes, translationX → 0 and alpha → 1.
        // Without driving the animator we just verify the pre-anim state is set:
        assertEquals(0f, view.alpha, 0.001f)
    }
}
