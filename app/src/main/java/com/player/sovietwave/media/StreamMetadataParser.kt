package com.player.sovietwave.media

import androidx.media3.common.Metadata
import androidx.media3.extractor.metadata.icy.IcyInfo
import androidx.media3.extractor.metadata.id3.TextInformationFrame

data class StreamTrackInfo(
    val title: String? = null,
    val artist: String? = null,
)

object StreamMetadataParser {

    private val TITLE_SEPARATORS = listOf(" - ", " — ", " – ")

    fun parse(metadata: Metadata): StreamTrackInfo {
        for (index in 0 until metadata.length()) {
            when (val entry = metadata[index]) {
                is IcyInfo -> entry.title?.let(::parseStreamTitle)?.let { return it }
                is TextInformationFrame -> {
                    when (entry.id) {
                        "TIT2" -> {
                            val parsed = parseStreamTitle(entry.value.orEmpty())
                            if (parsed.title != null || parsed.artist != null) return parsed
                        }
                        "TPE1" -> {
                            return StreamTrackInfo(artist = entry.value?.trim()?.takeIf { it.isNotEmpty() })
                        }
                    }
                }
            }
        }
        return StreamTrackInfo()
    }

    fun parseStreamTitle(raw: String): StreamTrackInfo {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return StreamTrackInfo()

        for (separator in TITLE_SEPARATORS) {
            val index = trimmed.indexOf(separator)
            if (index > 0) {
                val artist = trimmed.substring(0, index).trim()
                val title = trimmed.substring(index + separator.length).trim()
                if (artist.isNotEmpty() && title.isNotEmpty()) {
                    return StreamTrackInfo(title = title, artist = artist)
                }
            }
        }

        return StreamTrackInfo(title = trimmed)
    }

    fun merge(
        title: String?,
        artist: String?,
        stationTitle: String,
    ): StreamTrackInfo {
        val cleanTitle = title?.trim()?.takeIf { it.isNotEmpty() }
        val cleanArtist = artist?.trim()?.takeIf { it.isNotEmpty() }

        if (cleanTitle == null && cleanArtist == null) {
            return StreamTrackInfo()
        }

        if (cleanArtist != null) {
            return StreamTrackInfo(
                title = cleanTitle?.takeUnless { it.equals(stationTitle, ignoreCase = true) },
                artist = cleanArtist,
            )
        }

        if (cleanTitle != null && !cleanTitle.equals(stationTitle, ignoreCase = true)) {
            val parsed = parseStreamTitle(cleanTitle)
            if (parsed.artist != null || parsed.title != null) {
                return parsed
            }
        }

        return StreamTrackInfo()
    }
}
