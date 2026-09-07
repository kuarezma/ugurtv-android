package com.ugur.iptv.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ugur.iptv.R
import com.ugur.iptv.data.LiveCategory

class CategoryAdapter(
    private val onCategorySelected: (LiveCategory) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    private val categories = mutableListOf<LiveCategory>()
    private var selectedPosition = 0

    fun submitList(newList: List<LiveCategory>) {
        categories.clear()
        categories.addAll(newList)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_category, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = categories[position]
        holder.bind(category, position == selectedPosition)
    }

    override fun getItemCount(): Int = categories.size

    inner class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tvCategoryName)
        private val tvCount: TextView = itemView.findViewById(R.id.tvCategoryCount)

        fun bind(category: LiveCategory, isSelected: Boolean) {
            tvName.text = category.categoryName
            tvCount.text = if (category.channelCount > 0) category.channelCount.toString() else ""
            itemView.isSelected = isSelected

            itemView.setOnClickListener {
                val oldPos = selectedPosition
                selectedPosition = bindingAdapterPosition
                notifyItemChanged(oldPos)
                notifyItemChanged(selectedPosition)
                onCategorySelected(category)
            }

            itemView.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    val oldPos = selectedPosition
                    selectedPosition = bindingAdapterPosition
                    notifyItemChanged(oldPos)
                    notifyItemChanged(selectedPosition)
                    onCategorySelected(category)
                }
            }
        }
    }
}
