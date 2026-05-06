package com.vboard.aac.ui.main

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.vboard.aac.R
import com.vboard.aac.databinding.ItemCategoryChipBinding
import com.vboard.aac.domain.model.Category

class CategoryChipAdapter(
    private val onCategoryClick: (String?) -> Unit
) : ListAdapter<Category, CategoryChipAdapter.CategoryViewHolder>(CategoryDiffCallback()) {

    private var activeCategoryId: String? = null

    fun setActiveCategory(categoryId: String?) {
        val oldActive = activeCategoryId
        activeCategoryId = categoryId
        if (oldActive != categoryId) {
            notifyDataSetChanged()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemCategoryChipBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return CategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CategoryViewHolder(
        private val binding: ItemCategoryChipBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.chipContainer.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    val cat = getItem(pos)
                    val newId = if (cat.id == activeCategoryId) null else cat.id
                    setActiveCategory(newId)
                    onCategoryClick(newId)
                }
            }
        }

        fun bind(category: Category) {
            binding.chipIcon.text = category.icon
            binding.chipText.text = category.name

            val isActive = category.id == activeCategoryId
            val bgColor = if (isActive) {
                ContextCompat.getColor(binding.root.context, R.color.primary_container)
            } else {
                ContextCompat.getColor(binding.root.context, R.color.surface_container)
            }
            binding.chipContainer.setBackgroundColor(bgColor)

            val textColor = if (isActive) {
                ContextCompat.getColor(binding.root.context, R.color.on_primary_container)
            } else {
                ContextCompat.getColor(binding.root.context, R.color.on_surface_variant)
            }
            binding.chipText.setTextColor(textColor)
        }
    }

    class CategoryDiffCallback : DiffUtil.ItemCallback<Category>() {
        override fun areItemsTheSame(oldItem: Category, newItem: Category) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Category, newItem: Category) =
            oldItem == newItem
    }
}
