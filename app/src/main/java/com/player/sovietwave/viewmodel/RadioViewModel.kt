package com.player.sovietwave.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.player.sovietwave.data.ArtworkLoadResult
import com.player.sovietwave.data.FavoriteTrack
import com.player.sovietwave.data.FavoritesRepository
import com.player.sovietwave.data.ITunesArtworkApi
import com.player.sovietwave.data.RadioStation
import com.player.sovietwave.data.StationRepository
import com.player.sovietwave.data.WaveRadioApi
import com.player.sovietwave.data.loadArtwork
import com.player.sovietwave.media.StreamTrackInfo
import com.player.sovietwave.player.PlaybackState
import com.player.sovietwave.player.RadioPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RadioViewModel(application: Application) : AndroidViewModel(application) {

    private val radioPlayer = RadioPlayer(application)
    val uiState = radioPlayer.uiState

    private val favoritesRepository = FavoritesRepository(application)
    val favoriteKeys: StateFlow<Set<String>> = favoritesRepository.favorites
        .map { list -> list.map { it.key }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    private val _artworkUrl = MutableStateFlow<String?>(null)
    val artworkUrl: StateFlow<String?> = _artworkUrl.asStateFlow()

    private val _artworkBytes = MutableStateFlow<ByteArray?>(null)
    val artworkBytes: StateFlow<ByteArray?> = _artworkBytes.asStateFlow()

    private val _isArtworkLoading = MutableStateFlow(false)
    val isArtworkLoading: StateFlow<Boolean> = _isArtworkLoading.asStateFlow()

    private val _stations = MutableStateFlow<List<RadioStation>>(emptyList())
    val stations: StateFlow<List<RadioStation>> = _stations.asStateFlow()

    private val _selectedIndex = MutableStateFlow(0)
    val selectedIndex: StateFlow<Int> = _selectedIndex.asStateFlow()

    private var trackPollingJob: Job? = null
    private var artworkJob: Job? = null
    private var lastArtworkTrack: Pair<String, String>? = null
    private var artworkUnavailableTrack: Pair<String, String>? = null

    init {
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    StationRepository.getStations(getApplication())
                }
            }.onSuccess { loadedStations ->
                _stations.value = loadedStations
                val defaultIndex = loadedStations.indexOfFirst {
                    it.streamUrl.endsWith(".mp3", ignoreCase = true)
                }.takeIf { it >= 0 } ?: 0
                selectStation(defaultIndex)
            }
        }

        viewModelScope.launch {
            uiState
                .map { it.playbackState == PlaybackState.Playing }
                .distinctUntilChanged()
                .collect { isPlaying ->
                    if (isPlaying) {
                        startTrackPolling()
                    } else {
                        stopTrackPolling()
                    }
                }
        }
    }

    fun selectStation(index: Int) {
        if (index == _selectedIndex.value) return
        val station = _stations.value.getOrNull(index) ?: return
        _selectedIndex.value = index
        clearArtwork()
        radioPlayer.loadStation(station)
        viewModelScope.launch {
            fetchCurrentTrack()
        }
    }

    fun togglePlayPause() {
        radioPlayer.togglePlayPause()
    }

    fun toggleFavorite() {
        val state = uiState.value
        val track = FavoriteTrack.from(state.artist, state.trackTitle) ?: return
        viewModelScope.launch {
            favoritesRepository.toggle(track)
        }
    }

    override fun onCleared() {
        stopTrackPolling()
        artworkJob?.cancel()
        radioPlayer.release()
        super.onCleared()
    }

    private fun clearArtwork() {
        artworkJob?.cancel()
        _artworkUrl.value = null
        _artworkBytes.value = null
        _isArtworkLoading.value = false
        lastArtworkTrack = null
        artworkUnavailableTrack = null
        radioPlayer.updateTrackArtwork(null)
    }

    private fun startTrackPolling() {
        trackPollingJob?.cancel()
        trackPollingJob = viewModelScope.launch {
            while (isActive) {
                fetchCurrentTrack()
                delay(TRACK_POLL_INTERVAL_MS)
            }
        }
    }

    private fun stopTrackPolling() {
        trackPollingJob?.cancel()
        trackPollingJob = null
    }

    private suspend fun fetchCurrentTrack() {
        val trackInfo = withContext(Dispatchers.IO) {
            runCatching { WaveRadioApi.fetchCurrentTrack() }.getOrNull()
        } ?: return
        
        // Fetch history to get artist links for the current track
        val history = withContext(Dispatchers.IO) {
            runCatching { WaveRadioApi.fetchHistory(amount = 5) }.getOrNull()
        }
        
        val links = history?.find { 
            it.artist.equals(trackInfo.artist, ignoreCase = true) && 
            it.trackTitle.equals(trackInfo.title, ignoreCase = true)
        }?.artistLinks ?: emptyList()

        radioPlayer.updateNowPlaying(trackInfo, links)
        fetchArtwork(trackInfo)
    }

    private fun fetchArtwork(trackInfo: StreamTrackInfo) {
        val trackKey = normalizeTrackKey(trackInfo.artist, trackInfo.title)
        if (trackKey.first.isEmpty() && trackKey.second.isEmpty()) return
        if (trackKey == artworkUnavailableTrack) return

        if (artworkJob?.isActive == true && trackKey == lastArtworkTrack) return

        if (trackKey == lastArtworkTrack && _artworkBytes.value != null) return

        if (trackKey != lastArtworkTrack) {
            artworkJob?.cancel()
            lastArtworkTrack = trackKey
            artworkUnavailableTrack = null
            _artworkUrl.value = null
            _artworkBytes.value = null
            radioPlayer.updateTrackArtwork(null)
        }

        val artist = trackInfo.artist
        val title = trackInfo.title

        artworkJob = viewModelScope.launch {
            _isArtworkLoading.value = true
            try {
                val result = withContext(Dispatchers.IO) {
                    ITunesArtworkApi.loadArtwork(artist, title)
                }

                if (trackKey != lastArtworkTrack) return@launch

                when (result) {
                    ArtworkLoadResult.NotFound -> {
                        artworkUnavailableTrack = trackKey
                    }
                    ArtworkLoadResult.Failed -> {
                    }
                    is ArtworkLoadResult.UrlOnly -> {
                        _artworkUrl.value = result.url
                    }
                    is ArtworkLoadResult.Success -> {
                        _artworkUrl.value = result.url
                        _artworkBytes.value = result.displayBytes
                        radioPlayer.updateTrackArtwork(result.metadataBytes)
                    }
                }
            } finally {
                if (trackKey == lastArtworkTrack) {
                    _isArtworkLoading.value = false
                }
            }
        }
    }

    private fun normalizeTrackKey(artist: String?, title: String?): Pair<String, String> =
        artist?.trim()?.lowercase().orEmpty() to title?.trim()?.lowercase().orEmpty()

    companion object {
        private const val TRACK_POLL_INTERVAL_MS = 10_000L
    }
}
