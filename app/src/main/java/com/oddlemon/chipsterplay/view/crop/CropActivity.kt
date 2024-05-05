package com.oddlemon.chipsterplay.view.crop

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Rect
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.view.PixelCopy
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.oddlemon.chipsterplay.databinding.ActivityCropImageBinding
import com.oddlemon.chipsterplay.util.Constants.CROP_REQUEST_CODE
import com.oddlemon.chipsterplay.util.Constants.IMAGE_URI
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class CropActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCropImageBinding
    private lateinit var cameraImageUri: Uri
    private var resultIntent = Intent()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCropImageBinding.inflate(layoutInflater)
        setImageToView()
        initView(intent.getStringExtra(IMAGE_URI))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            initListener()
        }
        setContentView(binding.root)
    }

    private fun setImageToView() {
        val intentCamera = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val path = filesDir
        val file = File(path, "Soundgram_$timeStamp.jpg")

        // File 객체의 URI 를 얻는다.
        cameraImageUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            FileProvider.getUriForFile(this, applicationContext.packageName + ".fileprovider", file)
        } else {
            Uri.fromFile(file)
        }

        intentCamera.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri)
        val pickIntent = Intent(Intent.ACTION_PICK)
        pickIntent.type = MediaStore.Images.Media.CONTENT_TYPE
        pickIntent.data = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val pickTitle = "사진 가져올 방법을 선택하세요."
        val chooserIntent = Intent.createChooser(pickIntent, pickTitle)
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf<Parcelable>(intentCamera))
        startActivityForResult(chooserIntent, CROP_REQUEST_CODE)
    }

    private fun initView(stringExtra: String?) {
        if (stringExtra != null && stringExtra != "null") {
            binding.cropImageView.setImageURI(stringExtra.toUri())
            binding.cropImageView.setAspectRatio(1, 1)
        }
    }

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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            if (data == null) {
                resultIntent.data = cameraImageUri
            } else {
                resultIntent = data
            }
            binding.cropImageView.setImageURI(resultIntent.data)
            binding.cropImageView.setAspectRatio(4, 5)
        } else {
            finish()
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