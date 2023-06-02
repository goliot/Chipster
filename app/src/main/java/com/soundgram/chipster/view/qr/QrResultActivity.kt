package com.soundgram.chipster.view.qr

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.journeyapps.barcodescanner.ScanOptions
import com.soundgram.chipster.databinding.ActivityQrResultBinding

class QrResultActivity : AppCompatActivity() {
    private lateinit var binding: ActivityQrResultBinding

    private lateinit var viewModel: QrViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQrResultBinding.inflate(layoutInflater)
        viewModel = ViewModelProvider(this)[QrViewModel::class.java]
        setContentView(binding.root)

        val pocaId = intent.getStringExtra(ScanOptions.QR_CODE)?.toIntOrNull() ?: 307
        val userId = intent.getStringExtra(ScanOptions.QR_CODE)?.toIntOrNull() ?: 1114
        binding.resultTv.text = "스캔결과 : $pocaId"
        viewModel.getPocaInfo(pocaId, userId) {
            Toast.makeText(this, "포카 다운로드 완료!", Toast.LENGTH_SHORT).show()
        }
        val finishClickListener = View.OnClickListener { finish() }
        binding.startBt.setOnClickListener(finishClickListener)
        binding.closeIv.setOnClickListener(finishClickListener)
    }
}