package com.player.sovietwave.media

import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.Inet4Address
import java.net.InetAddress
import java.util.concurrent.TimeUnit

object ArtworkHttpClient {

    const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

    /**
     * Force IPv4 for Apple CDNs. Some Android devices have broken IPv6 stacks 
     * or the network/ISP has issues routing IPv6 to Apple, causing ConnectExceptions.
     */
    private val forceIpv4Dns = object : Dns {
        override fun lookup(hostname: String): List<InetAddress> {
            val addresses = Dns.SYSTEM.lookup(hostname)
            val ipv4 = addresses.filterIsInstance<Inet4Address>()
            return ipv4.ifEmpty { addresses }
        }
    }

    private fun baseBuilder(): OkHttpClient.Builder =
        OkHttpClient.Builder()
            .dns(forceIpv4Dns)
            .followRedirects(true)
            .followSslRedirects(true)
            .retryOnConnectionFailure(true)

    /** Shared client for Coil and quick thumbnail loads. */
    val client: OkHttpClient by lazy {
        baseBuilder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .callTimeout(45, TimeUnit.SECONDS)
            .build()
    }

    /** Longer timeouts for 800×800 artwork downloads. */
    val highResClient: OkHttpClient by lazy {
        baseBuilder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .callTimeout(40, TimeUnit.SECONDS)
            .build()
    }

    /** Shorter timeouts for 100×100 fallback. */
    val fallbackClient: OkHttpClient by lazy {
        baseBuilder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .callTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    fun imageRequest(url: String): Request =
        Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .build()
}
