package com.oddlemon.chipsterplay.util

import android.webkit.WebSettings

fun WebSettings.baseWebViewSetting() {
    javaScriptEnabled = true
    javaScriptCanOpenWindowsAutomatically = true
    loadWithOverviewMode = true
    useWideViewPort = true
    allowFileAccess = true
    allowContentAccess = true
    allowFileAccessFromFileURLs = true
    allowUniversalAccessFromFileURLs = true
    setRenderPriority(WebSettings.RenderPriority.HIGH)
    layoutAlgorithm = WebSettings.LayoutAlgorithm.SINGLE_COLUMN
    setEnableSmoothTransition(true)
    cacheMode = WebSettings.LOAD_NO_CACHE
    domStorageEnabled = true
    textZoom = 100
    mediaPlaybackRequiresUserGesture = false
    setSupportMultipleWindows(true)
}

