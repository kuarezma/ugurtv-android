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
import com.ugur.iptv.data.MovieItem

class MovieAdapter(
    private val onMovieFocused: (movie: MovieItem, position: Int, totalCount: Int) -> Unit,
    private val onMovieClicked: (MovieItem) -> Unit
) : RecyclerView.Adapter<MovieAdapter.MovieViewHolder>() {

    private val movies = mutableListOf<MovieItem>()

    fun submitList(newList: List<MovieItem>) {
        movies.clear()
        movies.addAll(newList)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovieViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_movie_card, parent, false)
        return MovieViewHolder(view)
    }

    override fun onBindViewHolder(holder: MovieViewHolder, position: Int) {
        holder.bind(movies[position])
    }

    override fun getItemCount(): Int = movies.size

    inner class MovieViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivPoster: ImageView = itemView.findViewById(R.id.ivMoviePoster)
        private val tvRating: TextView = itemView.findViewById(R.id.tvMovieRating)
        private val tvTitle: TextView = itemView.findViewById(R.id.tvMovieTitle)
        private val tvYear: TextView = itemView.findViewById(R.id.tvMovieYear)
        private val ivPlayBadge: ImageView = itemView.findViewById(R.id.ivMoviePlayBadge)

        init {
            itemView.clipToOutline = true
        }

        fun bind(movie: MovieItem) {
            tvTitle.text = movie.stream.name
            tvRating.text = movie.ratingFormatted
            tvYear.text = if (movie.year.isNotBlank()) movie.year else movie.categoryName

            val posterUrl = movie.stream.streamIcon
            if (!posterUrl.isNullOrBlank()) {
                try {
                    Glide.with(itemView.context)
                        .load(posterUrl)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .placeholder(R.drawable.ic_channel_placeholder)
                        .error(R.drawable.ic_channel_placeholder)
                        .into(ivPoster)
                } catch (e: Exception) {
                    ivPoster.setImageResource(R.drawable.ic_channel_placeholder)
                }
            } else {
                ivPoster.setImageResource(R.drawable.ic_channel_placeholder)
            }

            itemView.setOnClickListener {
                onMovieClicked(movie)
            }

            itemView.setOnFocusChangeListener { view, hasFocus ->
                view.isSelected = hasFocus
                if (hasFocus) {
                    ivPlayBadge.visibility = View.VISIBLE
                    tvTitle.setTextColor(Color.parseColor("#00F2FE"))
                    view.animate().scaleX(1.10f).scaleY(1.10f).translationZ(20f).setDuration(150).start()
                    val pos = bindingAdapterPosition
                    if (pos != RecyclerView.NO_POSITION) {
                        onMovieFocused(movie, pos, movies.size)
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
