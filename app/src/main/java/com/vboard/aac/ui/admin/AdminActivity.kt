package com.vboard.aac.ui.admin

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.vboard.aac.R
import com.vboard.aac.databinding.ActivityAdminBinding
import com.vboard.aac.ui.backup.BackupActivity
import com.vboard.aac.ui.edit.EditActivity
import com.vboard.aac.ui.uiconfig.UISettingsActivity
import com.vboard.aac.ui.stats.StatsActivity
import com.vboard.aac.ui.voicetest.VoiceSettingsActivity

class AdminActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupAdminCards()
        setupListeners()
    }

    private fun setupAdminCards() {
        setupCard(
            binding.cardVocab.root,
            R.drawable.bg_icon_circle,
            android.R.drawable.ic_menu_agenda,
            "Quản lý từ vựng",
            "Thêm, sửa, xóa thẻ từ vựng"
        ) { startActivity(Intent(this, EditActivity::class.java)) }

        setupCard(
            binding.cardVoice.root,
            R.drawable.bg_icon_circle,
            android.R.drawable.ic_lock_silent_mode_off,
            "Cài đặt giọng đọc",
            "Âm lượng, giọng nói"
        ) { startActivity(Intent(this, VoiceSettingsActivity::class.java)) }

        setupCard(
            binding.cardUI.root,
            R.drawable.bg_icon_circle,
            android.R.drawable.ic_menu_manage,
            "Cài đặt giao diện",
            "Chế độ sáng/tối, lưới màn hình"
        ) { startActivity(Intent(this, UISettingsActivity::class.java)) }

        setupCard(
            binding.cardStats.root,
            R.drawable.bg_icon_circle,
            android.R.drawable.ic_menu_sort_by_size,
            "Thống kê sử dụng",
            "Xem lịch sử giao tiếp"
        ) { startActivity(Intent(this, StatsActivity::class.java)) }

        setupCard(
            binding.cardBackup.root,
            R.drawable.bg_icon_circle,
            android.R.drawable.ic_menu_save,
            "Sao lưu & Khôi phục",
            "Xuất/Nhập kho từ vựng JSON"
        ) { startActivity(Intent(this, BackupActivity::class.java)) }
    }

    private fun setupCard(
        cardView: View,
        iconBg: Int,
        iconRes: Int,
        title: String,
        subtitle: String,
        onClick: () -> Unit
    ) {
        cardView.findViewById<LinearLayout>(R.id.iconContainer)
            .setBackgroundResource(iconBg)
        cardView.findViewById<ImageView>(R.id.icon)
            .setImageResource(iconRes)
        cardView.findViewById<TextView>(R.id.title).text = title
        cardView.findViewById<TextView>(R.id.subtitle).text = subtitle
        cardView.setOnClickListener { onClick() }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener { finish() }
    }
}
