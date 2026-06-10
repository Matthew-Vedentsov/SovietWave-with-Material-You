package com.player.sovietwave.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.player.sovietwave.data.BroadcastHistoryItem
import com.player.sovietwave.data.FavoriteTrack
import com.player.sovietwave.data.FavoritesRepository
import com.player.sovietwave.data.WaveRadioApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface HistoryUiState {
    data object Loading : HistoryUiState
    data class Success(val items: List<BroadcastHistoryItem>) : HistoryUiState
    data class Error(val message: String? = null) : HistoryUiState
}

class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val favoritesRepository = FavoritesRepository(application)

    private val _uiState = MutableStateFlow<HistoryUiState>(HistoryUiState.Loading)
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    val favoriteKeys: StateFlow<Set<String>> = favoritesRepository.favorites
        .map { items -> items.map { it.key }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    init {
        loadHistory()
    }

    fun loadHistory() {
        viewModelScope.launch {
            _uiState.value = HistoryUiState.Loading
            val items = withContext(Dispatchers.IO) {
                runCatching { WaveRadioApi.fetchHistory() }.getOrNull()
            }
            _uiState.value = when {
                items == null -> HistoryUiState.Error()
                else -> HistoryUiState.Success(items)
            }
        }
    }

    fun toggleFavorite(item: BroadcastHistoryItem) {
        viewModelScope.launch {
            favoritesRepository.toggle(FavoriteTrack.from(item))
        }
    }
}
