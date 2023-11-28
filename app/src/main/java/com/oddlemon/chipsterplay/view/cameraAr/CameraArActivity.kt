package com.oddlemon.chipsterplay.view.cameraAr

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.*
import android.hardware.camera2.*
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.util.Size
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.drawToBitmap
import com.oddlemon.chipsterplay.databinding.ActivityCameraArBinding
import java.text.SimpleDateFormat

class CameraArActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCameraArBinding
    private lateinit var cameraId: String
    private lateinit var cameraDevice: CameraDevice
    private lateinit var cameraCaptureSessions: CameraCaptureSession
    private lateinit var captureRequestBuilder: CaptureRequest.Builder
    private lateinit var imageDimension: Size
    private var backgroundHandler: Handler? = null

    private val stateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            cameraDevice = camera
            createCameraPreview()
        }

        override fun onDisconnected(camera: CameraDevice) {
            cameraDevice.close()
        }

        override fun onError(camera: CameraDevice, error: Int) {
            cameraDevice.close()
        }
    }


    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraArBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(
                surface: SurfaceTexture,
                width: Int,
                height: Int
            ) {
                openCamera()
            }

            override fun onSurfaceTextureSizeChanged(
                surface: SurfaceTexture,
                width: Int,
                height: Int
            ) {
            }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                return false
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
        }

        // 카메라 버튼 설정
        binding.cameraButton.setOnClickListener {
            takePicture()
        }

        binding.stickerView.setOnTouchListener(
            object : View.OnTouchListener {
                private var previousX = 0f
                private var previousY = 0f
                override fun onTouch(view: View, event: MotionEvent): Boolean {
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            previousX = event.rawX
                            previousY = event.rawY
                        }

                        MotionEvent.ACTION_MOVE -> {
                            val deltaX = event.rawX - previousX
                            val deltaY = event.rawY - previousY
                            val newX = binding.stickerView.x + deltaX
                            val newY = binding.stickerView.y + deltaY
                            binding.stickerView.animate().x(newX).y(newY).setDuration(0).start()
                            previousX = event.rawX
                            previousY = event.rawY
                        }
                    }
                    return true
                }
            })
    }

    private fun takePicture() {
        val bitmap = Bitmap.createBitmap(
            binding.textureView.width * 2,
            binding.textureView.height * 2,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        canvas.drawBitmap(
            binding.textureView.bitmap!!,
            Matrix(),
            null
        )
        canvas.drawBitmap(
            binding.stickerView.drawToBitmap(),
            binding.stickerView.x * 2,
            binding.stickerView.y * 2,
            null
        )
        getImageUri(this, bitmap)
    }


    private fun openCamera() {
        val manager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            cameraId = manager.cameraIdList[0]
            val characteristics = manager.getCameraCharacteristics(cameraId)
            val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            imageDimension = map!!.getOutputSizes(SurfaceTexture::class.java)[0]
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.CAMERA),
                    101
                )
                return
            }
            manager.openCamera(cameraId, stateCallback, null)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    private fun createCameraPreview() {
        try {
            val texture = binding.textureView.surfaceTexture
            texture?.setDefaultBufferSize(binding.textureView.width, binding.textureView.height)
            val surface = Surface(texture)

            captureRequestBuilder =
                cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            captureRequestBuilder.addTarget(surface)

            cameraDevice.createCaptureSession(
                listOf(surface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        cameraCaptureSessions = session
                        try {
                            captureRequestBuilder.set(
                                CaptureRequest.CONTROL_AF_MODE,
                                CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                            )

                            cameraCaptureSessions.setRepeatingRequest(
                                captureRequestBuilder.build(),
                                null,
                                backgroundHandler
                            )
                        } catch (e: CameraAccessException) {
                            e.printStackTrace()
                        }
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {}
                },
                null
            )
        } catch (e: CameraAccessException) {
            e.printStackTrace()
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
