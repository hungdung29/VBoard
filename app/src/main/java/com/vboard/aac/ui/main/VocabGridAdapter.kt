package com.vboard.aac.ui.main

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.vboard.aac.databinding.ItemVocabCardBinding
import com.vboard.aac.domain.model.VocabCard

class VocabGridAdapter(
    private val onCardClick: (VocabCard) -> Unit
) : ListAdapter<VocabCard, VocabGridAdapter.VocabViewHolder>(VocabDiffCallback()) {

    private var showLabels = true

    fun setShowLabels(show: Boolean) {
        showLabels = show
        notifyItemRangeChanged(0, itemCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VocabViewHolder {
        val binding = ItemVocabCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return VocabViewHolder(binding)
    }

    override fun onBindViewHolder(holder: VocabViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class VocabViewHolder(
        private val binding: ItemVocabCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.vocabCard.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    onCardClick(getItem(pos))
                }
            }
        }

        fun bind(card: VocabCard) {
            binding.wordLabel.text = card.word
            binding.wordLabel.visibility = if (showLabels) {
                android.view.View.VISIBLE
            } else {
                android.view.View.GONE
            }

            if (!card.localImagePath.isNullOrEmpty()) {
                binding.cardImage.visibility = android.view.View.VISIBLE
                binding.emojiText.visibility = android.view.View.GONE
                binding.cardImage.load(Uri.parse(card.localImagePath))
            } else {
                binding.cardImage.visibility = android.view.View.GONE
                binding.emojiText.visibility = android.view.View.VISIBLE
                val emoji = EMOJI_MAP[card.word] ?: "📝"
                binding.emojiText.text = emoji
            }

            // Accessibility
            binding.vocabCard.contentDescription = binding.root.context.getString(
                com.vboard.aac.R.string.cd_vocab_card, card.word
            )
        }
    }

    class VocabDiffCallback : DiffUtil.ItemCallback<VocabCard>() {
        override fun areItemsTheSame(oldItem: VocabCard, newItem: VocabCard) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: VocabCard, newItem: VocabCard) =
            oldItem == newItem
    }

    companion object {
        val EMOJI_MAP = mapOf(
            "Mẹ" to "👩", "Ba" to "👨", "Em" to "👧", "Ông" to "👴", "Bà" to "👵",
            "Con" to "👶", "Anh" to "🧑", "Chị" to "👩‍🦱",
            "Nước" to "💧", "Cơm" to "🍚", "Bánh" to "🍰", "Sữa" to "🥛",
            "Trái cây" to "🍎", "Thịt" to "🥩", "Cá" to "🐟", "Rau" to "🥬",
            "Trà" to "🍵", "Bánh mì" to "🥖",
            "Nhà" to "🏠", "Phòng" to "🚪", "Giường" to "🛏️", "Cửa" to "🚪",
            "Cửa sổ" to "🪟", "Bếp" to "🍳", "Tivi" to "📺",
            "Chơi" to "🎮", "Đồ chơi" to "🧸", "Bóng" to "⚽", "Sách" to "📖",
            "Đi dạo" to "🚶", "Bơi" to "🏊", "Nhảy" to "🕺", "Hát" to "🎵",
            "Vui" to "😊", "Buồn" to "😢", "Sợ" to "😨", "Mệt" to "😫",
            "Đói" to "😫", "Khát" to "🥤", "Nóng" to "🔥", "Lạnh" to "❄️", "Đau" to "🤕",
            "Muốn" to "💭", "Cần" to "✋", "Đi" to "🚶", "Ngủ" to "😴",
            "Tắm" to "🚿", "Mặc" to "👕", "Đi học" to "🏫", "Xem" to "👀",
            "Bút" to "✏️", "Giấy" to "📄", "Bảng" to "📋",
            "Điện thoại" to "📱", "Máy tính" to "💻", "Ô tô" to "🚗",
            "Trường" to "🏫", "Bệnh viện" to "🏥", "Công viên" to "🌳",
            "Siêu thị" to "🛒", "Biển" to "🌊"
        )
    }
}
