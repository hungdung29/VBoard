package com.vboard.aac.ui.settings

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.vboard.aac.R
import com.vboard.aac.ui.admin.AdminActivity
import com.vboard.aac.ui.pin.PinActivity

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        setupListeners()
    }

    private fun setupListeners() {
        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<View>(R.id.btnClose).setOnClickListener { finish() }
        findViewById<View>(R.id.btnOpenPin).setOnClickListener {
            startActivity(Intent(this, PinActivity::class.java))
        }
    }
}
