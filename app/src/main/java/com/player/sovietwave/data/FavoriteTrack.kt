package com.player.sovietwave.data

data class FavoriteTrack(
    val artist: String,
    val trackTitle: String,
    val startTimeEpochSeconds: Long = 0,
    val artistLinks: List<String> = emptyList(),
) {
    val key: String
        get() = "${artist.trim().lowercase()}|${trackTitle.trim().lowercase()}"

    fun toBroadcastHistoryItem(): BroadcastHistoryItem =
        BroadcastHistoryItem(
            artist = artist,
            trackTitle = trackTitle,
            startTimeEpochSeconds = startTimeEpochSeconds,
            artistLinks = artistLinks,
        )

    companion object {
        fun from(item: BroadcastHistoryItem): FavoriteTrack =
            FavoriteTrack(
                artist = item.artist,
                trackTitle = item.trackTitle,
                startTimeEpochSeconds = item.startTimeEpochSeconds,
                artistLinks = item.artistLinks,
            )

        fun from(artist: String?, trackTitle: String?): FavoriteTrack? {
            val cleanArtist = artist?.trim().orEmpty()
            val cleanTitle = trackTitle?.trim().orEmpty()
            if (cleanArtist.isEmpty() && cleanTitle.isEmpty()) return null
            return FavoriteTrack(
                artist = cleanArtist,
                trackTitle = cleanTitle,
            )
        }
    }
}
