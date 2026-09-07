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
import com.ugur.iptv.data.ChannelItem

class QuickChannelAdapter(
    private val onChannelSelected: (ChannelItem) -> Unit
) : RecyclerView.Adapter<QuickChannelAdapter.QuickViewHolder>() {

    private val channels = mutableListOf<ChannelItem>()

    fun submitList(newList: List<ChannelItem>) {
        channels.clear()
        channels.addAll(newList)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuickViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_channel_quick, parent, false)
        return QuickViewHolder(view)
    }

    override fun onBindViewHolder(holder: QuickViewHolder, position: Int) {
        holder.bind(channels[position])
    }

    override fun getItemCount(): Int = channels.size

    inner class QuickViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvNum: TextView = itemView.findViewById(R.id.tvQuickNum)
        private val ivLogo: ImageView = itemView.findViewById(R.id.ivQuickLogo)
        private val tvTitle: TextView = itemView.findViewById(R.id.tvQuickTitle)

        fun bind(channel: ChannelItem) {
            tvNum.text = channel.index.toString()
            tvTitle.text = channel.stream.name

            val iconUrl = channel.stream.streamIcon
            if (!iconUrl.isNullOrBlank()) {
                Glide.with(itemView.context)
                    .load(iconUrl)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.ic_channel_placeholder)
                    .error(R.drawable.ic_channel_placeholder)
                    .into(ivLogo)
            } else {
                ivLogo.setImageResource(R.drawable.ic_channel_placeholder)
            }

            itemView.setOnClickListener {
                onChannelSelected(channel)
            }
        }
    }
}
