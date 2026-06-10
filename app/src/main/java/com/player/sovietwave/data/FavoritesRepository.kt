package com.player.sovietwave.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.favoritesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "favorites",
)

class FavoritesRepository(context: Context) {

    private val dataStore = context.applicationContext.favoritesDataStore

    val favorites: Flow<List<FavoriteTrack>> = dataStore.data.map { preferences ->
        parseFavorites(preferences[FAVORITES_JSON_KEY].orEmpty())
    }

    suspend fun toggle(track: FavoriteTrack) {
        dataStore.edit { preferences ->
            val current = parseFavorites(preferences[FAVORITES_JSON_KEY].orEmpty()).toMutableList()
            val existingIndex = current.indexOfFirst { it.key == track.key }
            if (existingIndex >= 0) {
                current.removeAt(existingIndex)
            } else {
                current.add(0, track)
            }
            preferences[FAVORITES_JSON_KEY] = encodeFavorites(current)
        }
    }

    private fun parseFavorites(json: String): List<FavoriteTrack> {
        if (json.isBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(json)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    val artist = item.optString("artist").trim()
                    val trackTitle = item.optString("track_title").trim()
                    if (artist.isEmpty() && trackTitle.isEmpty()) continue
                    add(
                        FavoriteTrack(
                            artist = artist,
                            trackTitle = trackTitle,
                            startTimeEpochSeconds = item.optLong("start_time"),
                            artistLinks = item.optJSONArray("artist_links").toLinkTexts(),
                        ),
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun encodeFavorites(favorites: List<FavoriteTrack>): String {
        val array = JSONArray()
        favorites.forEach { track ->
            array.put(
                JSONObject().apply {
                    put("artist", track.artist)
                    put("track_title", track.trackTitle)
                    put("start_time", track.startTimeEpochSeconds)
                    put(
                        "artist_links",
                        JSONArray().apply {
                            track.artistLinks.forEach { link ->
                                put(JSONObject().put("link_text", link))
                            }
                        },
                    )
                },
            )
        }
        return array.toString()
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

    companion object {
        private val FAVORITES_JSON_KEY = stringPreferencesKey("favorites_json")
    }
}
