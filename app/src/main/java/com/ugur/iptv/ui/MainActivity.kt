package com.ugur.iptv.ui

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.ugur.iptv.R
import com.ugur.iptv.api.ApiClient
import com.ugur.iptv.data.ChannelItem
import com.ugur.iptv.data.LiveCategory
import com.ugur.iptv.data.LiveStream
import com.ugur.iptv.data.MovieItem
import com.ugur.iptv.data.PreferencesManager
import com.ugur.iptv.data.SeriesItem
import com.ugur.iptv.data.VodStream
import com.ugur.iptv.databinding.ActivityMainBinding
import com.ugur.iptv.player.LowLatencyPlayerManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: PreferencesManager
    private lateinit var playerManager: LowLatencyPlayerManager

    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var channelAdapter: ChannelAdapter
    private lateinit var movieAdapter: MovieAdapter
    private lateinit var seriesAdapter: SeriesAdapter

    private val allCategories = mutableListOf<LiveCategory>()
    private val allChannels = mutableListOf<ChannelItem>()
    private val currentFilteredChannels = mutableListOf<ChannelItem>()

    private val allMovies = mutableListOf<MovieItem>()
    private val allSeries = mutableListOf<SeriesItem>()

    private var activeCategory: LiveCategory? = null
    private var currentlyPlayingChannel: ChannelItem? = null
    private var currentHeroMovie: MovieItem? = null
    private var currentHeroSeries: SeriesItem? = null
    private var isDemoMode: Boolean = false

    private var previewPlayJob: Job? = null

    companion object {
        var channelRepository: List<ChannelItem> = emptyList()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = PreferencesManager(this)
        isDemoMode = intent.getBooleanExtra("EXTRA_IS_DEMO", false)

        initPlayer()
        setupTopNavigation()
        setupRecyclerViews()
        setupListeners()
        updateHeaderClock()

        loadData()
    }

    private fun updateHeaderClock() {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        binding.tvHeaderClock.text = sdf.format(Date())
    }

    private fun initPlayer() {
        playerManager = LowLatencyPlayerManager(this)
        playerManager.attachToView(binding.previewPlayerView)

        playerManager.onBufferingStateChanged = { isBuffering ->
            binding.pbPreviewLoading.visibility = if (isBuffering) View.VISIBLE else View.GONE
        }

        playerManager.onErrorOccurred = { _ ->
            binding.pbPreviewLoading.visibility = View.GONE
        }
    }

    private fun setupTopNavigation() {
        binding.tabLiveTv.setOnClickListener { switchTab(TabMode.LIVE_TV) }
        binding.tabMovies.setOnClickListener { switchTab(TabMode.MOVIES) }
        binding.tabSeries.setOnClickListener { switchTab(TabMode.SERIES) }
        binding.tabFavorites.setOnClickListener { switchTab(TabMode.FAVORITES) }
        binding.tabSettings.setOnClickListener { showSettingsDialog() }
    }

    enum class TabMode { LIVE_TV, MOVIES, SERIES, FAVORITES }

    private fun switchTab(tab: TabMode) {
        binding.tabLiveTv.isSelected = (tab == TabMode.LIVE_TV)
        binding.tabMovies.isSelected = (tab == TabMode.MOVIES)
        binding.tabSeries.isSelected = (tab == TabMode.SERIES)
        binding.tabFavorites.isSelected = (tab == TabMode.FAVORITES)

        when (tab) {
            TabMode.LIVE_TV -> {
                binding.sectionLiveTv.visibility = View.VISIBLE
                binding.sectionMovies.visibility = View.GONE
                binding.sectionSeries.visibility = View.GONE
                allCategories.firstOrNull { it.categoryId == "all" }?.let { selectCategory(it) }
            }
            TabMode.MOVIES -> {
                playerManager.pause()
                binding.sectionLiveTv.visibility = View.GONE
                binding.sectionMovies.visibility = View.VISIBLE
                binding.sectionSeries.visibility = View.GONE
                movieAdapter.submitList(allMovies)
                if (allMovies.isNotEmpty() && currentHeroMovie == null) {
                    setHeroMovie(allMovies[0])
                }
            }
            TabMode.SERIES -> {
                playerManager.pause()
                binding.sectionLiveTv.visibility = View.GONE
                binding.sectionMovies.visibility = View.GONE
                binding.sectionSeries.visibility = View.VISIBLE
                seriesAdapter.submitList(allSeries)
                if (allSeries.isNotEmpty() && currentHeroSeries == null) {
                    setHeroSeries(allSeries[0])
                }
            }
            TabMode.FAVORITES -> {
                binding.sectionLiveTv.visibility = View.VISIBLE
                binding.sectionMovies.visibility = View.GONE
                binding.sectionSeries.visibility = View.GONE
                allCategories.firstOrNull { it.categoryId == "favorites" }?.let { selectCategory(it) }
            }
        }
    }

    private fun setupRecyclerViews() {
        // 1. Categories
        categoryAdapter = CategoryAdapter { category ->
            selectCategory(category)
        }
        binding.rvCategories.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = categoryAdapter
            setHasFixedSize(true)
        }

        // 2. Channels
        channelAdapter = ChannelAdapter(
            onChannelFocused = { channel ->
                onChannelPreviewFocused(channel)
            },
            onChannelClicked = { channel ->
                openFullscreen(channel)
            },
            onFavoriteToggle = { channel ->
                val newState = prefs.toggleFavorite(channel.stream.streamId)
                channel.isFavorite = newState
                channelAdapter.notifyDataSetChanged()
                updatePreviewFavoriteState(channel)
                Toast.makeText(
                    this,
                    if (newState) getString(R.string.fav_added) else getString(R.string.fav_removed),
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
        binding.rvChannels.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = channelAdapter
            setHasFixedSize(true)
            itemAnimator = null
        }

        // 3. Movies Grid (5 columns for 65" TV)
        movieAdapter = MovieAdapter(
            onMovieFocused = { movie -> setHeroMovie(movie) },
            onMovieClicked = { movie -> playMovie(movie) }
        )
        binding.rvMovies.apply {
            layoutManager = GridLayoutManager(this@MainActivity, 5)
            adapter = movieAdapter
            setHasFixedSize(true)
        }

        // 4. Series Grid (5 columns)
        seriesAdapter = SeriesAdapter(
            onSeriesFocused = { series -> setHeroSeries(series) },
            onSeriesClicked = { series -> playSeries(series) }
        )
        binding.rvSeries.apply {
            layoutManager = GridLayoutManager(this@MainActivity, 5)
            adapter = seriesAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupListeners() {
        binding.btnExpandFullscreen.setOnClickListener {
            currentlyPlayingChannel?.let { openFullscreen(it) }
        }

        binding.ivPreviewFavorite.setOnClickListener {
            currentlyPlayingChannel?.let { channel ->
                val newState = prefs.toggleFavorite(channel.stream.streamId)
                channel.isFavorite = newState
                channelAdapter.notifyDataSetChanged()
                updatePreviewFavoriteState(channel)
            }
        }

        binding.btnHeroPlayMovie.setOnClickListener {
            currentHeroMovie?.let { playMovie(it) }
        }

        binding.btnHeroPlaySeries.setOnClickListener {
            currentHeroSeries?.let { playSeries(it) }
        }

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterChannels(s?.toString().orEmpty())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setHeroMovie(movie: MovieItem) {
        currentHeroMovie = movie
        binding.tvHeroMovieTitle.text = movie.stream.name
        binding.tvHeroMovieDesc.text = "${movie.categoryName} • IMDb ${movie.ratingFormatted} • ${movie.year}"

        if (!movie.stream.streamIcon.isNullOrBlank()) {
            Glide.with(this)
                .load(movie.stream.streamIcon)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(binding.ivHeroMovieBackdrop)
        }
    }

    private fun setHeroSeries(series: SeriesItem) {
        currentHeroSeries = series
        binding.tvHeroSeriesTitle.text = series.name
        binding.tvHeroSeriesDesc.text = "${series.genre ?: "Dizi"} • IMDb ${series.rating ?: "8.5"}\n${series.plot ?: ""}"

        if (!series.cover.isNullOrBlank()) {
            Glide.with(this)
                .load(series.cover)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(binding.ivHeroSeriesBackdrop)
        }
    }

    private fun playMovie(movie: MovieItem) {
        playerManager.pause()
        val dummyStream = LiveStream(
            num = movie.index,
            name = movie.stream.name,
            streamId = movie.stream.streamId,
            streamIcon = movie.stream.streamIcon
        )
        val channelItem = ChannelItem(
            index = movie.index,
            stream = dummyStream,
            categoryName = movie.categoryName,
            streamUrl = movie.streamUrl,
            isFavorite = movie.isFavorite
        )
        channelRepository = listOf(channelItem)
        val intent = Intent(this, FullscreenPlayerActivity::class.java).apply {
            putExtra("EXTRA_INITIAL_CHANNEL_ID", channelItem.stream.streamId)
        }
        startActivity(intent)
    }

    private fun playSeries(series: SeriesItem) {
        playerManager.pause()
        val dummyStream = LiveStream(
            num = 1,
            name = "${series.name} - Bölüm 1",
            streamId = series.seriesId,
            streamIcon = series.cover
        )
        val url = if (isDemoMode) {
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
        } else {
            ApiClient.buildSeriesStreamUrl(prefs.serverUrl, prefs.username, prefs.password, "${series.seriesId}_1")
        }
        val channelItem = ChannelItem(
            index = 1,
            stream = dummyStream,
            categoryName = series.genre ?: "Dizi",
            streamUrl = url,
            isFavorite = false
        )
        channelRepository = listOf(channelItem)
        val intent = Intent(this, FullscreenPlayerActivity::class.java).apply {
            putExtra("EXTRA_INITIAL_CHANNEL_ID", channelItem.stream.streamId)
        }
        startActivity(intent)
    }

    private fun loadData() {
        if (isDemoMode) {
            loadDemoData()
            return
        }

        lifecycleScope.launch {
            try {
                val service = ApiClient.getService(prefs.serverUrl)
                val (categories, streams, vodStreams, seriesList) = withContext(Dispatchers.IO) {
                    val cats = service.getLiveCategories(prefs.username, prefs.password)
                    val strms = service.getLiveStreams(prefs.username, prefs.password)
                    val vods = try { service.getVodStreams(prefs.username, prefs.password) } catch (e: Exception) { emptyList() }
                    val sers = try { service.getSeries(prefs.username, prefs.password) } catch (e: Exception) { emptyList() }
                    Tuple4(cats, strms, vods, sers)
                }

                populateData(categories, streams, vodStreams, seriesList)
            } catch (e: Exception) {
                Toast.makeText(
                    this@MainActivity,
                    "Sunucuya bağlanılamadı, Test Modu aktif edildi.",
                    Toast.LENGTH_SHORT
                ).show()
                loadDemoData()
            }
        }
    }

    data class Tuple4<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)

    private fun populateData(
        categories: List<LiveCategory>,
        streams: List<LiveStream>,
        vodStreams: List<VodStream>,
        seriesList: List<SeriesItem>
    ) {
        allCategories.clear()
        allChannels.clear()
        allMovies.clear()
        allSeries.clear()

        val catAll = LiveCategory("all", getString(R.string.cat_all), channelCount = streams.size)
        val catFav = LiveCategory("favorites", getString(R.string.cat_favorites))
        allCategories.add(catAll)
        allCategories.add(catFav)
        allCategories.addAll(categories)

        val catMap = categories.associateBy { it.categoryId }
        val favIds = prefs.getFavorites()

        streams.forEachIndexed { idx, st ->
            val catName = catMap[st.categoryId]?.categoryName ?: "Genel"
            val streamUrl = ApiClient.buildLiveStreamUrl(prefs.serverUrl, prefs.username, prefs.password, st.streamId)
            val item = ChannelItem(
                index = idx + 1,
                stream = st,
                categoryName = catName,
                streamUrl = streamUrl,
                isFavorite = favIds.contains(st.streamId.toString())
            )
            allChannels.add(item)
        }

        // Populate Movies
        vodStreams.forEachIndexed { idx, vod ->
            val movieUrl = ApiClient.buildVodStreamUrl(
                prefs.serverUrl,
                prefs.username,
                prefs.password,
                vod.streamId,
                vod.containerExtension ?: "mp4"
            )
            allMovies.add(
                MovieItem(
                    index = idx + 1,
                    stream = vod,
                    categoryName = "Film",
                    streamUrl = movieUrl,
                    ratingFormatted = vod.rating?.toString() ?: "8.0",
                    year = "2024"
                )
            )
        }

        allSeries.addAll(seriesList)

        catFav.channelCount = allChannels.count { it.isFavorite }
        channelRepository = allChannels

        categoryAdapter.submitList(allCategories)
        movieAdapter.submitList(allMovies)
        seriesAdapter.submitList(allSeries)
        selectCategory(catAll)
    }

    private fun loadDemoData() {
        allCategories.clear()
        allChannels.clear()
        allMovies.clear()
        allSeries.clear()

        // 1. Live Streams
        val demoStreams = listOf(
            LiveStream(1, "TRT 1 HD", streamId = 101, categoryId = "1", streamIcon = "https://upload.wikimedia.org/wikipedia/commons/thumb/c/c2/TRT_1_logo_2021.svg/320px-TRT_1_logo_2021.svg.png"),
            LiveStream(2, "TRT Haber HD", streamId = 102, categoryId = "1", streamIcon = "https://upload.wikimedia.org/wikipedia/commons/thumb/9/91/TRT_Haber_logo_2021.svg/320px-TRT_Haber_logo_2021.svg.png"),
            LiveStream(3, "TRT Spor HD", streamId = 103, categoryId = "2", streamIcon = "https://upload.wikimedia.org/wikipedia/commons/thumb/8/87/TRT_Spor_logo_2021.svg/320px-TRT_Spor_logo_2021.svg.png"),
            LiveStream(4, "TRT World HD", streamId = 104, categoryId = "1", streamIcon = "https://upload.wikimedia.org/wikipedia/commons/thumb/4/4b/TRT_World_logo.svg/320px-TRT_World_logo.svg.png"),
            LiveStream(5, "Red Bull TV HD", streamId = 105, categoryId = "2", streamIcon = "https://upload.wikimedia.org/wikipedia/commons/thumb/e/e0/Red_Bull_TV_logo.svg/320px-Red_Bull_TV_logo.svg.png"),
            LiveStream(6, "NASA TV HD", streamId = 106, categoryId = "3", streamIcon = "https://upload.wikimedia.org/wikipedia/commons/thumb/e/e5/NASA_logo.svg/320px-NASA_logo.svg.png"),
            LiveStream(7, "Bloomberg TV", streamId = 107, categoryId = "1", streamIcon = "https://upload.wikimedia.org/wikipedia/commons/thumb/6/6f/Bloomberg_Television_logo.svg/320px-Bloomberg_Television_logo.svg.png"),
            LiveStream(8, "France 24 HD", streamId = 108, categoryId = "1", streamIcon = "https://upload.wikimedia.org/wikipedia/commons/thumb/c/c3/France_24_logo.svg/320px-France_24_logo.svg.png")
        )

        val demoUrls = mapOf(
            101 to "https://tv-trt1.medya.trt.com.tr/live/hls/trt1/playlist.m3u8",
            102 to "https://tv-trthaber.medya.trt.com.tr/live/hls/trthaber/playlist.m3u8",
            103 to "https://tv-trtspor.medya.trt.com.tr/live/hls/trtspor/playlist.m3u8",
            104 to "https://tv-trtworld.medya.trt.com.tr/live/hls/trtworld/playlist.m3u8",
            105 to "https://rbmn-live.akamaized.net/hls/live/590964/BoRB-AT/master.m3u8",
            106 to "https://ntv1.akamaized.net/hls/live/2014075/NASA-NTV1-HLS/master.m3u8",
            107 to "https://liveproduseast.global.ssl.fastly.net/us/Channel-us-fast-1/manifest.m3u8",
            108 to "https://france24.akamaized.net/hls/live/2034293/F24_EN_LO_HLS/master.m3u8"
        )

        val cats = listOf(
            LiveCategory("all", getString(R.string.cat_all), channelCount = demoStreams.size),
            LiveCategory("favorites", getString(R.string.cat_favorites)),
            LiveCategory("1", "Ulusal & Haber"),
            LiveCategory("2", "Spor"),
            LiveCategory("3", "Belgesel & Bilim")
        )

        demoStreams.forEachIndexed { index, stream ->
            val catName = when(stream.categoryId) {
                "1" -> "Ulusal & Haber"
                "2" -> "Spor"
                "3" -> "Belgesel & Bilim"
                else -> "Genel"
            }
            val url = demoUrls[stream.streamId] ?: "https://tv-trt1.medya.trt.com.tr/live/hls/trt1/playlist.m3u8"
            val item = ChannelItem(
                index = index + 1,
                stream = stream,
                categoryName = catName,
                streamUrl = url,
                isFavorite = prefs.isFavorite(stream.streamId)
            )
            allChannels.add(item)
        }

        // 2. Netflix-Style Demo Movies (Ultra HD open legal films)
        allMovies.add(
            MovieItem(
                index = 1,
                stream = VodStream(name = "Big Buck Bunny 4K", streamId = 201, streamIcon = "https://upload.wikimedia.org/wikipedia/commons/thumb/c/c5/Big_buck_bunny_poster_big.jpg/320px-Big_buck_bunny_poster_big.jpg"),
                categoryName = "Animasyon",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                ratingFormatted = "8.4",
                year = "2024"
            )
        )
        allMovies.add(
            MovieItem(
                index = 2,
                stream = VodStream(name = "Tears of Steel 4K", streamId = 202, streamIcon = "https://upload.wikimedia.org/wikipedia/commons/thumb/e/e0/Tears_of_Steel_poster.jpg/320px-Tears_of_Steel_poster.jpg"),
                categoryName = "Bilim Kurgu",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
                ratingFormatted = "8.8",
                year = "2024"
            )
        )
        allMovies.add(
            MovieItem(
                index = 3,
                stream = VodStream(name = "Sintel 4K", streamId = 203, streamIcon = "https://upload.wikimedia.org/wikipedia/commons/thumb/8/8f/Sintel_poster.jpg/320px-Sintel_poster.jpg"),
                categoryName = "Fantastik",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
                ratingFormatted = "8.6",
                year = "2023"
            )
        )
        allMovies.add(
            MovieItem(
                index = 4,
                stream = VodStream(name = "Cosmos Laundromat", streamId = 204, streamIcon = "https://upload.wikimedia.org/wikipedia/commons/thumb/3/30/Cosmos_Laundromat_-_First_Cycle_-_Official_Blender_Foundation_release.webm/page1-320px-Cosmos_Laundromat_-_First_Cycle_-_Official_Blender_Foundation_release.webm.jpg"),
                categoryName = "Macera",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/WeAreGoingOnBullrun.mp4",
                ratingFormatted = "8.5",
                year = "2023"
            )
        )

        // 3. Netflix-Style Demo Series
        allSeries.add(
            SeriesItem(
                seriesId = 301,
                name = "Breaking Bad",
                cover = "https://upload.wikimedia.org/wikipedia/en/thumb/6/61/Breaking_Bad_title_card.png/320px-Breaking_Bad_title_card.png",
                genre = "Suç • Dram • Gerilim",
                rating = "9.5",
                plot = "Kanser teşhisi konan Walter White, ailesi için metamfetamin üreticiliğine soyunur."
            )
        )
        allSeries.add(
            SeriesItem(
                seriesId = 302,
                name = "Stranger Things",
                cover = "https://upload.wikimedia.org/wikipedia/commons/thumb/3/38/Stranger_Things_logo.png/320px-Stranger_Things_logo.png",
                genre = "Bilim Kurgu • Gizem",
                rating = "8.7",
                plot = "Küçük bir kasabada kaybolan bir çocuk ve ortaya çıkan doğaüstü güçler."
            )
        )

        channelRepository = allChannels
        allCategories.addAll(cats)
        categoryAdapter.submitList(allCategories)
        movieAdapter.submitList(allMovies)
        seriesAdapter.submitList(allSeries)
        selectCategory(cats[0])

        if (allMovies.isNotEmpty()) setHeroMovie(allMovies[0])
        if (allSeries.isNotEmpty()) setHeroSeries(allSeries[0])
    }

    private fun selectCategory(category: LiveCategory) {
        activeCategory = category
        binding.tvCurrentCategory.text = category.categoryName

        val filtered = when (category.categoryId) {
            "all" -> allChannels
            "favorites" -> allChannels.filter { it.isFavorite }
            else -> allChannels.filter { it.stream.categoryId == category.categoryId }
        }

        currentFilteredChannels.clear()
        currentFilteredChannels.addAll(filtered)
        channelAdapter.submitList(currentFilteredChannels)

        binding.tvChannelCount.text = getString(R.string.channel_count_format, filtered.size)
        binding.tvEmptyChannels.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE

        if (filtered.isNotEmpty() && currentlyPlayingChannel == null) {
            onChannelPreviewFocused(filtered[0])
        }
    }

    private fun filterChannels(query: String) {
        val trimmed = query.trim().lowercase()
        val baseList = when (activeCategory?.categoryId) {
            "favorites" -> allChannels.filter { it.isFavorite }
            "all", null -> allChannels
            else -> allChannels.filter { it.stream.categoryId == activeCategory?.categoryId }
        }

        val result = if (trimmed.isEmpty()) {
            baseList
        } else {
            baseList.filter {
                it.stream.name.lowercase().contains(trimmed) || it.index.toString() == trimmed
            }
        }

        currentFilteredChannels.clear()
        currentFilteredChannels.addAll(result)
        channelAdapter.submitList(currentFilteredChannels)
        binding.tvChannelCount.text = getString(R.string.channel_count_format, result.size)
        binding.tvEmptyChannels.visibility = if (result.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun onChannelPreviewFocused(channel: ChannelItem) {
        currentlyPlayingChannel = channel
        binding.tvPreviewTitle.text = channel.stream.name
        binding.tvPreviewCategory.text = channel.categoryName
        updatePreviewFavoriteState(channel)

        if (!channel.stream.streamIcon.isNullOrBlank()) {
            Glide.with(this)
                .load(channel.stream.streamIcon)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.ic_channel_placeholder)
                .error(R.drawable.ic_channel_placeholder)
                .into(binding.ivPreviewLogo)
        } else {
            binding.ivPreviewLogo.setImageResource(R.drawable.ic_channel_placeholder)
        }

        previewPlayJob?.cancel()
        previewPlayJob = lifecycleScope.launch {
            delay(150)
            playerManager.playStream(channel.streamUrl)
        }
    }

    private fun updatePreviewFavoriteState(channel: ChannelItem) {
        if (channel.isFavorite) {
            binding.ivPreviewFavorite.setImageResource(R.drawable.ic_heart_filled)
        } else {
            binding.ivPreviewFavorite.setImageResource(R.drawable.ic_heart)
        }
    }

    private fun openFullscreen(channel: ChannelItem) {
        playerManager.pause()
        val intent = Intent(this, FullscreenPlayerActivity::class.java).apply {
            putExtra("EXTRA_INITIAL_CHANNEL_ID", channel.stream.streamId)
        }
        startActivity(intent)
    }

    private fun showSettingsDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_settings)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val tvUser = dialog.findViewById<TextView>(R.id.tvDialogUsername)
        val tvServer = dialog.findViewById<TextView>(R.id.tvDialogServer)
        val tvStatus = dialog.findViewById<TextView>(R.id.tvDialogStatus)
        val tvExpires = dialog.findViewById<TextView>(R.id.tvDialogExpires)
        val btnLogout = dialog.findViewById<Button>(R.id.btnDialogLogout)
        val btnClose = dialog.findViewById<Button>(R.id.btnDialogClose)

        val displayUser = if (isDemoMode) "Demo Kullanıcı" else prefs.username
        val displayServer = if (isDemoMode) "Demo Test Sunucusu" else prefs.serverUrl
        tvUser.text = getString(R.string.username_format, displayUser)
        tvServer.text = getString(R.string.server_url_format, displayServer)
        tvStatus.text = getString(R.string.status_active)
        tvExpires.text = getString(R.string.expires_at)

        btnLogout.setOnClickListener {
            prefs.clearCredentials()
            playerManager.release()
            dialog.dismiss()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        btnClose.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    override fun onResume() {
        super.onResume()
        updateHeaderClock()
        if (binding.sectionLiveTv.visibility == View.VISIBLE) {
            currentlyPlayingChannel?.let {
                playerManager.playStream(it.streamUrl)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        playerManager.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        playerManager.release()
    }
}
