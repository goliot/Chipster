package com.soundgram.chipster.domain.model

enum class NetworkState(private val value: String) {
    NONE_STATE("NONE_STATE"), MOBILE("MOBILE"), WIFI("WIFI");

    fun getName(): String = this.value
}
