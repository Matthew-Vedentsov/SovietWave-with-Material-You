package com.player.sovietwave.m3u

import com.player.sovietwave.data.RadioStation
import java.io.InputStream
import java.net.URL

object M3uParser {

    fun parseAll(input: InputStream): List<RadioStation> {
        val lines = input.bufferedReader().readLines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        val stations = mutableListOf<RadioStation>()
        var index = 0
        while (index < lines.size) {
            val line = lines[index]
            if (line.startsWith("#EXTINF:")) {
                val title = line.substringAfter(",", "Radio").trim()
                val nextIndex = index + 1
                if (nextIndex < lines.size && !lines[nextIndex].startsWith("#")) {
                    stations += RadioStation(title, lines[nextIndex])
                    index = nextIndex
                }
            } else if (!line.startsWith("#")) {
                stations += RadioStation("Radio", line)
            }
            index++
        }

        require(stations.isNotEmpty()) { "M3U file does not contain a stream URL" }
        return stations
    }

    fun parse(input: InputStream): RadioStation =
        parseAll(input).let { stations ->
            stations.find { it.streamUrl.endsWith(".mp3", ignoreCase = true) }
                ?: stations.last()
        }

    fun parseFromUrl(m3uUrl: String): RadioStation =
        URL(m3uUrl).openStream().use(::parse)
}
