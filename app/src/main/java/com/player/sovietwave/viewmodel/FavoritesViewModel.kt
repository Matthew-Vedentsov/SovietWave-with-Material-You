package com.player.sovietwave.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.player.sovietwave.data.FavoriteTrack
import com.player.sovietwave.data.FavoritesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FavoritesViewModel(application: Application) : AndroidViewModel(application) {

    private val favoritesRepository = FavoritesRepository(application)

    val favorites: StateFlow<List<FavoriteTrack>> = favoritesRepository.favorites
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val favoriteKeys: StateFlow<Set<String>> = favorites
        .map { items -> items.map { it.key }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    fun toggleFavorite(track: FavoriteTrack) {
        viewModelScope.launch {
            favoritesRepository.toggle(track)
        }
    }
}
