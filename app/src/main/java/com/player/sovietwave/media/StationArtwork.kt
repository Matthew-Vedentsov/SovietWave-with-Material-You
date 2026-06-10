package com.player.sovietwave.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.graphics.drawable.toBitmap
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.player.sovietwave.R
import com.player.sovietwave.data.RadioStation
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

object StationArtwork {

    private const val ARTWORK_RES = R.drawable.ic_radio_artwork
    private const val ARTWORK_SIZE_PX = 512
    private const val METADATA_MAX_DIMENSION_PX = 512
    private const val METADATA_JPEG_QUALITY = 85
    @Volatile
    private var cachedArtwork: ByteArray? = null

    fun getArtworkBytes(context: Context): ByteArray? {
        cachedArtwork?.let { return it }
        return synchronized(this) {
            cachedArtwork ?: encodeArtwork(context)?.also { cachedArtwork = it }
        }
    }

    fun metadata(context: Context, title: String): MediaMetadata {
        val builder = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(context.getString(R.string.notification_artist))
            .setAlbumTitle(context.getString(R.string.app_name))
            .setIsPlayable(true)
            .setIsBrowsable(false)
        getArtworkBytes(context)?.let { artwork ->
            builder.setArtworkData(artwork, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
        }
        return builder.build()
    }

    fun browseRootMetadata(context: Context): MediaMetadata {
        val builder = MediaMetadata.Builder()
            .setTitle(context.getString(R.string.app_name))
            .setArtist(context.getString(R.string.notification_artist))
            .setIsBrowsable(true)
            .setIsPlayable(false)
        getArtworkBytes(context)?.let { artwork ->
            builder.setArtworkData(artwork, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
        }
        return builder.build()
    }

    fun toMediaItem(context: Context, station: RadioStation): MediaItem =
        MediaItem.Builder()
            .setMediaId(station.streamUrl)
            .setUri(station.streamUrl)
            .setMediaMetadata(metadata(context, station.displayName(context)))
            .build()

    fun downloadArtworkBytes(url: String, fallbackUrl: String? = null): ByteArray? =
        com.player.sovietwave.data.ArtworkDownloader
            .download(highResUrl = url, fallbackUrl = fallbackUrl ?: url)
            ?.bytes

    fun prepareForMetadata(bytes: ByteArray): ByteArray =
        try {
            scaleAndCompress(bytes, METADATA_MAX_DIMENSION_PX)
        } catch (_: Throwable) {
            bytes
        }

    fun decodeForDisplay(bytes: ByteArray): Bitmap? =
        runCatching {
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, displayBitmapOptions())
        }.getOrNull()

    private fun displayBitmapOptions(): BitmapFactory.Options =
        BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }

    private fun scaleAndCompress(bytes: ByteArray, maxDimension: Int): ByteArray {
        val source = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, displayBitmapOptions())
            ?: return bytes
        val scaled = scaleDown(source, maxDimension)
        return ByteArrayOutputStream().use { stream ->
            scaled.compress(Bitmap.CompressFormat.JPEG, METADATA_JPEG_QUALITY, stream)
            if (scaled !== source && !scaled.isRecycled) scaled.recycle()
            if (!source.isRecycled) source.recycle()
            stream.toByteArray()
        }
    }

    private fun scaleDown(source: Bitmap, maxDimension: Int): Bitmap {
        val largestSide = max(source.width, source.height)
        if (largestSide <= maxDimension) return source
        val scale = maxDimension.toFloat() / largestSide
        val targetWidth = (source.width * scale).roundToInt().coerceAtLeast(1)
        val targetHeight = (source.height * scale).roundToInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(source, targetWidth, targetHeight, true)
    }

    private fun isDecodableImage(bytes: ByteArray): Boolean {
        if (bytes.isEmpty()) return false
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        return options.outWidth > 0 && options.outHeight > 0
    }

    private fun encodeArtwork(context: Context): ByteArray? {
        val drawable = context.getDrawable(ARTWORK_RES) ?: return null
        val bitmap = drawable.toBitmap(ARTWORK_SIZE_PX, ARTWORK_SIZE_PX, Bitmap.Config.ARGB_8888)
        return ByteArrayOutputStream().use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            if (!bitmap.isRecycled) bitmap.recycle()
            stream.toByteArray()
        }
    }
}
