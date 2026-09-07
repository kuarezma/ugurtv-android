package com.ugur.iptv.ui

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.ugur.iptv.R
import com.ugur.iptv.data.SeriesItem

class SeriesAdapter(
    private val onSeriesFocused: (series: SeriesItem, position: Int, totalCount: Int) -> Unit,
    private val onSeriesClicked: (SeriesItem) -> Unit
) : RecyclerView.Adapter<SeriesAdapter.SeriesViewHolder>() {

    private val seriesList = mutableListOf<SeriesItem>()

    fun submitList(newList: List<SeriesItem>) {
        seriesList.clear()
        seriesList.addAll(newList)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SeriesViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_series_card, parent, false)
        return SeriesViewHolder(view)
    }

    override fun onBindViewHolder(holder: SeriesViewHolder, position: Int) {
        holder.bind(seriesList[position])
    }

    override fun getItemCount(): Int = seriesList.size

    inner class SeriesViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivCover: ImageView = itemView.findViewById(R.id.ivSeriesCover)
        private val tvRating: TextView = itemView.findViewById(R.id.tvSeriesRating)
        private val tvTitle: TextView = itemView.findViewById(R.id.tvSeriesTitle)
        private val tvGenre: TextView = itemView.findViewById(R.id.tvSeriesGenre)
        private val ivPlayBadge: ImageView = itemView.findViewById(R.id.ivSeriesPlayBadge)

        init {
            itemView.clipToOutline = true
        }

        fun bind(series: SeriesItem) {
            tvTitle.text = series.name
            tvRating.text = series.rating?.toString() ?: "8.0"
            tvGenre.text = series.genre ?: "Dizi"

            val coverUrl = series.cover
            if (!coverUrl.isNullOrBlank()) {
                try {
                    Glide.with(itemView.context)
                        .load(coverUrl)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .placeholder(R.drawable.ic_channel_placeholder)
                        .error(R.drawable.ic_channel_placeholder)
                        .into(ivCover)
                } catch (e: Exception) {
                    ivCover.setImageResource(R.drawable.ic_channel_placeholder)
                }
            } else {
                ivCover.setImageResource(R.drawable.ic_channel_placeholder)
            }

            itemView.setOnClickListener {
                onSeriesClicked(series)
            }

            itemView.setOnFocusChangeListener { view, hasFocus ->
                view.isSelected = hasFocus
                if (hasFocus) {
                    ivPlayBadge.visibility = View.VISIBLE
                    tvTitle.setTextColor(Color.parseColor("#00F2FE"))
                    view.animate().scaleX(1.10f).scaleY(1.10f).translationZ(20f).setDuration(150).start()
                    val pos = bindingAdapterPosition
                    if (pos != RecyclerView.NO_POSITION) {
                        onSeriesFocused(series, pos, seriesList.size)
                    }
                } else {
                    ivPlayBadge.visibility = View.GONE
                    tvTitle.setTextColor(Color.WHITE)
                    view.animate().scaleX(1.0f).scaleY(1.0f).translationZ(0f).setDuration(150).start()
                }
            }
        }
    }
}
