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
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    selectedPosition = pos
                }
                onCategorySelected(category)
            }

            itemView.setOnFocusChangeListener { view, hasFocus ->
                view.isSelected = hasFocus
                if (hasFocus) {
                    val pos = bindingAdapterPosition
                    if (pos != RecyclerView.NO_POSITION) {
                        selectedPosition = pos
                    }
                    view.animate().scaleX(1.04f).scaleY(1.04f).translationZ(8f).setDuration(120).start()
                    tvName.setTextColor(android.graphics.Color.parseColor("#00F2FE"))
                    onCategorySelected(category)
                } else {
                    view.animate().scaleX(1.0f).scaleY(1.0f).translationZ(0f).setDuration(120).start()
                    tvName.setTextColor(android.graphics.Color.parseColor("#E0E6ED"))
                }
            }
        }
    }
}
