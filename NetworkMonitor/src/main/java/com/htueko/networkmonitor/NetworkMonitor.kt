package com.htueko.networkmonitor

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import androidx.annotation.IntRange
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine


@ExperimentalCoroutinesApi
class NetworkMonitor
@RequiresPermission(android.Manifest.permission.ACCESS_NETWORK_STATE)
private constructor(
    application: Application
) {

    private val connectivityManager =
        application.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE)
                as ConnectivityManager

    // general availability of Internet over any type
    var isOnline = false
        get() {
            updateFields()
            return field
        }

    var isOverWifi = false
        get() {
            updateFields()
            return field
        }

    var isOverCellular = false
        get() {
            updateFields()
            return field
        }

    var isOverEthernet = false
        get() {
            updateFields()
            return field
        }

    companion object {
        @Volatile
        private var INSTANCE: NetworkMonitor? = null

        fun getInstance(application: Application): NetworkMonitor {
            synchronized(this) {
                if (INSTANCE == null) {
                    INSTANCE = NetworkMonitor(application)
                }
                return INSTANCE!!
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun updateFields() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            val networkAvailability =
                connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)

            if (networkAvailability != null &&
                networkAvailability.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                networkAvailability.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            ) {
                //has network
                isOnline = true

                // wifi
                isOverWifi =
                    networkAvailability.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)

                // cellular
                isOverCellular =
                    networkAvailability.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)

                // ethernet
                isOverEthernet =
                    networkAvailability.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
            } else {
                isOnline = false
                isOverWifi = false
                isOverCellular = false
                isOverEthernet = false
            }
        } else {

            val info = connectivityManager.activeNetworkInfo
            if (info != null && info.isConnected) {
                isOnline = true

                val wifi = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
                isOverWifi = wifi != null && wifi.isConnected

                val cellular = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE)
                isOverCellular = cellular != null && cellular.isConnected

                val ethernet = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_ETHERNET)
                isOverEthernet = ethernet != null && ethernet.isConnected

            } else {
                isOnline = false
                isOverWifi = false
                isOverCellular = false
                isOverEthernet = false
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun watchNetwork(): Flow<Boolean> = watchWifi()
        .combine(watchCellular()) { wifi, cellular -> wifi || cellular }
        .combine(watchEthernet()) { wifiAndCellular, ethernet -> wifiAndCellular || ethernet }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun watchNetworkAsLiveData(): LiveData<Boolean> = watchNetwork().asLiveData()

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun watchWifi(): Flow<Boolean> = callbackFlowForType(NetworkCapabilities.TRANSPORT_WIFI)

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun watchWifiAsLiveData() = watchWifi().asLiveData()

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun watchCellular(): Flow<Boolean> = callbackFlowForType(NetworkCapabilities.TRANSPORT_CELLULAR)

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun watchCellularAsLiveData() = watchCellular().asLiveData()

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun watchEthernet(): Flow<Boolean> = callbackFlowForType(NetworkCapabilities.TRANSPORT_ETHERNET)

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun watchEthernetAsLiveData() = watchEthernet().asLiveData()

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun callbackFlowForType(@IntRange(from = 0, to = 7) type: Int) = callbackFlow {

        offer(false)

        val networkRequest = NetworkRequest.Builder()
            .addTransportType(type)
            .build()

        val callback = object : ConnectivityManager.NetworkCallback() {

            override fun onLost(network: Network) {
                offer(false)
            }

            override fun onUnavailable() {
                offer(false)
            }

            override fun onLosing(network: Network, maxMsToLive: Int) {
                // do nothing
            }

            override fun onAvailable(network: Network) {
                offer(true)
            }
        }

        connectivityManager.registerNetworkCallback(networkRequest, callback)

        awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
    }
}