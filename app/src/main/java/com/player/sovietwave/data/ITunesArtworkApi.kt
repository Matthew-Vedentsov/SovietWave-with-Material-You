package com.player.sovietwave.data

import android.net.Uri
import android.util.Log
import com.player.sovietwave.media.ArtworkHttpClient
import okhttp3.Request
import org.json.JSONObject

sealed class ArtworkSearchResult {
    data class Found(
        val url: String,
        val fallbackUrl: String,
    ) : ArtworkSearchResult()

    data object NotFound : ArtworkSearchResult()
    data object Failed : ArtworkSearchResult()
}

object ITunesArtworkApi {

    private const val TAG = "ITunesArtworkApi"
    private const val SEARCH_URL = "https://itunes.apple.com/search"

    fun searchArtwork(artist: String?, trackTitle: String?): ArtworkSearchResult {
        val term = buildSearchTerm(artist, trackTitle) ?: return ArtworkSearchResult.Failed

        val requestUrl = Uri.parse(SEARCH_URL).buildUpon()
            .appendQueryParameter("term", term)
            .appendQueryParameter("media", "music")
            .appendQueryParameter("entity", "musicTrack")
            .appendQueryParameter("limit", "1")
            .build()
            .toString()

        Log.d(TAG, "Searching iTunes: $requestUrl")

        return try {
            val request = Request.Builder()
                .url(requestUrl)
                .header("User-Agent", ArtworkHttpClient.USER_AGENT)
                .build()

            ArtworkHttpClient.client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "iTunes search failed with code: ${response.code}")
                    return ArtworkSearchResult.Failed
                }
                
                val body = response.body?.string()?.trim() ?: return ArtworkSearchResult.Failed
                if (body.isEmpty()) return ArtworkSearchResult.Failed

                val jsonStart = body.indexOf("{")
                val jsonEnd = body.lastIndexOf("}")
                if (jsonStart == -1 || jsonEnd == -1 || jsonEnd <= jsonStart) {
                    return ArtworkSearchResult.Failed
                }
                val jsonStr = body.substring(jsonStart, jsonEnd + 1)
                
                val result = parseSearchResult(jsonStr)
                Log.d(TAG, "Search result: $result")
                result
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error searching iTunes", e)
            ArtworkSearchResult.Failed
        }
    }

    private fun buildSearchTerm(artist: String?, trackTitle: String?): String? {
        val cleanArtist = artist?.trim().orEmpty()
        val cleanTitle = trackTitle?.trim().orEmpty()
        return when {
            cleanArtist.isNotEmpty() && cleanTitle.isNotEmpty() -> "$cleanArtist $cleanTitle"
            cleanTitle.isNotEmpty() -> cleanTitle
            cleanArtist.isNotEmpty() -> cleanArtist
            else -> null
        }
    }

    private fun parseSearchResult(body: String): ArtworkSearchResult {
        return try {
            val json = JSONObject(body)
            val results = json.optJSONArray("results") ?: return ArtworkSearchResult.Failed
            if (results.length() == 0) return ArtworkSearchResult.NotFound

            val result = results.getJSONObject(0)
            val artworkUrl = result.optString("artworkUrl100").ifEmpty {
                result.optString("artworkUrl60").ifEmpty {
                    result.optString("artworkUrl30")
                }
            }.trim()

            if (artworkUrl.isEmpty()) return ArtworkSearchResult.NotFound

            ArtworkSearchResult.Found(
                url = upscaleArtworkUrl(artworkUrl),
                fallbackUrl = artworkUrl,
            )
        } catch (e: Exception) {
            e.printStackTrace()
            ArtworkSearchResult.Failed
        }
    }

    private fun upscaleArtworkUrl(url: String): String {
        val regex = Regex("(\\d+)x(\\d+)(bb)?")
        return if (url.contains(regex)) {
            url.replace(regex, "800x800bb")
        } else {
            url.replace("100x100", "800x800")
        }
    }
}
