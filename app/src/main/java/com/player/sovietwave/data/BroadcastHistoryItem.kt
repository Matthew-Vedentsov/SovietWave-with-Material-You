package com.player.sovietwave.data

data class BroadcastHistoryItem(
    val artist: String,
    val trackTitle: String,
    val startTimeEpochSeconds: Long,
    val artistLinks: List<String> = emptyList(),
)
