@file:JvmName("NetworkFunctions")

package com.serebit.autotitan.network

import com.serebit.logkat.Logger
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket

private const val HTTPS_PORT = 443
private const val DEFAULT_TIMEOUT = 4000

fun ping(host: String, timeout: Int = DEFAULT_TIMEOUT): Boolean = try {
    Socket().use { socket ->
        socket.connect(InetSocketAddress(host, HTTPS_PORT), timeout)
        true
    }
} catch (ex: IOException) {
    Logger.debug("Failed to open a socket to $host. ${ex.message ?: "No error message available."}")
    false
}
