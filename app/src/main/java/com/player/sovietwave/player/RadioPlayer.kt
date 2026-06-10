package com.player.sovietwave.player

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.player.sovietwave.data.RadioStation
import com.player.sovietwave.media.StationArtwork
import com.player.sovietwave.media.StreamTrackInfo
import com.player.sovietwave.service.RadioPlaybackService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class PlaybackState {
    Idle,
    Buffering,
    Playing,
    Paused,
    Error,
}

data class RadioUiState(
    val stationTitle: String = "",
    val trackTitle: String? = null,
    val artist: String? = null,
    val artistLinks: List<String> = emptyList(),
    val playbackState: PlaybackState = PlaybackState.Idle,
    val errorMessage: String? = null,
)

class RadioPlayer(context: Context) {

    private val appContext = context.applicationContext
    private val sessionToken = SessionToken(
        appContext,
        ComponentName(appContext, RadioPlaybackService::class.java),
    )
    private val controllerFuture: ListenableFuture<MediaController> =
        MediaController.Builder(appContext, sessionToken).buildAsync()

    private var controller: MediaController? = null
    private var pendingStation: RadioStation? = null
    private var trackArtworkBytes: ByteArray? = null

    private val _uiState = MutableStateFlow(RadioUiState())
    val uiState: StateFlow<RadioUiState> = _uiState.asStateFlow()

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            updatePlaybackState()
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            updatePlaybackState()
        }

        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            _uiState.update {
                it.copy(
                    playbackState = PlaybackState.Error,
                    errorMessage = error.message ?: "Playback error",
                )
            }
        }
    }

    init {
        controllerFuture.addListener(
            {
                runCatching { controllerFuture.get() }.onSuccess { mediaController ->
                    controller = mediaController
                    mediaController.addListener(playerListener)
                    pendingStation?.let { loadStation(it) }
                    pendingStation = null
                    updatePlaybackState()
                }
            },
            MoreExecutors.directExecutor(),
        )
    }

    fun loadStation(station: RadioStation) {
        val stationTitle = station.displayName(appContext)
        val mediaController = controller
        if (mediaController == null) {
            pendingStation = station
            trackArtworkBytes = null
            _uiState.update {
                it.copy(
                    stationTitle = stationTitle,
                    trackTitle = null,
                    artist = null,
                    playbackState = PlaybackState.Idle,
                    errorMessage = null,
                )
            }
            return
        }

        trackArtworkBytes = null
        _uiState.update {
            it.copy(
                stationTitle = stationTitle,
                trackTitle = null,
                artist = null,
                playbackState = PlaybackState.Idle,
                errorMessage = null,
            )
        }
        mediaController.stop()
        mediaController.setMediaItem(StationArtwork.toMediaItem(appContext, station))
        mediaController.prepare()
    }

    fun updateNowPlaying(trackInfo: StreamTrackInfo, artistLinks: List<String> = emptyList()) {
        if (trackInfo.title == null && trackInfo.artist == null) return
        _uiState.update {
            it.copy(
                trackTitle = trackInfo.title,
                artist = trackInfo.artist,
                artistLinks = artistLinks,
            )
        }
        updateMediaSessionMetadata(trackInfo, trackArtworkBytes)
    }

    fun updateTrackArtwork(artworkBytes: ByteArray?) {
        trackArtworkBytes = artworkBytes
        val state = _uiState.value
        if (state.trackTitle == null && state.artist == null) return
        updateMediaSessionMetadata(
            StreamTrackInfo(title = state.trackTitle, artist = state.artist),
            artworkBytes,
        )
    }

    fun togglePlayPause() {
        val mediaController = controller ?: return
        if (mediaController.playbackState == Player.STATE_IDLE) return
        if (mediaController.isPlaying) {
            mediaController.pause()
        } else {
            mediaController.play()
        }
    }

    fun release() {
        controller?.removeListener(playerListener)
        MediaController.releaseFuture(controllerFuture)
        controller = null
    }

    private fun updateMediaSessionMetadata(
        trackInfo: StreamTrackInfo,
        artworkBytes: ByteArray? = trackArtworkBytes,
    ) {
        val mediaController = controller ?: return
        val currentItem = mediaController.currentMediaItem ?: return
        val stationTitle = _uiState.value.stationTitle

        val metadata = currentItem.mediaMetadata.buildUpon().apply {
            trackInfo.title?.let(::setTitle)
            trackInfo.artist?.let(::setArtist)
            setAlbumTitle(stationTitle)
            applyArtwork(artworkBytes)
        }.build()

        val updatedItem = currentItem.buildUpon()
            .setMediaMetadata(metadata)
            .build()

        mediaController.replaceMediaItem(
            mediaController.currentMediaItemIndex,
            updatedItem,
        )
    }

    private fun MediaMetadata.Builder.applyArtwork(artworkBytes: ByteArray?) {
        if (artworkBytes != null) {
            setArtworkData(artworkBytes, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
        } else {
            StationArtwork.getArtworkBytes(appContext)?.let { defaultArtwork ->
                setArtworkData(defaultArtwork, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
            }
        }
    }

    private fun updatePlaybackState() {
        val mediaController = controller ?: return
        _uiState.update { state ->
            state.copy(
                playbackState = when (mediaController.playbackState) {
                    Player.STATE_BUFFERING -> PlaybackState.Buffering
                    Player.STATE_READY -> if (mediaController.isPlaying) {
                        PlaybackState.Playing
                    } else {
                        PlaybackState.Paused
                    }
                    Player.STATE_IDLE -> PlaybackState.Idle
                    else -> state.playbackState
                },
                errorMessage = null,
            )
        }
    }
}
