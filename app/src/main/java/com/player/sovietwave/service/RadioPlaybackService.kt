package com.player.sovietwave.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionError
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.player.sovietwave.MainActivity
import com.player.sovietwave.R
import com.player.sovietwave.data.StationRepository
import com.player.sovietwave.media.StationArtwork

class RadioPlaybackService : MediaLibraryService() {

    private var mediaLibrarySession: MediaLibrarySession? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        setMediaNotificationProvider(
            DefaultMediaNotificationProvider.Builder(this)
                .setNotificationId(NOTIFICATION_ID)
                .setChannelId(CHANNEL_ID)
                .build()
                .apply {
                    setSmallIcon(R.drawable.ic_notification)
                },
        )

        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true,
            )
            .setHandleAudioBecomingNoisy(true)
            .build()

        mediaLibrarySession = MediaLibrarySession.Builder(this, player, LibrarySessionCallback())
            .setSessionActivity(createSessionActivityIntent())
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? =
        mediaLibrarySession

    override fun onDestroy() {
        mediaLibrarySession?.run {
            player.release()
            release()
        }
        mediaLibrarySession = null
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.notification_channel_description)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun createSessionActivityIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        return PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private inner class LibrarySessionCallback : MediaLibrarySession.Callback {

        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: MediaLibraryService.LibraryParams?,
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val rootItem = MediaItem.Builder()
                .setMediaId(ROOT_ID)
                .setMediaMetadata(StationArtwork.browseRootMetadata(this@RadioPlaybackService))
                .build()
            return Futures.immediateFuture(LibraryResult.ofItem(rootItem, params))
        }

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: MediaLibraryService.LibraryParams?,
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            if (parentId != ROOT_ID) {
                return Futures.immediateFuture(
                    LibraryResult.ofError(SessionError.ERROR_BAD_VALUE),
                )
            }
            val items = StationRepository.getStations(this@RadioPlaybackService)
                .map { station -> StationArtwork.toMediaItem(this@RadioPlaybackService, station) }
            return Futures.immediateFuture(LibraryResult.ofItemList(ImmutableList.copyOf(items), params))
        }

        override fun onGetItem(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            mediaId: String,
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val station = StationRepository.getStations(this@RadioPlaybackService)
                .find { it.streamUrl == mediaId }
                ?: return Futures.immediateFuture(
                    LibraryResult.ofError(SessionError.ERROR_BAD_VALUE),
                )
            val item = StationArtwork.toMediaItem(this@RadioPlaybackService, station)
            return Futures.immediateFuture(LibraryResult.ofItem(item, null))
        }

        override fun onPlaybackResumption(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            val player = mediaSession.player
            val currentItem = player.currentMediaItem
            if (currentItem != null) {
                return Futures.immediateFuture(
                    MediaSession.MediaItemsWithStartPosition(
                        ImmutableList.of(currentItem),
                        player.currentMediaItemIndex,
                        player.currentPosition,
                    ),
                )
            }
            val stations = StationRepository.getStations(this@RadioPlaybackService)
            val defaultStation = stations.find {
                it.streamUrl.endsWith(".mp3", ignoreCase = true)
            } ?: stations.firstOrNull()
                ?: return Futures.immediateFuture(
                    MediaSession.MediaItemsWithStartPosition(ImmutableList.of(), 0, 0),
                )
            val mediaItem = StationArtwork.toMediaItem(this@RadioPlaybackService, defaultStation)
            return Futures.immediateFuture(
                MediaSession.MediaItemsWithStartPosition(ImmutableList.of(mediaItem), 0, 0),
            )
        }
    }

    companion object {
        const val ROOT_ID = "sovietwave_root"
        private const val CHANNEL_ID = "radio_playback"
        private const val NOTIFICATION_ID = 1
    }
}
