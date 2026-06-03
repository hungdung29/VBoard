package com.vboard.aac.ui.edit

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.vboard.aac.R
import com.vboard.aac.databinding.ItemEditCardBinding
import com.vboard.aac.domain.model.VocabCard
import com.vboard.aac.ui.main.VocabGridAdapter

class EditCardAdapter(
    private val onEditClick: (VocabCard) -> Unit,
    private val onDeleteClick: (VocabCard) -> Unit
) : RecyclerView.Adapter<EditCardAdapter.ViewHolder>() {

    private val cards = mutableListOf<VocabCard>()

    fun submitList(newList: List<VocabCard>) {
        val diffCallback = object : DiffUtil.Callback() {
            override fun getOldListSize(): Int = cards.size
            override fun getNewListSize(): Int = newList.size

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return cards[oldItemPosition].id == newList[newItemPosition].id
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return cards[oldItemPosition] == newList[newItemPosition]
            }
        }
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        cards.clear()
        cards.addAll(newList)
        diffResult.dispatchUpdatesTo(this)
    }

    fun getCards(): List<VocabCard> = cards

    fun moveItem(fromPosition: Int, toPosition: Int) {
        val item = cards.removeAt(fromPosition)
        cards.add(toPosition, item)
        notifyItemMoved(fromPosition, toPosition)
    }

    override fun getItemCount(): Int = cards.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemEditCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(cards[position])
    }

    inner class ViewHolder(
        private val binding: ItemEditCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.btnEdit.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    onEditClick(cards[pos])
                }
            }
            binding.btnDelete.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    onDeleteClick(cards[pos])
                }
            }
            binding.editCard.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    onEditClick(cards[pos])
                }
            }
        }

        fun bind(card: VocabCard) {
            binding.wordText.text = card.word
            if (!card.localImagePath.isNullOrEmpty()) {
                binding.cardImage.visibility = View.VISIBLE
                binding.emojiText.visibility = View.GONE
                binding.cardImage.load(Uri.parse(card.localImagePath))
            } else {
                binding.cardImage.visibility = View.GONE
                binding.emojiText.visibility = View.VISIBLE
                val emoji = VocabGridAdapter.EMOJI_MAP[card.word] ?: "📝"
                binding.emojiText.text = emoji
            }
        }
    }
}
