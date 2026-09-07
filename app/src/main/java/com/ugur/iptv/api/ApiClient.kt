package com.ugur.iptv.api

import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    private var currentBaseUrl: String? = null
    private var currentService: XtreamService? = null

    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectionPool(ConnectionPool(10, 5, TimeUnit.MINUTES))
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    fun getService(baseUrl: String): XtreamService {
        val sanitizedUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        if (currentService == null || currentBaseUrl != sanitizedUrl) {
            currentBaseUrl = sanitizedUrl
            val retrofit = Retrofit.Builder()
                .baseUrl(sanitizedUrl)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            currentService = retrofit.create(XtreamService::class.java)
        }
        return currentService!!
    }

    fun buildLiveStreamUrl(
        baseUrl: String,
        username: String,
        password: String,
        streamId: Int,
        extension: String = "m3u8"
    ): String {
        val cleanBase = if (baseUrl.endsWith("/")) baseUrl.dropLast(1) else baseUrl
        return "$cleanBase/live/$username/$password/$streamId.$extension"
    }

    fun buildVodStreamUrl(
        baseUrl: String,
        username: String,
        password: String,
        streamId: Int,
        extension: String = "mp4"
    ): String {
        val cleanBase = if (baseUrl.endsWith("/")) baseUrl.dropLast(1) else baseUrl
        val ext = if (extension.isBlank()) "mp4" else extension
        return "$cleanBase/movie/$username/$password/$streamId.$ext"
    }

    fun buildSeriesStreamUrl(
        baseUrl: String,
        username: String,
        password: String,
        episodeId: String,
        extension: String = "mp4"
    ): String {
        val cleanBase = if (baseUrl.endsWith("/")) baseUrl.dropLast(1) else baseUrl
        val ext = if (extension.isBlank()) "mp4" else extension
        return "$cleanBase/series/$username/$password/$episodeId.$ext"
    }
}
