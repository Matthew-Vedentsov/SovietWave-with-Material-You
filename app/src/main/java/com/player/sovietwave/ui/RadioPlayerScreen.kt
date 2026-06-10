package com.player.sovietwave.ui

import android.content.Intent
import android.content.res.Configuration
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.SubcomposeAsyncImage
import com.player.sovietwave.R
import com.player.sovietwave.data.FavoriteTrack
import com.player.sovietwave.data.RadioStation
import com.player.sovietwave.media.StationArtwork
import com.player.sovietwave.player.PlaybackState
import com.player.sovietwave.player.RadioUiState
import com.player.sovietwave.viewmodel.RadioViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RadioPlayerScreen(
    onHistoryClick: () -> Unit,
    onFavoritesClick: () -> Unit,
    viewModel: RadioViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val artworkUrl by viewModel.artworkUrl.collectAsStateWithLifecycle()
    val artworkBytes by viewModel.artworkBytes.collectAsStateWithLifecycle()
    val isArtworkLoading by viewModel.isArtworkLoading.collectAsStateWithLifecycle()
    val stations by viewModel.stations.collectAsStateWithLifecycle()
    val selectedIndex by viewModel.selectedIndex.collectAsStateWithLifecycle()
    val favoriteKeys by viewModel.favoriteKeys.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    Scaffold { innerPadding ->
        if (isLandscape) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center,
                ) {
                    StationArtwork(
                        isPlaying = uiState.playbackState == PlaybackState.Playing,
                        artworkUrl = artworkUrl,
                        artworkBytes = artworkBytes,
                        isArtworkLoading = isArtworkLoading,
                        size = 200.dp,
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    PlayerInfo(uiState)
                    PlayerControls(
                        uiState = uiState,
                        stations = stations,
                        selectedIndex = selectedIndex,
                        isFavorite = FavoriteTrack.from(uiState.artist, uiState.trackTitle)?.let { favoriteKeys.contains(it.key) } == true,
                        onSelectStation = viewModel::selectStation,
                        onTogglePlayPause = viewModel::togglePlayPause,
                        onToggleFavorite = viewModel::toggleFavorite,
                        onHistoryClick = onHistoryClick,
                        onFavoritesClick = onFavoritesClick,
                        formatLabel = { station -> station.formatLabel(context) },
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 32.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                StationArtwork(
                    isPlaying = uiState.playbackState == PlaybackState.Playing,
                    artworkUrl = artworkUrl,
                    artworkBytes = artworkBytes,
                    isArtworkLoading = isArtworkLoading,
                    size = 180.dp,
                )
                Spacer(modifier = Modifier.height(32.dp))
                PlayerInfo(uiState)
                PlayerControls(
                    uiState = uiState,
                    stations = stations,
                    selectedIndex = selectedIndex,
                    isFavorite = FavoriteTrack.from(uiState.artist, uiState.trackTitle)?.let { favoriteKeys.contains(it.key) } == true,
                    onSelectStation = viewModel::selectStation,
                    onTogglePlayPause = viewModel::togglePlayPause,
                    onToggleFavorite = viewModel::toggleFavorite,
                    onHistoryClick = onHistoryClick,
                    onFavoritesClick = onFavoritesClick,
                    formatLabel = { station -> station.formatLabel(context) },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlayerInfo(uiState: RadioUiState) {
    if (uiState.trackTitle != null || uiState.artist != null) {
        Text(
            text = uiState.stationTitle.ifEmpty { stringResource(R.string.default_station_name) },
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = uiState.trackTitle.orEmpty(),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )
        uiState.artist?.let { artist ->
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = artist,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    } else {
        Text(
            text = uiState.stationTitle.ifEmpty { stringResource(R.string.default_station_name) },
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )
    }

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = statusText(
            state = uiState.playbackState,
            errorMessage = uiState.errorMessage,
            hasTrackInfo = uiState.trackTitle != null || uiState.artist != null,
        ),
        style = MaterialTheme.typography.bodyLarge,
        color = when (uiState.playbackState) {
            PlaybackState.Error -> MaterialTheme.colorScheme.error
            PlaybackState.Playing -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        textAlign = TextAlign.Center,
    )

    Spacer(modifier = Modifier.height(8.dp))
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlayerControls(
    uiState: RadioUiState,
    stations: List<RadioStation>,
    selectedIndex: Int,
    isFavorite: Boolean,
    onSelectStation: (Int) -> Unit,
    onTogglePlayPause: () -> Unit,
    onToggleFavorite: () -> Unit,
    onHistoryClick: () -> Unit,
    onFavoritesClick: () -> Unit,
    formatLabel: (RadioStation) -> String,
) {
    if (stations.size > 1) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            modifier = Modifier.fillMaxWidth(),
        ) {
            stations.forEachIndexed { index, station ->
                FilterChip(
                    selected = index == selectedIndex,
                    onClick = { onSelectStation(index) },
                    label = { Text(formatLabel(station)) },
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    } else {
        Spacer(modifier = Modifier.height(16.dp))
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        val context = LocalContext.current
        // Ссылка (Artist Link)
        val artistLink = uiState.artistLinks.firstOrNull()
        IconButton(
            onClick = {
                artistLink?.let {
                    val intent = Intent(Intent.ACTION_VIEW, it.toUri())
                    context.startActivity(intent)
                }
            },
            modifier = Modifier.size(48.dp),
            enabled = artistLink != null
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = stringResource(R.string.artist_link),
                tint = if (artistLink != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
            )
        }

        FilledIconButton(
            onClick = onTogglePlayPause,
            modifier = Modifier.size(80.dp),
            shape = CircleShape,
        ) {
            when (uiState.playbackState) {
                PlaybackState.Buffering -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(36.dp),
                        strokeWidth = 3.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
                PlaybackState.Playing -> {
                    Icon(
                        imageVector = Icons.Default.Pause,
                        contentDescription = stringResource(R.string.pause),
                        modifier = Modifier.size(40.dp),
                    )
                }
                else -> {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = stringResource(R.string.play),
                        modifier = Modifier.size(40.dp),
                    )
                }
            }
        }

        // Лайк (Favorite)
        FavoriteLikeButton(
            isFavorite = isFavorite,
            onClick = onToggleFavorite,
            modifier = Modifier.size(48.dp),
            enabled = !uiState.artist.isNullOrEmpty()
        )
    }

    Spacer(modifier = Modifier.height(24.dp))

    OutlinedButton(onClick = onHistoryClick) {
        Text(stringResource(R.string.history_button))
    }

    Spacer(modifier = Modifier.height(12.dp))

    OutlinedButton(onClick = onFavoritesClick) {
        Text(stringResource(R.string.favorites_button))
    }
}

@Composable
private fun StationArtwork(
    isPlaying: Boolean,
    artworkUrl: String?,
    artworkBytes: ByteArray?,
    isArtworkLoading: Boolean,
    size: Dp,
) {
    val bitmap by produceState<ImageBitmap?>(initialValue = null, artworkBytes) {
        value = artworkBytes?.let { bytes ->
            withContext(Dispatchers.Default) {
                StationArtwork.decodeForDisplay(bytes)?.asImageBitmap()
            }
        }
    }
    val hasArtwork = bitmap != null || !artworkUrl.isNullOrBlank()
    val shape = if (hasArtwork) RoundedCornerShape(12.dp) else CircleShape

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isPlaying && !hasArtwork) 1.06f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "scale",
    )

    Surface(
        modifier = Modifier
            .size(size)
            .scale(if (isPlaying && !hasArtwork) scale else 1f),
        shape = shape,
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 6.dp,
    ) {
        when {
            bitmap != null -> {
                Image(
                    bitmap = bitmap!!,
                    contentDescription = stringResource(R.string.station_artwork),
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(shape),
                    contentScale = ContentScale.Crop,
                )
            }
            !artworkUrl.isNullOrBlank() -> {
                SubcomposeAsyncImage(
                    model = artworkUrl,
                    contentDescription = stringResource(R.string.station_artwork),
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(shape),
                    contentScale = ContentScale.Crop,
                    loading = {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(36.dp),
                                strokeWidth = 3.dp,
                            )
                        }
                    },
                    error = { RadioIconFallback() },
                )
            }
            isArtworkLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(36.dp),
                        strokeWidth = 3.dp,
                    )
                }
            }
            else -> {
                RadioIconFallback()
            }
        }
    }
}

@Composable
private fun RadioIconFallback() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Radio,
            contentDescription = stringResource(R.string.station_artwork),
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

@Composable
private fun statusText(
    state: PlaybackState,
    errorMessage: String?,
    hasTrackInfo: Boolean,
): String = when (state) {
    PlaybackState.Idle -> stringResource(R.string.status_idle)
    PlaybackState.Buffering -> stringResource(R.string.status_buffering)
    PlaybackState.Playing -> if (hasTrackInfo) {
        stringResource(R.string.status_playing)
    } else {
        stringResource(R.string.track_info_pending)
    }
    PlaybackState.Paused -> stringResource(R.string.status_paused)
    PlaybackState.Error -> errorMessage ?: stringResource(R.string.status_error)
}
