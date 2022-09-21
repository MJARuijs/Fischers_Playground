package com.mjaruijs.fischersplayground.networking

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities

class ConnectivityCallback(private val onNetworkAvailable: () -> Unit, private val onNetworkLost: () -> Unit) : ConnectivityManager.NetworkCallback() {

    private var connected = false

    private var connectedThroughWifi = false

    override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
        super.onCapabilitiesChanged(network, networkCapabilities)

        if (!networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
            return
        }

        val hasWifi = networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)

        if (!connected) {
            connected = true
            connectedThroughWifi = hasWifi
            onNetworkAvailable()
        } else if (!connectedThroughWifi) {
            if (hasWifi) {
                onNetworkLost()
                onNetworkAvailable()
            }
            connectedThroughWifi = hasWifi
        }
    }

    override fun onLost(network: Network) {
        super.onLost(network)

        connected = false
        connectedThroughWifi = false

        onNetworkLost()
    }

}