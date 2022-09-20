package com.mjaruijs.fischersplayground.networking

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities

class ConnectivityCallback(private val onNetworkAvailable: () -> Unit, private val onNetworkLost: () -> Unit) : ConnectivityManager.NetworkCallback() {

    private var connectedThroughWifi = false
    private var connectedThroughCellular = false

    override fun onAvailable(network: Network) {
        super.onAvailable(network)
//        println("Network available")
//        if (!connectedThroughWifi) {
            onNetworkAvailable()
//            connectedThroughWifi = true
//        }
    }

    override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
        super.onCapabilitiesChanged(network, networkCapabilities)

        if (!networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
            return
        }

//        println("Capabilities changed")
//
//        val hasWifi = networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
//        val hasMobileData = networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
//
//        if ()
//
//        println("hasWifi: $hasWifi. HasMobile: $hasMobileData")

//        if (!connected) {
//            onNetworkAvailable()
//            connectedThroughWifi = networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
//
//            connected = true
//        } else {
//            if (!connectedThroughWifi && networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
//                onNetworkLost()
//                onNetworkAvailable()
//
//                connected = true
//                connectedThroughWifi = true
//            }
//        }

    }

    override fun onLost(network: Network) {
        println("Network lost")
        super.onLost(network)

//        connected = false
//        connectedThroughWifi = false
//
//        onNetworkLost()
    }

}