package com.ugur.iptv.ui

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.ugur.iptv.R
import com.ugur.iptv.api.ApiClient
import com.ugur.iptv.data.ChannelItem
import com.ugur.iptv.data.LiveCategory
import com.ugur.iptv.data.LiveStream
import com.ugur.iptv.data.PreferencesManager
import com.ugur.iptv.databinding.ActivityMainBinding
import com.ugur.iptv.player.LowLatencyPlayerManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: PreferencesManager
    private lateinit var playerManager: LowLatencyPlayerManager

    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var channelAdapter: ChannelAdapter

    private val allCategories = mutableListOf<LiveCategory>()
    private val allChannels = mutableListOf<ChannelItem>()
    private val currentFilteredChannels = mutableListOf<ChannelItem>()

    private var activeCategory: LiveCategory? = null
    private var currentlyPlayingChannel: ChannelItem? = null
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
        setupRecyclerViews()
        setupListeners()
        loadData()
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

    private fun setupRecyclerViews() {
        // Categories List
        categoryAdapter = CategoryAdapter { category ->
            selectCategory(category)
        }
        binding.rvCategories.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = categoryAdapter
            setHasFixedSize(true)
        }

        // Channels List
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
            itemAnimator = null // Zero latency scrolling
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

        binding.btnOpenSettings.setOnClickListener {
            showSettingsDialog()
        }

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterChannels(s?.toString().orEmpty())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun loadData() {
        if (isDemoMode) {
            loadDemoData()
            return
        }

        lifecycleScope.launch {
            try {
                val service = ApiClient.getService(prefs.serverUrl)
                val (categories, streams) = withContext(Dispatchers.IO) {
                    val cats = service.getLiveCategories(prefs.username, prefs.password)
                    val strms = service.getLiveStreams(prefs.username, prefs.password)
                    Pair(cats, strms)
                }

                populateData(categories, streams)
            } catch (e: Exception) {
                Toast.makeText(
                    this@MainActivity,
                    "Veriler yüklenirken hata oluştu: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
                // Fallback to demo data on network error so TV is never stuck
                loadDemoData()
            }
        }
    }

    private fun populateData(categories: List<LiveCategory>, streams: List<LiveStream>) {
        allCategories.clear()
        allChannels.clear()

        // 1. Virtual Categories: Tüm Kanallar & Favorilerim
        val catAll = LiveCategory("all", getString(R.string.cat_all), channelCount = streams.size)
        val catFav = LiveCategory("favorites", getString(R.string.cat_favorites))
        allCategories.add(catAll)
        allCategories.add(catFav)
        allCategories.addAll(categories)

        val catMap = categories.associateBy { it.categoryId }

        // Build Channel Items
        val favIds = prefs.getFavorites()
        streams.forEachIndexed { idx, st ->
            val catName = catMap[st.categoryId]?.categoryName ?: "Genel"
            val streamUrl = ApiClient.buildLiveStreamUrl(
                prefs.serverUrl,
                prefs.username,
                prefs.password,
                st.streamId
            )
            val item = ChannelItem(
                index = idx + 1,
                stream = st,
                categoryName = catName,
                streamUrl = streamUrl,
                isFavorite = favIds.contains(st.streamId.toString())
            )
            allChannels.add(item)
        }

        // Count favorites
        catFav.channelCount = allChannels.count { it.isFavorite }
        channelRepository = allChannels

        categoryAdapter.submitList(allCategories)
        selectCategory(catAll)
    }

    private fun loadDemoData() {
        allCategories.clear()
        allChannels.clear()

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

        channelRepository = allChannels
        allCategories.addAll(cats)
        categoryAdapter.submitList(allCategories)
        selectCategory(cats[0])
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

        // Fast debounce preview (150ms) to prevent player stutter during fast arrow key scrolling
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

        tvUser.text = "Kullanıcı: ${if (isDemoMode) "Demo Kullanıcı" else prefs.username}"
        tvServer.text = "Sunucu: ${if (isDemoMode) "Demo Test Sunucusu" else prefs.serverUrl}"
        tvStatus.text = "Durum: Aktif (Vestel TV 65\")"
        tvExpires.text = "Bitiş: Süresiz"

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
        currentlyPlayingChannel?.let {
            playerManager.playStream(it.streamUrl)
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
