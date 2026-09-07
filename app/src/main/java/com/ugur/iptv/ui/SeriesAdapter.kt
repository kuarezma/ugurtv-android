package com.ugur.iptv.ui

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
    private val onSeriesFocused: (SeriesItem) -> Unit,
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

        fun bind(series: SeriesItem) {
            tvTitle.text = series.name
            tvRating.text = series.rating?.toString() ?: "8.0"
            tvGenre.text = series.genre ?: "Dizi"

            val coverUrl = series.cover
            if (!coverUrl.isNullOrBlank()) {
                Glide.with(itemView.context)
                    .load(coverUrl)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.ic_channel_placeholder)
                    .error(R.drawable.ic_channel_placeholder)
                    .into(ivCover)
            } else {
                ivCover.setImageResource(R.drawable.ic_channel_placeholder)
            }

            itemView.setOnClickListener {
                onSeriesClicked(series)
            }

            itemView.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    onSeriesFocused(series)
                }
            }
        }
    }
}
