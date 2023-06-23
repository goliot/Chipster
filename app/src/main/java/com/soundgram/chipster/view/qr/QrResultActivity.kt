package com.soundgram.chipster.view.qr

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.soundgram.chipster.R
import com.soundgram.chipster.databinding.ActivityQrResultBinding
import com.soundgram.chipster.util.Constants.DEFAULT_USER_ID
import com.soundgram.chipster.util.Constants.DEFAULT_PACK_ID
import com.soundgram.chipster.util.Constants.PACK_ID
import com.soundgram.chipster.util.Constants.USER_ID
import com.soundgram.chipster.util.hide
import com.soundgram.chipster.util.show

class QrResultActivity : AppCompatActivity() {
    private lateinit var binding: ActivityQrResultBinding

    private lateinit var viewModel: QrViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQrResultBinding.inflate(layoutInflater)
        viewModel = ViewModelProvider(this)[QrViewModel::class.java]
        setContentView(binding.root)

        val packId = intent.getStringExtra(PACK_ID)?.toIntOrNull() ?: DEFAULT_PACK_ID
        val userId = intent.getStringExtra(USER_ID)?.toIntOrNull() ?: DEFAULT_USER_ID
        initView()

        viewModel.postUserPack(
            packId = packId,
            userId = userId,
            onSuccess = {
                Toast.makeText(this, "포카 다운로드 완료!", Toast.LENGTH_SHORT).show()
                binding.resultIv.setImageResource(R.mipmap.img_qr_result_ok)
                binding.titleTv.text = "인증이 완료되었습니다."
                binding.subtitleTv.text = "이제 마음껏 즐겨보세요!"
                binding.startBt.show()
                binding.backIv.hide()
            },
            onError = { errorMessage ->
                Toast.makeText(this, "포카 다운로드 중 에러가 발생했습니다.", Toast.LENGTH_SHORT).show()
                binding.resultIv.setImageResource(R.mipmap.img_qr_result_fail)
                binding.titleTv.text = "인증에 실패했습니다."
                binding.subtitleTv.text = errorMessage
                binding.startBt.hide()
                binding.backIv.show()
            }
        )

    }

    private fun initView() {
        binding.titleTv.text = "인증 중입니다."
        binding.subtitleTv.text = ""
        val finishClickListener = View.OnClickListener { finish() }
        binding.startBt.setOnClickListener(finishClickListener)
        binding.backIv.setOnClickListener(finishClickListener)
        binding.closeIv.setOnClickListener(finishClickListener)
    }
}