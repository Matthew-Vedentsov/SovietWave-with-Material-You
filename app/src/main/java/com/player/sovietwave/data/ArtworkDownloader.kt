package com.player.sovietwave.data

import android.graphics.BitmapFactory
import android.util.Log
import com.player.sovietwave.media.ArtworkHttpClient
import com.player.sovietwave.media.StationArtwork
import okhttp3.OkHttpClient

data class ArtworkDownload(
    val url: String,
    val bytes: ByteArray,
)

object ArtworkDownloader {

    private const val TAG = "ArtworkDownloader"

    fun download(highResUrl: String, fallbackUrl: String): ArtworkDownload? {
        Log.d(TAG, "Starting download: $highResUrl")
        downloadFromUrl(highResUrl, ArtworkHttpClient.highResClient)?.let {
            Log.d(TAG, "High-res download success")
            return ArtworkDownload(highResUrl, it)
        }
        
        Log.d(TAG, "High-res failed, trying fallback: $fallbackUrl")
        downloadFromUrl(fallbackUrl, ArtworkHttpClient.fallbackClient)?.let {
            Log.d(TAG, "Fallback download success")
            return ArtworkDownload(fallbackUrl, it)
        }
        
        Log.d(TAG, "All downloads failed")
        return null
    }

    private fun downloadFromUrl(url: String, client: OkHttpClient): ByteArray? {
        return try {
            val request = ArtworkHttpClient.imageRequest(url)
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "Download failed with code: ${response.code} for $url")
                    return null
                }
                
                val bytes = response.body?.bytes()
                if (bytes == null || !isValidImage(bytes)) {
                    Log.w(TAG, "Invalid image data received from $url")
                    return null
                }
                bytes
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading artwork: $url", e)
            null
        }
    }

    private fun isValidImage(bytes: ByteArray): Boolean {
        if (bytes.size < 10) return false
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        return options.outWidth > 0 && options.outHeight > 0
    }
}

sealed class ArtworkLoadResult {
    data class Success(
        val url: String,
        val displayBytes: ByteArray,
        val metadataBytes: ByteArray,
    ) : ArtworkLoadResult()

    data class UrlOnly(
        val url: String,
        val fallbackUrl: String,
    ) : ArtworkLoadResult()

    data object NotFound : ArtworkLoadResult()
    data object Failed : ArtworkLoadResult()
}

fun ITunesArtworkApi.loadArtwork(artist: String?, trackTitle: String?): ArtworkLoadResult {
    return when (val search = searchArtwork(artist, trackTitle)) {
        ArtworkSearchResult.NotFound -> ArtworkLoadResult.NotFound
        ArtworkSearchResult.Failed -> ArtworkLoadResult.Failed
        is ArtworkSearchResult.Found -> {
            val downloaded = ArtworkDownloader.download(search.url, search.fallbackUrl)
            if (downloaded != null) {
                ArtworkLoadResult.Success(
                    url = downloaded.url,
                    displayBytes = downloaded.bytes,
                    metadataBytes = StationArtwork.prepareForMetadata(downloaded.bytes),
                )
            } else {
                ArtworkLoadResult.UrlOnly(search.url, search.fallbackUrl)
            }
        }
    }
}
