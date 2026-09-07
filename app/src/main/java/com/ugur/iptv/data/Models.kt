package com.ugur.iptv.data

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class XtreamLoginResponse(
    @SerializedName("user_info") val userInfo: UserInfo?,
    @SerializedName("server_info") val serverInfo: ServerInfo?
) : Serializable

data class UserInfo(
    @SerializedName("username") val username: String? = null,
    @SerializedName("password") val password: String? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("auth") val auth: Int? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("exp_date") val expDate: String? = null,
    @SerializedName("is_trial") val isTrial: String? = null,
    @SerializedName("active_cons") val activeCons: String? = null,
    @SerializedName("max_connections") val maxConnections: String? = null
) : Serializable

data class ServerInfo(
    @SerializedName("url") val url: String? = null,
    @SerializedName("port") val port: String? = null,
    @SerializedName("https_port") val httpsPort: String? = null,
    @SerializedName("server_protocol") val serverProtocol: String? = null,
    @SerializedName("timezone") val timezone: String? = null,
    @SerializedName("time_now") val timeNow: String? = null
) : Serializable

data class LiveCategory(
    @SerializedName("category_id") val categoryId: String,
    @SerializedName("category_name") val categoryName: String,
    @SerializedName("parent_id") val parentId: Int = 0,
    var channelCount: Int = 0
) : Serializable

data class LiveStream(
    @SerializedName("num") val num: Any? = null,
    @SerializedName("name") val name: String,
    @SerializedName("stream_type") val streamType: String? = null,
    @SerializedName("stream_id") val streamId: Int,
    @SerializedName("stream_icon") val streamIcon: String? = null,
    @SerializedName("epg_channel_id") val epgChannelId: String? = null,
    @SerializedName("category_id") val categoryId: String? = null,
    @SerializedName("custom_sid") val customSid: String? = null,
    @SerializedName("direct_source") val directSource: String? = null
) : Serializable

data class ChannelItem(
    val index: Int,
    val stream: LiveStream,
    val categoryName: String,
    val streamUrl: String,
    var isFavorite: Boolean = false
) : Serializable

data class VodCategory(
    @SerializedName("category_id") val categoryId: String,
    @SerializedName("category_name") val categoryName: String,
    @SerializedName("parent_id") val parentId: Int = 0,
    var movieCount: Int = 0
) : Serializable

data class VodStream(
    @SerializedName("num") val num: Any? = null,
    @SerializedName("name") val name: String,
    @SerializedName("stream_type") val streamType: String? = null,
    @SerializedName("stream_id") val streamId: Int,
    @SerializedName("stream_icon") val streamIcon: String? = null,
    @SerializedName("rating") val rating: Any? = null,
    @SerializedName("rating_5based") val rating5Based: Any? = null,
    @SerializedName("added") val added: String? = null,
    @SerializedName("category_id") val categoryId: String? = null,
    @SerializedName("container_extension") val containerExtension: String? = "mp4"
) : Serializable

data class MovieItem(
    val index: Int,
    val stream: VodStream,
    val categoryName: String,
    val streamUrl: String,
    val ratingFormatted: String,
    val year: String = "",
    var isFavorite: Boolean = false
) : Serializable

data class SeriesCategory(
    @SerializedName("category_id") val categoryId: String,
    @SerializedName("category_name") val categoryName: String,
    @SerializedName("parent_id") val parentId: Int = 0,
    var seriesCount: Int = 0
) : Serializable

data class SeriesItem(
    @SerializedName("num") val num: Any? = null,
    @SerializedName("name") val name: String,
    @SerializedName("series_id") val seriesId: Int,
    @SerializedName("cover") val cover: String? = null,
    @SerializedName("plot") val plot: String? = null,
    @SerializedName("cast") val cast: String? = null,
    @SerializedName("director") val director: String? = null,
    @SerializedName("genre") val genre: String? = null,
    @SerializedName("releaseDate") val releaseDate: String? = null,
    @SerializedName("last_modified") val lastModified: String? = null,
    @SerializedName("rating") val rating: Any? = null,
    @SerializedName("rating_5based") val rating5Based: Any? = null,
    @SerializedName("category_id") val categoryId: String? = null
) : Serializable

data class EpisodeItem(
    @SerializedName("id") val id: String? = null,
    @SerializedName("episode_num") val episodeNum: Any? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("container_extension") val containerExtension: String? = "mp4",
    var streamUrl: String = ""
) : Serializable
