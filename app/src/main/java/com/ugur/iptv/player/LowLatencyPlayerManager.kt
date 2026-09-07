package com.ugur.iptv.player

import android.content.Context
import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.ugur.iptv.api.ApiClient

class LowLatencyPlayerManager(private val context: Context) {

    private var exoPlayer: ExoPlayer? = null
    private var currentUrl: String? = null
    var onBufferingStateChanged: ((Boolean) -> Unit)? = null
    var onErrorOccurred: ((String) -> Unit)? = null

    init {
        initPlayer()
    }

    private fun initPlayer() {
        if (exoPlayer != null) return

        // 1. Ultra-Low Latency Load Control (Fast Startup: 300ms playback buffer)
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                1200, // minBufferMs
                4000, // maxBufferMs
                300,  // bufferForPlaybackMs (instant start)
                800   // bufferForPlaybackAfterRebufferMs
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        // 2. Hardware Decoder Priority
        val renderersFactory = DefaultRenderersFactory(context)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
            .setEnableDecoderFallback(true)

        // 3. OkHttp DataSource Factory with HTTP/2 & keep-alive
        val httpDataSourceFactory = OkHttpDataSource.Factory(ApiClient.okHttpClient)
            .setUserAgent("UgurTV-ExoPlayer/1.0 (Android TV)")

        val mediaSourceFactory = DefaultMediaSourceFactory(httpDataSourceFactory)

        exoPlayer = ExoPlayer.Builder(context, renderersFactory)
            .setLoadControl(loadControl)
            .setMediaSourceFactory(mediaSourceFactory)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()
            .apply {
                repeatMode = Player.REPEAT_MODE_OFF
                playWhenReady = true

                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        when (playbackState) {
                            Player.STATE_BUFFERING -> onBufferingStateChanged?.invoke(true)
                            Player.STATE_READY -> onBufferingStateChanged?.invoke(false)
                            Player.STATE_ENDED -> onBufferingStateChanged?.invoke(false)
                            Player.STATE_IDLE -> onBufferingStateChanged?.invoke(false)
                        }
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        onBufferingStateChanged?.invoke(false)
                        // Fallback retry with .ts if .m3u8 fails
                        val url = currentUrl
                        if (url != null && url.endsWith(".m3u8")) {
                            val fallbackUrl = url.replace(".m3u8", ".ts")
                            playStream(fallbackUrl)
                        } else {
                            onErrorOccurred?.invoke(error.localizedMessage ?: "Yayın hatası")
                        }
                    }
                })
            }
    }

    fun attachToView(playerView: PlayerView) {
        playerView.player = exoPlayer
    }

    fun playStream(url: String) {
        val player = exoPlayer ?: return
        if (currentUrl == url && player.isPlaying) return

        currentUrl = url
        onBufferingStateChanged?.invoke(true)

        val uri = Uri.parse(url)
        val mediaItem = MediaItem.Builder()
            .setUri(uri)
            .setLiveConfiguration(
                MediaItem.LiveConfiguration.Builder()
                    .setMaxPlaybackSpeed(1.02f)
                    .setMinPlaybackSpeed(0.98f)
                    .setTargetOffsetMs(1500)
                    .build()
            )
            .build()

        player.setMediaItem(mediaItem)
        player.prepare()
        player.playWhenReady = true
    }

    fun pause() {
        exoPlayer?.pause()
    }

    fun resume() {
        exoPlayer?.play()
    }

    fun stop() {
        exoPlayer?.stop()
        currentUrl = null
    }

    fun release() {
        exoPlayer?.release()
        exoPlayer = null
        currentUrl = null
    }

    fun setAspectRatio(playerView: PlayerView, mode: Int) {
        when (mode) {
            0 -> playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
            1 -> playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
            2 -> playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            3 -> playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
            else -> playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
        }
    }
}
