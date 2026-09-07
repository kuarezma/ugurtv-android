package com.ugur.iptv.ui

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.ugur.iptv.R
import com.ugur.iptv.data.ChannelItem
import com.ugur.iptv.data.PreferencesManager
import com.ugur.iptv.databinding.ActivityFullscreenPlayerBinding
import com.ugur.iptv.player.LowLatencyPlayerManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FullscreenPlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFullscreenPlayerBinding
    private lateinit var playerManager: LowLatencyPlayerManager
    private lateinit var prefs: PreferencesManager
    private lateinit var quickAdapter: QuickChannelAdapter

    private var channelList: List<ChannelItem> = emptyList()
    private var currentIndex: Int = 0

    private val handler = Handler(Looper.getMainLooper())
    private val hideOsdRunnable = Runnable { hideOsd() }
    private val directDialRunnable = Runnable { executeDirectDial() }

    private var digitBuffer = StringBuilder()
    private var currentAspectMode = 0 // 0: Fit, 1: Fill, 2: 16:9, 3: 4:3

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFullscreenPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = PreferencesManager(this)
        channelList = MainActivity.channelRepository

        val initialChannelId = intent.getIntExtra("EXTRA_INITIAL_CHANNEL_ID", -1)
        val foundIndex = channelList.indexOfFirst { it.stream.streamId == initialChannelId }
        currentIndex = if (foundIndex != -1) foundIndex else 0

        initPlayer()
        setupQuickDrawer()
        setupListeners()

        currentAspectMode = prefs.aspectRatioMode
        playerManager.setAspectRatio(binding.fullscreenPlayerView, currentAspectMode)

        if (channelList.isNotEmpty()) {
            playCurrentChannel()
        }
    }

    private fun initPlayer() {
        playerManager = LowLatencyPlayerManager(this)
        playerManager.attachToView(binding.fullscreenPlayerView)

        playerManager.onBufferingStateChanged = { isBuffering ->
            binding.pbBuffering.visibility = if (isBuffering) View.VISIBLE else View.GONE
        }

        playerManager.onErrorOccurred = { errorMsg ->
            binding.pbBuffering.visibility = View.GONE
            Toast.makeText(this, "Kanal Hatası: $errorMsg", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupQuickDrawer() {
        quickAdapter = QuickChannelAdapter { channel ->
            val newIdx = channelList.indexOf(channel)
            if (newIdx != -1) {
                currentIndex = newIdx
                playCurrentChannel()
                hideQuickDrawer()
            }
        }

        binding.rvQuickChannels.apply {
            layoutManager = LinearLayoutManager(this@FullscreenPlayerActivity)
            adapter = quickAdapter
            setHasFixedSize(true)
        }
        quickAdapter.submitList(channelList)
    }

    private fun setupListeners() {
        binding.ivOsdFavorite.setOnClickListener {
            val channel = channelList.getOrNull(currentIndex) ?: return@setOnClickListener
            val newState = prefs.toggleFavorite(channel.stream.streamId)
            channel.isFavorite = newState
            updateOsdFavoriteIcon(channel)
            Toast.makeText(
                this,
                if (newState) getString(R.string.fav_added) else getString(R.string.fav_removed),
                Toast.LENGTH_SHORT
            ).show()
        }

        binding.btnAspectRatio.setOnClickListener {
            cycleAspectRatio()
        }
    }

    private fun playCurrentChannel() {
        val channel = channelList.getOrNull(currentIndex) ?: return
        prefs.lastChannelId = channel.stream.streamId
        playerManager.playStream(channel.streamUrl)
        showOsd(channel)
    }

    private fun zapNext() {
        if (channelList.isEmpty()) return
        currentIndex = (currentIndex + 1) % channelList.size
        playCurrentChannel()
    }

    private fun zapPrevious() {
        if (channelList.isEmpty()) return
        currentIndex = if (currentIndex - 1 < 0) channelList.size - 1 else currentIndex - 1
        playCurrentChannel()
    }

    private fun cycleAspectRatio() {
        currentAspectMode = (currentAspectMode + 1) % 4
        prefs.aspectRatioMode = currentAspectMode
        playerManager.setAspectRatio(binding.fullscreenPlayerView, currentAspectMode)

        val aspectName = when (currentAspectMode) {
            0 -> getString(R.string.aspect_fit)
            1 -> getString(R.string.aspect_fill)
            2 -> getString(R.string.aspect_16_9)
            3 -> getString(R.string.aspect_4_3)
            else -> getString(R.string.aspect_fit)
        }
        Toast.makeText(this, "${getString(R.string.aspect_ratio)}: $aspectName", Toast.LENGTH_SHORT).show()
    }

    private fun showOsd(channel: ChannelItem) {
        binding.tvOsdChannelNumber.text = String.format("%03d", channel.index)
        binding.tvOsdTitle.text = channel.stream.name
        binding.tvOsdCategory.text = channel.categoryName

        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        binding.tvOsdClock.text = timeFormat.format(Date())

        updateOsdFavoriteIcon(channel)

        val iconUrl = channel.stream.streamIcon
        if (!iconUrl.isNullOrBlank()) {
            Glide.with(this)
                .load(iconUrl)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.ic_channel_placeholder)
                .error(R.drawable.ic_channel_placeholder)
                .into(binding.ivOsdLogo)
        } else {
            binding.ivOsdLogo.setImageResource(R.drawable.ic_channel_placeholder)
        }

        binding.layoutBottomOsd.visibility = View.VISIBLE
        handler.removeCallbacks(hideOsdRunnable)
        handler.postDelayed(hideOsdRunnable, 4000)
    }

    private fun hideOsd() {
        binding.layoutBottomOsd.visibility = View.GONE
    }

    private fun updateOsdFavoriteIcon(channel: ChannelItem) {
        if (channel.isFavorite) {
            binding.ivOsdFavorite.setImageResource(R.drawable.ic_heart_filled)
        } else {
            binding.ivOsdFavorite.setImageResource(R.drawable.ic_heart)
        }
    }

    private fun showQuickDrawer() {
        binding.layoutQuickDrawer.visibility = View.VISIBLE
        binding.rvQuickChannels.requestFocus()
        binding.rvQuickChannels.scrollToPosition(currentIndex)
    }

    private fun hideQuickDrawer() {
        binding.layoutQuickDrawer.visibility = View.GONE
    }

    private fun handleDigitInput(digit: Int) {
        digitBuffer.append(digit)
        binding.tvDirectNumber.text = digitBuffer.toString()
        binding.tvDirectNumber.visibility = View.VISIBLE

        handler.removeCallbacks(directDialRunnable)
        handler.postDelayed(directDialRunnable, 1200)
    }

    private fun executeDirectDial() {
        val number = digitBuffer.toString().toIntOrNull()
        digitBuffer.clear()
        binding.tvDirectNumber.visibility = View.GONE

        if (number != null) {
            val target = channelList.indexOfFirst { it.index == number }
            if (target != -1) {
                currentIndex = target
                playCurrentChannel()
            } else {
                Toast.makeText(this, "$number numaralı kanal bulunamadı", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // Quick Drawer open state
        if (binding.layoutQuickDrawer.visibility == View.VISIBLE) {
            if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                hideQuickDrawer()
                return true
            }
            return super.onKeyDown(keyCode, event)
        }

        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_CHANNEL_UP -> {
                zapNext()
                return true
            }
            KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_CHANNEL_DOWN -> {
                zapPrevious()
                return true
            }
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                showQuickDrawer()
                return true
            }
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                if (binding.layoutBottomOsd.visibility == View.VISIBLE) {
                    hideOsd()
                } else {
                    channelList.getOrNull(currentIndex)?.let { showOsd(it) }
                }
                return true
            }
            KeyEvent.KEYCODE_0 -> { handleDigitInput(0); return true }
            KeyEvent.KEYCODE_1 -> { handleDigitInput(1); return true }
            KeyEvent.KEYCODE_2 -> { handleDigitInput(2); return true }
            KeyEvent.KEYCODE_3 -> { handleDigitInput(3); return true }
            KeyEvent.KEYCODE_4 -> { handleDigitInput(4); return true }
            KeyEvent.KEYCODE_5 -> { handleDigitInput(5); return true }
            KeyEvent.KEYCODE_6 -> { handleDigitInput(6); return true }
            KeyEvent.KEYCODE_7 -> { handleDigitInput(7); return true }
            KeyEvent.KEYCODE_8 -> { handleDigitInput(8); return true }
            KeyEvent.KEYCODE_9 -> { handleDigitInput(9); return true }
            KeyEvent.KEYCODE_BACK -> {
                finish()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onResume() {
        super.onResume()
        playerManager.resume()
    }

    override fun onPause() {
        super.onPause()
        playerManager.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(hideOsdRunnable)
        handler.removeCallbacks(directDialRunnable)
        playerManager.release()
    }
}
