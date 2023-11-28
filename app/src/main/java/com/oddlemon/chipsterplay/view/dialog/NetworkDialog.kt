package com.oddlemon.chipsterplay.view.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import com.oddlemon.chipsterplay.databinding.DialogSoundgramBinding
import kotlin.system.exitProcess

class NetworkDialog(
    context: Context,
) : Dialog(context) {

    private lateinit var binding: DialogSoundgramBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DialogSoundgramBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.dialogTv.text =
            "서비스 연결이 원활하지 않습니다.\n\n일시적인 장애이거나\n네트워크 문제일 수 있습니다.\n잠시 후 다시 이용해주세요."
//            _text.text = "네트워크 연결을 확인해주세요.\n확인 후 다시 실행해주시기 바랍니다."
//            _text.text = "서비스 연결이 원활하지 않습니다.\n\n일시적인 장애이거나\n네트워크 문제일 수 있습니다.\n잠시 후 다시 이용해주세요."
        binding.confirmBt.setOnClickListener {
            dismiss()
            exitProcess(0)
        }

    }
}