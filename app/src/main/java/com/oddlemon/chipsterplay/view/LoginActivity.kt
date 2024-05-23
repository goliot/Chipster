package com.oddlemon.chipsterplay.view

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Message
import android.provider.MediaStore
import android.util.Log
import android.view.*
import android.webkit.*
import android.webkit.WebView.WebViewTransport
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import com.google.firebase.iid.FirebaseInstanceId
import com.oddlemon.chipsterplay.ChipsterApplication
import com.oddlemon.chipsterplay.ChipsterApplication.Companion.ALBUM_TITLE
import com.oddlemon.chipsterplay.ChipsterApplication.Companion.APP_VERSION
import com.oddlemon.chipsterplay.ChipsterApplication.Companion.pushMovingPage
import com.oddlemon.chipsterplay.ChipsterApplication.Companion.token
import com.oddlemon.chipsterplay.ChipsterApplication.Companion.totId
import com.oddlemon.chipsterplay.databinding.ActivityLoginBinding
import com.oddlemon.chipsterplay.util.Constants.CAMERA_REQUEST_CODE
import com.oddlemon.chipsterplay.util.Constants.CROP_REQUEST_CODE
import com.oddlemon.chipsterplay.util.Constants.DEFAULT_PACK_ID
import com.oddlemon.chipsterplay.util.Constants.DEFAULT_POCA_ID
import com.oddlemon.chipsterplay.util.Constants.ERROR
import com.oddlemon.chipsterplay.util.Constants.GALLERY_REQUEST_CODE
import com.oddlemon.chipsterplay.util.Constants.IMAGE_URI
import com.oddlemon.chipsterplay.util.Constants.MOVE_BINDER
import com.oddlemon.chipsterplay.util.Constants.MOVE_DETAIL
import com.oddlemon.chipsterplay.util.Constants.MOVE_MAIN
import com.oddlemon.chipsterplay.util.Constants.MOVE_MAP
import com.oddlemon.chipsterplay.util.Constants.PERMISSION_REQUEST_CODE
import com.oddlemon.chipsterplay.util.Constants.USER_ID
import com.oddlemon.chipsterplay.util.baseWebViewSetting
import com.oddlemon.chipsterplay.util.permissionCheck
import com.oddlemon.chipsterplay.util.userid.Installation
import com.oddlemon.chipsterplay.util.userid.UniqueDeviceID
import com.oddlemon.chipsterplay.view.ar.ArActivity
import com.oddlemon.chipsterplay.view.crop.CropActivity
import com.oddlemon.chipsterplay.view.dialog.NetworkDialog
import com.oddlemon.chipsterplay.view.qr.QrActivity
import org.apache.http.util.EncodingUtils
import java.io.File
import java.net.URISyntaxException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.system.exitProcess


class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var prefs: SharedPreferences
    private lateinit var LOGIN_URL: String
    private lateinit var webView: WebView
    private var filePathCallbackLollipop: ValueCallback<Array<Uri>>? = null
    private var imageUri: Uri? = null
    private var cameraImageUri: Uri? = null
    private lateinit var startActivityResult: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i("dlgocks1", "[Activity] - LoginActvitiy")
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        prefs = getSharedPreferences("Pref", MODE_PRIVATE)

        LOGIN_URL = ChipsterApplication.MAIN_URL + "/login.php"
