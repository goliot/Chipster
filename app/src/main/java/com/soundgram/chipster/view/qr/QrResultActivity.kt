package com.soundgram.chipster.view.qr

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.journeyapps.barcodescanner.ScanOptions
import com.soundgram.chipster.databinding.ActivityQrResultBinding

class QrResultActivity : AppCompatActivity() {
    private lateinit var binding: ActivityQrResultBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQrResultBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.resultTv.text = "스캔결과 : ${intent.getStringExtra(ScanOptions.QR_CODE)}"

        val finishClickListener = View.OnClickListener { finish() }
        binding.startBt.setOnClickListener(finishClickListener)
        binding.closeIv.setOnClickListener(finishClickListener)
    }
}