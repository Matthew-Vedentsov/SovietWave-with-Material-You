package com.player.sovietwave.data

import android.content.Context
import com.player.sovietwave.m3u.M3uParser

object StationRepository {

    @Volatile
    private var cachedStations: List<RadioStation>? = null

    fun getStations(context: Context): List<RadioStation> {
        cachedStations?.let { return it }
        return synchronized(this) {
            cachedStations ?: loadStations(context.applicationContext).also { cachedStations = it }
        }
    }

    private fun loadStations(context: Context): List<RadioStation> =
        context.assets.open("radio.m3u").use(M3uParser::parseAll)
}
