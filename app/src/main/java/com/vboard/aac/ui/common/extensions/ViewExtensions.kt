package com.vboard.aac.ui.common.extensions

import android.view.View

private const val PRESS_SCALE = 0.96f
private const val PRESS_DURATION = 100L
private const val FAST_DURATION = 200L
private const val SLIDE_OFFSET_DP = 30f

fun View.animatePressIn() {
    animate().cancel()
    scaleX = PRESS_SCALE
    scaleY = PRESS_SCALE
    animate().scaleX(PRESS_SCALE).scaleY(PRESS_SCALE)
        .setDuration(PRESS_DURATION)
        .start()
}

fun View.animatePressOut() {
    animate().cancel()
    scaleX = 1f
    scaleY = 1f
    animate().scaleX(1f).scaleY(1f)
        .setDuration(PRESS_DURATION)
        .start()
}

/** Slides view in from +30dp on the right while fading from 0 to 1. */
fun View.fadeSlideInFromRight() {
    val offset = SLIDE_OFFSET_DP * resources.displayMetrics.density
    translationX = offset
    alpha = 0f
    animate().translationX(0f).alpha(1f)
        .setDuration(FAST_DURATION)
        .start()
}

/** Fades the view out, then runs onEnd. Caller is responsible for removal afterwards. */
fun View.fadeOut(onEnd: () -> Unit) {
    animate().cancel()
    animate().alpha(0f)
        .setDuration(FAST_DURATION)
        .withEndAction(onEnd)
        .start()
}
