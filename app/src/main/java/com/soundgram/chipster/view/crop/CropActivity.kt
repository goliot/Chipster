package com.soundgram.chipster.view.crop

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Rect
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.view.PixelCopy
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import com.soundgram.chipster.databinding.ActivityCropImageBinding
import com.soundgram.chipster.util.Constants.IMAGE_URI
import java.text.SimpleDateFormat
import java.util.*

class CropActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCropImageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCropImageBinding.inflate(layoutInflater)
        initView(intent.getStringExtra(IMAGE_URI))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            initListener()
        }
        setContentView(binding.root)
    }

    private fun initView(stringExtra: String?) {
        if (stringExtra != null && stringExtra != "null") {
            binding.cropImageView.setImageURI(stringExtra.toUri())
            binding.cropImageView.setAspectRatio(1, 1)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun initListener() {
        binding.confirmBt.setOnClickListener {
            val bitmap = Bitmap.createBitmap(
                binding.cropContainer.width,
                binding.cropContainer.height,
                Bitmap.Config.ARGB_8888
            )
            val location = IntArray(2)
            binding.cropContainer.getLocationInWindow(location)
            PixelCopy.request(
                window,
                Rect(
                    location[0],
                    location[1],
                    location[0] + binding.cropContainer.width,
                    location[1] + binding.cropContainer.height
                ),
                bitmap,
                { result ->
                    if (result == PixelCopy.SUCCESS) {
                        val result = Intent().apply {
                            data = getImageUri(this@CropActivity, bitmap)
                        }
                        setResult(RESULT_OK, result)
                        finish()
                    }
                },
                Handler(Looper.getMainLooper())
            )
        }
    }

    private fun getImageUri(context: Context, bitmap: Bitmap): Uri {
        val fileName = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(System.currentTimeMillis())
        return Uri.parse(
            MediaStore.Images.Media.insertImage(
                context.contentResolver,
                bitmap,
                fileName,
                null
            )
        )
    }


}