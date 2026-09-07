package com.ugur.iptv

import com.ugur.iptv.api.ApiClient
import com.ugur.iptv.data.ChannelItem
import com.ugur.iptv.data.LiveCategory
import com.ugur.iptv.data.LiveStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DataLogicTest {

    @Test
    fun testBuildLiveStreamUrl() {
        val url = ApiClient.buildLiveStreamUrl(
            baseUrl = "http://example.com:8080",
            username = "user123",
            password = "pass456",
            streamId = 789,
            extension = "m3u8"
        )
        assertEquals("http://example.com:8080/live/user123/pass456/789.m3u8", url)

        // Trailing slash handling
        val urlWithSlash = ApiClient.buildLiveStreamUrl(
            baseUrl = "http://example.com:8080/",
            username = "user123",
            password = "pass456",
            streamId = 789,
            extension = "ts"
        )
        assertEquals("http://example.com:8080/live/user123/pass456/789.ts", urlWithSlash)
    }

    @Test
    fun testChannelItemProperties() {
        val stream = LiveStream(
            num = 1,
            name = "TRT 1 HD",
            streamId = 101,
            categoryId = "cat_national"
        )
        val channel = ChannelItem(
            index = 1,
            stream = stream,
            categoryName = "Ulusal",
            streamUrl = "http://stream.url",
            isFavorite = false
        )

        assertEquals("TRT 1 HD", channel.stream.name)
        assertEquals(101, channel.stream.streamId)
        assertFalse(channel.isFavorite)

        channel.isFavorite = true
        assertTrue(channel.isFavorite)
    }

    @Test
    fun testCategoryCount() {
        val cat = LiveCategory(
            categoryId = "1",
            categoryName = "Spor",
            channelCount = 25
        )
        assertEquals("Spor", cat.categoryName)
        assertEquals(25, cat.channelCount)
    }
}
