package com.soundgram.chipster.view

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.soundgram.chipster.ChipsterApplication
import com.soundgram.chipster.databinding.ActivitySplashBinding
import com.soundgram.chipster.domain.model.NetworkState
import com.soundgram.chipster.util.userid.UniqueDeviceID
import com.soundgram.chipster.util.baseWebViewSetting
import com.soundgram.chipster.util.getNetworkInfo
import com.soundgram.chipster.view.dialog.NetworkDialog
import org.apache.http.util.EncodingUtils
import kotlin.system.exitProcess

class SplashActivity : AppCompatActivity() {

    private lateinit var pendingIntent: PendingIntent
    private var SPLASH_URL = ""

    //status bar의 높이 계산
    private val statusBarHeight: Int
        get() {
            var result = 0
            val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
            if (resourceId > 0) result = resources.getDimensionPixelSize(resourceId)
            return result
        }

    private lateinit var binding: ActivitySplashBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i("dlgocks1", "[Activity] - SplashActivity")
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
//            val layoutParams = WindowManager.LayoutParams()
//            layoutParams.layoutInDisplayCutoutMode =
//                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
//            window.attributes = layoutParams
//            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
//            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
//        }
//        setFullScreen()
        // AppCompact Toolbar 인스턴스 얻기
//        binding.splashFrame.setPadding(0, statusBarHeight, 0, 0)

        //액티비티간 이동시 효과제거
        window.setWindowAnimations(0)
        SPLASH_URL = ChipsterApplication.MAIN_URL + "/login.php"
//            ChipsterApplication.MAIN_URL + "/margdnuoseae.php?tot_id=" + ChipsterApplication.totId

        networkCheck()
        runWebview()
    }

    @SuppressLint("ClickableViewAccessibility")
    fun runWebview() {
        Log.d("splash-uuid", UniqueDeviceID.getUniqueID(this@SplashActivity))
        val intent = Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        val webViewSetting = binding.splashWebview.settings
        webViewSetting.apply {
            baseWebViewSetting()
        }
        binding.splashWebview.apply {
            isVerticalScrollBarEnabled = true
            isHorizontalScrollBarEnabled = false
            settings.pluginState = WebSettings.PluginState.ON
            settings.pluginState =
                WebSettings.PluginState.ON_DEMAND
            val userAgent = WebView(baseContext).settings.userAgentString
            settings.setUserAgentString("$userAgent APP_Soundgram_Android")
            addJavascriptInterface(WebAppInterface(), "SoundgramSplash")

            setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP -> if (!v.hasFocus()) {
                        v.requestFocus()
                    }
                }
                false
            }
            webViewClient = object : WebViewClient() {
                override fun onReceivedError(
                    view: WebView,
                    errorCode: Int,
                    description: String,
                    failingUrl: String
                ) {
                    binding.splashWebview.loadUrl("about:blank")
                    NetworkDialog(this@SplashActivity).show()
                    super.onReceivedError(view, errorCode, description, failingUrl)
                }

                override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    startActivity(intent)
                    return true
                }
            }

            val str = "tot=" + ChipsterApplication.totId
            postUrl(
                SPLASH_URL, EncodingUtils.getBytes(str, "BASE64")
            )
        }

        if (0 != ApplicationInfo.FLAG_DEBUGGABLE.let {
                applicationInfo.flags = applicationInfo.flags and it; applicationInfo.flags
            }) {
            WebView.setWebContentsDebuggingEnabled(true)
        }


    }

    private fun networkCheck() {
        when (val networkState = getNetworkInfo(application)) {
            NetworkState.NONE_STATE -> {
                ChipsterApplication.startingNetwork = networkState.getName()
                NetworkDialog(this@SplashActivity).show()
            }
            else -> ChipsterApplication.startingNetwork = networkState.getName()
        }
    }

    override fun setRequestedOrientation(requestedOrientation: Int) {
        if (Build.VERSION.SDK_INT != Build.VERSION_CODES.O) {
            super.setRequestedOrientation(requestedOrientation)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            MY_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isEmpty()) {
                    Toast.makeText(this, "Failed get permission", Toast.LENGTH_SHORT).show()
                    ActivityCompat.finishAffinity(this@SplashActivity)
                    overridePendingTransition(0, 0)
                    return
                }
                var i = 0
                while (i < grantResults.size) {
                    if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(
                            this,
                            "Permission is denied : " + permissions[i],
                            Toast.LENGTH_SHORT
                        ).show()
                        return
                    }
                    i++
                }

//                val intent = Intent(this@SplashActivity, NFCActivity::class.java)
//                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
//                startActivity(intent)
//                ActivityCompat.finishAffinity(this@SplashActivity)
//                overridePendingTransition(0, 0)
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    override fun onBackPressed() {
        binding.splashWebview.loadUrl("javascript:showConfirm('close', '" + "chipster" + "', '앱을 종료하시겠습니까?')")
    }

    fun web_close() {
        binding.splashWebview.clearHistory()
        binding.splashWebview.destroy()
        ActivityCompat.finishAffinity(this@SplashActivity)
        System.runFinalization()
        exitProcess(0)
    }


    inner class WebAppInterface {
        @JavascriptInterface
        fun goMain() { // must be final
            binding.splashWebview.destroy()
//                val intent = Intent(this@SplashActivity, NFCActivity::class.java)
//                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
//                startActivity(intent)
            //finish();
            ActivityCompat.finishAffinity(this@SplashActivity)
            overridePendingTransition(0, 0)
        }

        @JavascriptInterface
        fun _close() {
            web_close()
        }

        @JavascriptInterface
        fun wvDestory() {
            Log.i("dlgocks1", "wevDestroy")
        }
    }

    companion object {
        private const val MY_PERMISSION_REQUEST_CODE = 123
    }
}
