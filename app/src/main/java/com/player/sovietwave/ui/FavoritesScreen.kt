package com.player.sovietwave.ui

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.player.sovietwave.R
import com.player.sovietwave.data.FavoriteTrack
import com.player.sovietwave.viewmodel.FavoritesViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    onBack: () -> Unit,
    viewModel: FavoritesViewModel = viewModel(),
) {
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.favorites_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        if (favorites.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.favorites_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(
                    items = favorites,
                    key = { track -> track.key },
                ) { track ->
                    FavoriteListItem(
                        track = track,
                        isFavorite = true,
                        onToggleFavorite = { viewModel.toggleFavorite(track) },
                    )
                }
            }
        }
    }
}

@Composable
fun FavoriteListItem(
    track: FavoriteTrack,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
) {
    val context = LocalContext.current
    val artistLink = track.artistLinks.firstOrNull()

    ListItem(
        headlineContent = {
            Text(
                text = track.trackTitle.ifEmpty { track.artist },
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        },
        supportingContent = {
            if (track.trackTitle.isNotEmpty() && track.artist.isNotEmpty()) {
                Text(
                    text = track.artist,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        overlineContent = if (track.startTimeEpochSeconds > 0) {
            {
                Text(formatFavoriteTime(track.startTimeEpochSeconds))
            }
        } else {
            null
        },
        trailingContent = {
            Row {
                FavoriteLikeButton(
                    isFavorite = isFavorite,
                    onClick = onToggleFavorite,
                )
                if (artistLink != null) {
                    IconButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, artistLink.toUri())
                            context.startActivity(intent)
                        },
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = stringResource(R.string.artist_link),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        },
    )
}

@Composable
private fun formatFavoriteTime(epochSeconds: Long): String {
    val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
        .withLocale(Locale.getDefault())
    return Instant.ofEpochSecond(epochSeconds)
        .atZone(ZoneId.systemDefault())
        .format(formatter)
}
