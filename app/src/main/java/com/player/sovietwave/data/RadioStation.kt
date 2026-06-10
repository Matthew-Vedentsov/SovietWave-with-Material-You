package com.player.sovietwave.data

import android.content.Context
import com.player.sovietwave.R

data class RadioStation(
    val title: String,
    val streamUrl: String,
) {
    fun displayName(context: Context): String =
        context.getString(R.string.default_station_name)

    fun formatLabel(context: Context): String {
        val resourceId = when {
            streamUrl.endsWith(".mp3", ignoreCase = true) -> R.string.format_mp3
            streamUrl.contains("soviet", ignoreCase = true) -> R.string.format_aac
            else -> null
        }
        return resourceId?.let(context::getString) ?: title
    }
}
