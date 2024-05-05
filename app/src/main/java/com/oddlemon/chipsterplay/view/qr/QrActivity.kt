package com.oddlemon.chipsterplay.view.qr

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.zxing.ResultPoint
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.CaptureManager
import com.oddlemon.chipsterplay.databinding.ActivityQrBinding
import com.oddlemon.chipsterplay.util.Constants.CAMERA_PERMISSION_REQUEST_CODE
import com.oddlemon.chipsterplay.util.Constants.DEFAULT_USER_ID
import com.oddlemon.chipsterplay.util.Constants.PACK_ID
import com.oddlemon.chipsterplay.util.Constants.USER_ID

class QrActivity : AppCompatActivity() {

    private lateinit var binding: ActivityQrBinding
    private lateinit var capture: CaptureManager

    private var userId: Int = DEFAULT_USER_ID
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQrBinding.inflate(layoutInflater)
        setContentView(binding.root)
        checkPermission()
        initView()
    }

    private fun initView() {
        userId = intent.getIntExtra(USER_ID, DEFAULT_USER_ID)

        binding.closeIv.setOnClickListener {
            finish()
        }
    }

    private fun checkPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_REQUEST_CODE
            )
        } else {
            // 권한이 부여되었으면 QR 코드 스캔 시작
            capture = CaptureManager(this, binding.decoratedBardcordView)
            binding.decoratedBardcordView.decodeContinuous(callback)
        }
    }

    // QR 코드 스캔 결과 처리
    private val callback = object : BarcodeCallback {
        override fun barcodeResult(result: BarcodeResult?) {
            Log.i("dlgocks1", "result : $result")
            Log.i("dlgocks1", "userId : $userId")
            val intent = Intent(this@QrActivity, QrResultActivity::class.java).apply {
                putExtra(PACK_ID, result.toString())
                putExtra(USER_ID, userId)
            }
            startActivity(intent)
            finish()
        }

        override fun possibleResultPoints(resultPoints: MutableList<ResultPoint>?) {}
    }

    override fun onResume() {
        super.onResume()
        capture.onResume()
    }

    override fun onPause() {
        super.onPause()
        capture.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        capture.onDestroy()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        capture.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

}