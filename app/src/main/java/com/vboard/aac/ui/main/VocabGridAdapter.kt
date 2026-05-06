package com.vboard.aac.ui.main

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.vboard.aac.databinding.ItemVocabCardBinding
import com.vboard.aac.ui.common.CategoryTinter

class VocabGridAdapter(
    private val onCardClick: (VocabCardUiItem) -> Unit,
) : ListAdapter<VocabCardUiItem, VocabGridAdapter.VocabViewHolder>(DiffCallback()) {

    private var showLabels = true

    fun setShowLabels(show: Boolean) {
        if (showLabels == show) return
        showLabels = show
        notifyItemRangeChanged(0, itemCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VocabViewHolder {
        val binding = ItemVocabCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false,
        )
        return VocabViewHolder(binding)
    }

    override fun onBindViewHolder(holder: VocabViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class VocabViewHolder(
        private val binding: ItemVocabCardBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.vocabCard.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) onCardClick(getItem(pos))
            }
        }

        fun bind(item: VocabCardUiItem) {
            val ctx = binding.root.context
            val (bgRes, labelRes) = CategoryTinter.colorsFor(item.categoryCode)

            binding.vocabCard.setCategoryCode(item.categoryCode)
            binding.vocabCard.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(ctx, bgRes),
            )

            binding.emojiText.text = item.emoji
            binding.wordLabel.text = item.word
            binding.wordLabel.setTextColor(ContextCompat.getColor(ctx, labelRes))
            binding.wordLabel.visibility = if (showLabels) View.VISIBLE else View.GONE
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<VocabCardUiItem>() {
        override fun areItemsTheSame(oldItem: VocabCardUiItem, newItem: VocabCardUiItem) =
            oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: VocabCardUiItem, newItem: VocabCardUiItem) =
            oldItem == newItem
    }
}
