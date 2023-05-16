package com.soundgram.chipster.view.qr

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.google.zxing.ResultPoint
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.CaptureManager
import com.journeyapps.barcodescanner.ScanOptions
import com.soundgram.chipster.databinding.ActivityQrBinding
import com.soundgram.chipster.util.Constants.CAMERA_PERMISSION_REQUEST_CODE

class QrActivity : AppCompatActivity() {

    private lateinit var binding: ActivityQrBinding
    private lateinit var capture: CaptureManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQrBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.closeIv.setOnClickListener {
            finish()
        }

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
            startActivity(Intent(this@QrActivity, QrResultActivity::class.java).apply {
                putExtra(ScanOptions.QR_CODE, result.toString())
            })
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