//        LOGIN_URL = ChipsterApplication.MAIN_URL + "/home_rhw.php"
        startActivityResult = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result: ActivityResult ->
            if (result.resultCode == MOVE_MAIN) {
                webView.loadUrl("javascript:_Main();")
            }
            if (result.resultCode == MOVE_BINDER) {
                result.data?.let {
                    val packId = it.getIntExtra("packId", DEFAULT_PACK_ID)
                    webView.loadUrl("javascript:gotopackbinder(${packId});")
                }
            }
            if (result.resultCode == MOVE_MAP) {
                result.data?.let {
                    val packId = it.getIntExtra("packId", DEFAULT_PACK_ID)
                    webView.loadUrl("javascript:moveMap($packId);")
                }
            }
            if (result.resultCode == MOVE_DETAIL) {
                result.data?.let {
                    val packId = it.getIntExtra("packId", DEFAULT_PACK_ID)
                    val pocaID = it.getIntExtra("pocaId", DEFAULT_PACK_ID)
                    webView.loadUrl("javascript:gotopackbinder(304,231);")
                }
            }
        }
        permissionCheck(
            context = this@LoginActivity,
            activity = this@LoginActivity,
            onSuccess = {
                initWebView()
            },
            onFail = {
                initWebView()
//                Toast.makeText(
//                    this@LoginActivity,
//                    "권한을 허용하지 않으면 일부 기능이 작동하지 않을 수 있습니다.",
//                    Toast.LENGTH_SHORT
//                ).show()
            }
        )
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initWebView() {
        webView = binding.loginWebview


        // 웹뷰 세팅 적용
        binding.loginWebview.settings.apply {
            baseWebViewSetting()
            pluginState = WebSettings.PluginState.ON
            pluginState = WebSettings.PluginState.ON_DEMAND
            val userAgent = WebView(baseContext).settings.userAgentString
            userAgentString = "$userAgent APP_Soundgram_Android"
        }

        // 웹뷰 클라이언트 및 속성 적용
        binding.loginWebview.apply {
            isVerticalScrollBarEnabled = true
            isHorizontalScrollBarEnabled = false
            webViewClient = object : WebViewClient() {
                override fun onReceivedError(
                    view: WebView,
                    errorCode: Int,
                    description: String,
                    failingUrl: String
                ) {
                    Log.e(ERROR + "onReceivedError", "description : $description")
                    loadUrl("about:blank") // 빈페이지 출력
                    NetworkDialog(this@LoginActivity).show()
                    super.onReceivedError(view, errorCode, description, failingUrl)
                }

                override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                    return if (url.startsWith("http://") || url.startsWith("https://")) {
                        // HTTP 또는 HTTPS 프로토콜인 경우 웹뷰에서 처리
                        view.loadUrl(url)
                        false
                    } else {
                        // HTTP 또는 HTTPS 프로토콜이 아닌 경우 외부 브라우저에서 처리
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        startActivity(intent)
                        true
                    }
                }

                override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                }

                override fun onPageFinished(view: WebView, url: String) {
                    /* NOTICE 로그인을 한 다음 앱을 종료하고, 다시 앱을 실행했을 때 간헐적으로 로그인이 안 된 상태가 된다.
                         이는 웹뷰의 RAM과 영구 저장소 사이에 쿠키가 동기화가 안 되어 있기 때문이다. 따라서 강제로 동기화를 해준다. */
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                        CookieSyncManager.getInstance().sync()
                    } else {
                        // 롤리팝 이상에서는 CookieManager의 flush를 하도록 변경됨.
                        CookieManager.getInstance().flush()
                    }
                }
            }

            webChromeClient = object : WebChromeClient() {

                override fun onGeolocationPermissionsShowPrompt(
                    origin: String?,
                    callback: GeolocationPermissions.Callback?
                ) {
                    super.onGeolocationPermissionsShowPrompt(origin, callback)
                    callback?.invoke(origin, true, false)
                }

                override fun onCreateWindow(
                    view: WebView,
                    isDialog: Boolean,
                    isUserGesture: Boolean,
                    resultMsg: Message
                ): Boolean {
                    val newWebView = WebView(this@LoginActivity)
                    newWebView.settings.apply {
                        baseWebViewSetting()
                    }
                    val custinfoDialog = Dialog(
                        this@LoginActivity,
                        android.R.style.Theme_Black_NoTitleBar_Fullscreen
                    ).apply {
                        setContentView(newWebView)
                        setOnKeyListener(DialogInterface.OnKeyListener { _, i, _ ->
                            if (i == KeyEvent.KEYCODE_BACK) {
                                newWebView.loadUrl("javascript:window.self.close()")
                                return@OnKeyListener true
                            }
                            false
                        })
                    }
                    custinfoDialog.show()
                    newWebView.webChromeClient = object : WebChromeClient() {
                        override fun onCloseWindow(window: WebView) {
                            custinfoDialog.dismiss()
                            binding.loginWebview.removeView(window)
                        }
                    }

                    // WebView Popup에서 내용이 안보이고 빈 화면만 보여 아래 코드 추가
                    newWebView.webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(
                            view: WebView,
                            request: WebResourceRequest
                        ): Boolean {
                            return if (request.url.toString().startsWith("intent:")) {
                                try {
                                    val intent = Intent.parseUri(
                                        request.url.toString(),
                                        Intent.URI_INTENT_SCHEME
                                    )
                                    val existPackage = intent.getPackage()?.let {
                                        packageManager.getLaunchIntentForPackage(it)
                                    }
                                    existPackage?.let {
                                        startActivity(intent)
                                    }
                                } catch (e: URISyntaxException) {
                                    e.printStackTrace()
                                }
                                true
                            } else {
                                false
                            }
                        }
                    }
                    (resultMsg.obj as WebViewTransport).webView = newWebView
                    resultMsg.sendToTarget()
                    return true
                }

                override fun onCloseWindow(window: WebView) {
                    super.onCloseWindow(window)
                }

                override fun onShowFileChooser(
                    webView: WebView?,
                    filePathCallback: ValueCallback<Array<Uri>>?,
                    fileChooserParams: FileChooserParams?
                ): Boolean {
                    Log.d("dlgocks1", "onShowFileChooser")
                    // Callback 초기화 (중요!)
                    if (filePathCallbackLollipop != null) {
                        filePathCallbackLollipop!!.onReceiveValue(null)
                        filePathCallbackLollipop = null
                    }
                    filePathCallbackLollipop = filePathCallback

                    val intentCamera = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
                    val path = filesDir
                    val file = File(path, "Soundgram_$timeStamp.jpg")
                    // File 객체의 URI 를 얻는다.
                    cameraImageUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        FileProvider.getUriForFile(
                            this@LoginActivity,
                            applicationContext.packageName + ".fileprovider",
                            file
                        )
                    } else {
                        Uri.fromFile(file)
                    }

                    intentCamera.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri)

                    val intent = Intent(this@LoginActivity, CropActivity::class.java)
                    startActivityForResult(
                        intent,
                        CROP_REQUEST_CODE
                    )
