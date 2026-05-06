package com.vboard.aac.debug

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.vboard.aac.R

/**
 * Debug-only activity that previews every Phase 2 component primitive on one
 * scrollable screen. Launch via:
 *   adb shell am start -a com.vboard.aac.debug.GALLERY
 *
 * Not present in release builds.
 */
class DebugStyleGalleryActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_VBoard_New)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_debug_gallery)
    }
}
