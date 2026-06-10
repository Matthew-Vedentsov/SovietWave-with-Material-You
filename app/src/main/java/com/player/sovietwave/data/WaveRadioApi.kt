package com.player.sovietwave.data

import com.player.sovietwave.media.StreamMetadataParser
import com.player.sovietwave.media.StreamTrackInfo
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object WaveRadioApi {

    private const val API_BASE_URL = "https://core.waveradio.org/public"
    private const val HISTORY_AMOUNT = 100

    fun fetchCurrentTrack(stationId: String = SOVIET_STATION_ID): StreamTrackInfo? {
        val body = get("$API_BASE_URL/current?station=$stationId&brief=1") ?: return null
        val json = JSONObject(body)
        if (json.optInt("status", -1) != 0) return null
        val payload = json.optString("payload").trim()
        if (payload.isEmpty()) return null
        return StreamMetadataParser.parseStreamTitle(payload)
    }

    fun fetchHistory(
        stationId: String = SOVIET_STATION_ID,
        amount: Int = HISTORY_AMOUNT,
    ): List<BroadcastHistoryItem>? {
        val body = get(
            "$API_BASE_URL/history?station=$stationId&amount=$amount&extend=1",
        ) ?: return null
        return parseHistoryResponse(body)
    }

    private fun parseHistoryResponse(body: String): List<BroadcastHistoryItem>? {
        val json = JSONObject(body)
        if (json.optInt("status", -1) != 0) return null
        val payload = json.optJSONArray("payload") ?: return emptyList()
        return payload.toHistoryItems()
    }

    private fun JSONArray.toHistoryItems(): List<BroadcastHistoryItem> = buildList {
        for (index in 0 until length()) {
            val item = getJSONObject(index)
            val artist = item.optString("artist").trim()
            val trackTitle = item.optString("track_title").trim()
            if (artist.isEmpty() && trackTitle.isEmpty()) continue
            add(
                BroadcastHistoryItem(
                    artist = artist,
                    trackTitle = trackTitle,
                    startTimeEpochSeconds = item.optLong("start_time"),
                    artistLinks = item.optJSONArray("artist_links").toLinkTexts(),
                ),
            )
        }
    }

    private fun JSONArray?.toLinkTexts(): List<String> {
        if (this == null) return emptyList()
        return buildList {
            for (index in 0 until length()) {
                val link = getJSONObject(index).optString("link_text").trim()
                if (link.isNotEmpty()) add(link)
            }
        }
    }

    private fun get(url: String): String? {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 10_000
        }
        return try {
            if (connection.responseCode !in 200..299) return null
            connection.inputStream.bufferedReader().readText()
        } finally {
            connection.disconnect()
        }
    }

    const val SOVIET_STATION_ID = "soviet"
}
