package com.soundgram.chipster

import android.app.Application
import android.net.Uri
import com.google.firebase.FirebaseApp

class ChipsterApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
    }

    companion object {
        var token: String? = null
        var MAIN_URL: String = "http://chipsterplay.soundgram.co.kr"
        var pushMovingPage: String = "none" // none or click_action
        var totId: Int = 2 //
        var startingNetwork: String = "NONE_STATE"
        const val ALBUM_TITLE = "Chipster"

        // TODO 앱버전 작성
        const val APP_VERSION = "1.1.2"
    }
}