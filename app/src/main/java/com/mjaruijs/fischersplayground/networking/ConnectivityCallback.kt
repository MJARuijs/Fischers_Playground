package com.mjaruijs.fischersplayground.networking

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities

class ConnectivityCallback(private val onNetworkAvailable: () -> Unit, private val onNetworkLost: () -> Unit) : ConnectivityManager.NetworkCallback() {

    private var connected = false

    private var connectedThroughWifi = false
    private var connectedThroughCellular = false

    override fun onAvailable(network: Network) {
        super.onAvailable(network)
//        if (!connectedThroughWifi) {
//        if (!connected) {
//            println("Network available")
//
//            connected = true
//            onNetworkAvailable()
//        }

//            connectedThroughWifi = true
//        }
    }

    override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
        super.onCapabilitiesChanged(network, networkCapabilities)

        if (!networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
            return
        }

        val hasWifi = networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        val hasMobileData = networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)

        println("hasWifi: $hasWifi. HasMobile: $hasMobileData")

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

//        if (connected) {
//            if (connectedThroughWifi) {
//
//            } else {
//                onNetworkLost()
//                onNetworkAvailable()
//            }
//        }


//        if (!connected) {
//            connectedThroughWifi = hasWifi
////            if (connected) {
////                onNetworkLost()
////            }
//            onNetworkAvailable()
//
//            connected = true
//        }

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

        connected = false
        connectedThroughWifi = false

        onNetworkLost()
    }

}