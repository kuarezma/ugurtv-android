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

class ChannelAdapter(
    private val onChannelFocused: (ChannelItem) -> Unit,
    private val onChannelClicked: (ChannelItem) -> Unit,
    private val onFavoriteToggle: (ChannelItem) -> Unit
) : RecyclerView.Adapter<ChannelAdapter.ChannelViewHolder>() {

    private val channels = mutableListOf<ChannelItem>()
    private var selectedIndex = -1

    fun submitList(newList: List<ChannelItem>) {
        channels.clear()
        channels.addAll(newList)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChannelViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_channel, parent, false)
        return ChannelViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChannelViewHolder, position: Int) {
        val channel = channels[position]
        holder.bind(channel, position == selectedIndex)
    }

    override fun getItemCount(): Int = channels.size

    inner class ChannelViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvNum: TextView = itemView.findViewById(R.id.tvChannelNum)
        private val ivLogo: ImageView = itemView.findViewById(R.id.ivChannelLogo)
        private val tvName: TextView = itemView.findViewById(R.id.tvChannelName)
        private val ivFav: ImageView = itemView.findViewById(R.id.ivFavIndicator)

        fun bind(channel: ChannelItem, isSelected: Boolean) {
            tvNum.text = channel.index.toString()
            tvName.text = channel.stream.name
            ivFav.visibility = if (channel.isFavorite) View.VISIBLE else View.GONE
            itemView.isSelected = isSelected

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
                onChannelClicked(channel)
            }

            itemView.setOnFocusChangeListener { view, hasFocus ->
                view.isSelected = hasFocus
                if (hasFocus) {
                    val pos = bindingAdapterPosition
                    if (pos != RecyclerView.NO_POSITION) {
                        selectedIndex = pos
                    }
                    onChannelFocused(channel)
                }
            }

            itemView.setOnLongClickListener {
                onFavoriteToggle(channel)
                true
            }
        }
    }
}
