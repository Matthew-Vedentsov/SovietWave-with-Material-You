package com.player.sovietwave

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.player.sovietwave.media.ArtworkHttpClient

class SovietWaveApplication : Application(), ImageLoaderFactory {

    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .okHttpClient(ArtworkHttpClient.client)
            .crossfade(true)
            .build()
}
