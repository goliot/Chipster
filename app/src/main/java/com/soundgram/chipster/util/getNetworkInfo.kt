package com.soundgram.chipster.util

import android.content.Context
import android.net.ConnectivityManager
import com.soundgram.chipster.domain.model.NetworkState

fun getNetworkInfo(context: Context): NetworkState {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val activeNetwork = cm.activeNetworkInfo
    var networkInfo = NetworkState.NONE_STATE
    if (activeNetwork != null) {
        if (activeNetwork.type == ConnectivityManager.TYPE_WIFI) {
            networkInfo = NetworkState.WIFI
        } else if (activeNetwork.type == ConnectivityManager.TYPE_MOBILE) {
            networkInfo = NetworkState.MOBILE
        }
    }
    return networkInfo
}
