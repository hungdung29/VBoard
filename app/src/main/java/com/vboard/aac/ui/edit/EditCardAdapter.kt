package com.vboard.aac.ui.edit

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.vboard.aac.R
import com.vboard.aac.databinding.ItemEditCardBinding
import com.vboard.aac.domain.model.VocabCard
import com.vboard.aac.ui.main.VocabGridAdapter

class EditCardAdapter(
    private val onEditClick: (VocabCard) -> Unit,
    private val onDeleteClick: (VocabCard) -> Unit
) : ListAdapter<VocabCard, EditCardAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemEditCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemEditCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.btnEdit.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    onEditClick(getItem(pos))
                }
            }
            binding.btnDelete.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    onDeleteClick(getItem(pos))
                }
            }
            binding.editCard.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    onEditClick(getItem(pos))
                }
            }
        }

        fun bind(card: VocabCard) {
            binding.wordText.text = card.word
            val emoji = VocabGridAdapter.EMOJI_MAP[card.word] ?: "📝"
            binding.emojiText.text = emoji
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<VocabCard>() {
        override fun areItemsTheSame(oldItem: VocabCard, newItem: VocabCard) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: VocabCard, newItem: VocabCard) =
            oldItem == newItem
    }
}