//                    val intent =
//                        Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
//                    startActivityForResult(intent, GALLERY_REQUEST_CODE)

                    return true
                }

            }
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
            setCookieAllow(this)
            addJavascriptInterface(LoginWebViewInterface(), "SoundgramLogin")
        }

        val isGuide: Boolean = prefs.getBoolean("isTotGuide", true)
        val uuid: String = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            Installation.id(this@LoginActivity)
        } else {
            UniqueDeviceID.getUniqueID(this@LoginActivity)
        }
        val id = FirebaseInstanceId.getInstance().instanceId
        id.addOnCompleteListener { task -> // 토큰 확인
            if (task.isSuccessful) {
                token = task.result?.token
            } else {
                Log.e(
                    ERROR + "soundgram-firebase",
                    "Token Exception : " + task.exception.toString()
                )
            }
            Log.i("dlgocks - userToken", token.toString())
            val postContents =
                "&app_ver=$APP_VERSION&package=$packageName&ostype=1&uuid=$uuid&isGuide=$isGuide&token=$token&pushMovingPage=$pushMovingPage&tot_id=$totId"
            binding.loginWebview.postUrl(
                LOGIN_URL,
                EncodingUtils.getBytes(postContents, "BASE64")
            )
        }
    }

    override fun onBackPressed() {
        if (::webView.isInitialized && webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    private fun setCookieAllow(webView: WebView) {
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        webView.settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        cookieManager.setAcceptThirdPartyCookies(webView, true)
    }

    private fun webClose() {
        binding.loginWebview.clearHistory()
        ActivityCompat.finishAffinity(this@LoginActivity)
        System.runFinalization()
        exitProcess(0)
    }

    private fun appStart() {
        // UI쓰레드에서 동작해야 함
        runOnUiThread {
            binding.loginWebview.clearHistory()
            binding.loginWebview.destroy()
            val intent = Intent(this, SplashActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
            startActivity(intent)
            ActivityCompat.finishAffinity(this@LoginActivity)
            overridePendingTransition(0, 0)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    initWebView()
                }
                return
            }
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            CookieSyncManager.getInstance().startSync()
        } else {
            CookieManager.getInstance().flush()
        }
    }

    inner class LoginWebViewInterface {

        @JavascriptInterface
        fun setCookie() {
            Log.i("dlgocks1 - JavascriptInterface", "_setCookie()")
            CookieManager.getInstance().flush()
        }

        @JavascriptInterface
        fun reload() {
            Log.i("dlgocks1 - JavascriptInterface", "_reload()")
            try {
                //액티비티 화면 재갱신 시키는 코드
                finish() //현재 액티비티 종료 실시
                overridePendingTransition(0, 0) //인텐트 애니메이션 없애기
                startActivity(intent) //현재 액티비티 재실행 실시
                overridePendingTransition(0, 0) //인텐트 애니메이션 없애기
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        @JavascriptInterface
        fun mailto() {
            Log.i("dlgocks1 - JavascriptInterface", "_mailto()")
            val emailIntent = Intent(Intent.ACTION_SEND)
            try {
                val user_os_version = Build.VERSION.RELEASE
                emailIntent.type = "plain/text"
                emailIntent.putExtra(
                    Intent.EXTRA_EMAIL,
                    arrayOf("soundgram.info@soundgram.co.kr")
                )
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, "[Soundgram 문의]")
                emailIntent.putExtra(
                    Intent.EXTRA_TEXT, """
                            어플리케이션 : $ALBUM_TITLE
                            기기 종류 : ${Build.MODEL}
                            OS 버전 : $user_os_version
                            앱 버전 : $APP_VERSION
                            """.trimIndent()
                )
                emailIntent.type = "message/rfc822"
                emailIntent.setPackage("com.google.android.gm")
                if (emailIntent.resolveActivity(packageManager) != null) startActivity(
                    emailIntent
                )
                startActivity(emailIntent)
            } catch (e: Exception) {
                e.printStackTrace()
                emailIntent.type = "text/html"
                emailIntent.putExtra(Intent.EXTRA_EMAIL, arrayOf("email@gmail.com"))
                startActivity(Intent.createChooser(emailIntent, "Send Email"))
            }
        }

        @JavascriptInterface
        fun _NextStep() { // must be final
            Log.i("dlgocks1 - JavascriptInterface", "_NextStep()")
            appStart()
        }

        @JavascriptInterface
        fun _close() {
            Log.i("dlgocks1 - JavascriptInterface", "_close()")
            webClose()
        }

        @JavascriptInterface
        fun _GetImageFromGallery() {
            Log.i("dlgocks1 - JavascriptInterface", "GetImageFromMobile")
            val intent =
                Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, GALLERY_REQUEST_CODE)
        }

        @JavascriptInterface
        fun _GetImageFromCamera() {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            imageUri = getCameraImageURI()
            intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
            startActivityForResult(intent, CAMERA_REQUEST_CODE)
        }

        @JavascriptInterface
        fun _MoveQR() {
            startActivity(Intent(this@LoginActivity, QrActivity::class.java))
        }

        @JavascriptInterface
        fun _MoveQR(userId: String) {
            val intent = Intent(this@LoginActivity, QrActivity::class.java)
            intent.putExtra(USER_ID, userId)
            Log.i("dlgocks1", userId)
            startActivity(intent)
        }

        @JavascriptInterface
        fun _MoveAR(userId: String, packId: String) {
            Log.i("MoveAR", "Function _MoveAR called with userId: $userId and packId: $packId")

            try {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                    Toast.makeText(this@LoginActivity, "AR 모듈을 실행할 수 없습니다.", Toast.LENGTH_SHORT).show()
                    return
                }

                val userIdInt = if (userId == "undefined") 1208 else userId.toInt()
                val packIdInt = if (packId == "undefined") 432 else packId.toInt()

                Log.i("MoveAR", "Parsed userId: $userIdInt, packId: $packIdInt")

                val intent = Intent(this@LoginActivity, ArActivity::class.java)
                intent.putExtra("packId", packIdInt)
                intent.putExtra("userId", userIdInt)

                Log.i("MoveAR", "Starting AR Activity with packId: $packIdInt, userId: $userIdInt")

                startActivityResult.launch(intent)
            } catch (e: NumberFormatException) {
                Log.e("MoveAR", "Error parsing userId or packId: ${e.message}")
                Toast.makeText(this@LoginActivity, "잘못된 ID 형식입니다.", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e("MoveAR", "Unexpected error: ${e.message}")
                Toast.makeText(this@LoginActivity, "예기치 않은 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
            }
        }

        fun getCameraImageURI(): Uri? {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
            val path = filesDir
            val file = File(path, "chipster_$timeStamp.jpg")
            return FileProvider.getUriForFile(
                this@LoginActivity,
                applicationContext.packageName + ".fileprovider",
                file
            )
        }


    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == GALLERY_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            val selectedImageUri = data.data
            startActivityForResult(
                Intent(this@LoginActivity, CropActivity::class.java).apply {
                    putExtra(IMAGE_URI, selectedImageUri.toString())
                },
                CROP_REQUEST_CODE
            )
        }
        if (requestCode == CAMERA_REQUEST_CODE && resultCode == RESULT_OK) {
            startActivityForResult(
                Intent(this@LoginActivity, CropActivity::class.java).apply {
                    putExtra(IMAGE_URI, imageUri.toString())
                },
                CROP_REQUEST_CODE
            )
        }
        if (requestCode == CROP_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                Log.i("dlgocks1", data.toString())
                if (filePathCallbackLollipop == null) return
                if (data == null) return
                if (data.data == null) return
                filePathCallbackLollipop!!.onReceiveValue(
                    WebChromeClient.FileChooserParams.parseResult(
                        resultCode,
                        data
                    )
                )
                filePathCallbackLollipop = null
            } else {
                if (filePathCallbackLollipop != null) {   //  resultCode에 RESULT_OK가 들어오지 않으면 null 처리하지 한다.(이렇게 하지 않으면 다음부터 input 태그를 클릭해도 반응하지 않음)
                    filePathCallbackLollipop!!.onReceiveValue(null)
                    filePathCallbackLollipop = null
                }
            }
        }
        if (resultCode == MOVE_MAIN) {
            data?.let {
                val packId = data.getIntExtra("packId", DEFAULT_PACK_ID)
                webView.loadUrl("javascript:_Main(${packId});")
            }
        }
        if (resultCode == MOVE_BINDER) {
            data?.let {
                val packId = data.getIntExtra("packId", DEFAULT_PACK_ID)
                webView.loadUrl("javascript:gotopackbinder(${packId});")
            }
        }
        if (resultCode == MOVE_MAP) {
            data?.let {
                val packId = data.getIntExtra("packId", DEFAULT_PACK_ID)
                webView.loadUrl("javascript:moveMap(${packId});")
            }
        }
        if (resultCode == MOVE_DETAIL) {
            data?.let {
                val packId = data.getIntExtra("packId", DEFAULT_PACK_ID)
                val pocaId = data.getIntExtra("pocaId", DEFAULT_POCA_ID)
                webView.loadUrl("javascript:javascript:poca_page(${pocaId});")
            }
        }
    }

}