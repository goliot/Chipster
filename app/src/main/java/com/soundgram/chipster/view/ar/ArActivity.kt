package com.soundgram.chipster.view.ar

import OnSwipeTouchListener
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.*
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.github.penfeizhou.animation.apng.APNGDrawable
import com.github.penfeizhou.animation.loader.ResourceStreamLoader
import com.google.ar.core.*
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.core.exceptions.UnavailableException
import com.google.ar.sceneform.ArSceneView
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.FixedWidthViewSizer
import com.google.ar.sceneform.rendering.ViewRenderable
import com.google.zxing.integration.android.IntentIntegrator.REQUEST_CODE
import com.soundgram.chipster.R
import com.soundgram.chipster.databinding.ActivityArBinding
import com.soundgram.chipster.domain.model.ArPocaDistanceType
import com.soundgram.chipster.domain.model.Poca
import com.soundgram.chipster.util.*
import com.soundgram.chipster.util.Constants.DEFAULT_USER_ID
import com.soundgram.chipster.util.Constants.DEFAULT_PACK_ID
import com.soundgram.chipster.util.Constants.MOVE_BINDER
import com.soundgram.chipster.util.Constants.MOVE_DETAIL
import com.soundgram.chipster.util.Constants.MOVE_MAIN
import com.soundgram.chipster.util.Constants.MOVE_MAP
import com.soundgram.chipster.view.ar.ArpocaViewModel.Companion.POCATEXT_LOADING
import kotlinx.coroutines.*
import uk.co.appoly.arcorelocation.LocationMarker
import uk.co.appoly.arcorelocation.LocationScene
import uk.co.appoly.arcorelocation.utils.ARLocationPermissionHelper
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException


class ArActivity : AppCompatActivity() {

    private var doubleBackToExitPressedOnce = false
    private var packId = 0
    private var userId = 0

    private var _binding: ActivityArBinding? = null
    private val binding get() = _binding!!
    private var locationScene: LocationScene? = null

    private lateinit var arLayout: CompletableFuture<ViewRenderable>
    private lateinit var arSceneView: ArSceneView
    private lateinit var arLayoutRenderable: ViewRenderable
    private lateinit var gpsTracker: GpsTracker
    private lateinit var viewModel: ArpocaViewModel
    lateinit var mediaProjectionManager: MediaProjectionManager
    private lateinit var mediaProjection: MediaProjection
    private val showToastMessage: (String) -> Unit = {
        Toast.makeText(this@ArActivity, it, Toast.LENGTH_SHORT).show()
    }

    @RequiresApi(VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ARLocationPermissionHelper.requestPermission(this)
        init()
        setContentView(binding.root)
        initObserver()
        initListener()
        setArView()
    }

    private fun init() {
        packId = intent.getIntExtra("packId", DEFAULT_PACK_ID) // 테스트용 307
        userId = intent.getIntExtra("userId", DEFAULT_USER_ID) // 1025
        _binding = ActivityArBinding.inflate(layoutInflater)
        viewModel = ViewModelProvider(this)[ArpocaViewModel::class.java]
        binding.arSceneView.planeRenderer.isVisible = false
        binding.scanningTv.text = POCATEXT_LOADING

        viewModel.getPocasWithPackId(packId = packId, onError = showToastMessage)
    }

    /** 뷰모델에서 옵저빙할 값들을 정의한다. */
    private fun initObserver() {
        /** 팩 정보가 확인되면 Ar의 이미지를 변경한다. */
        viewModel.packInfo.observe(this) {
            arLayout.thenAccept { viewRenderable ->
                Glide.with(this)
//                    .load(IMAGE_PATH.format(packId, viewModel.packInfo.value?.targetImg))
                    .load("https://chipsterplay.soundgram.co.kr/media/arpoca/307/pack_img/target_img_1.png")
                    .error(R.drawable.bottom_04)
                    .into(viewRenderable.view.findViewById(R.id.ar_target_iv))
                viewRenderable.sizer = FixedWidthViewSizer(0.2f)
            }
        }

    }

    /** ARview를 생성한다. */
    private fun setArView() {
        arSceneView = binding.arSceneView
        arLayout = ViewRenderable.builder()
            .setView(this, R.layout.ar_target_layout)
            .build()
        completeArLayout()

        arSceneView.scene.addOnUpdateListener {
            if (!viewModel.hasFinishedLoading) {
                return@addOnUpdateListener
            }
            if (locationScene == null) {
                locationScene = LocationScene(this, this, arSceneView)
                locationScene?.distanceLimit = 1
                locationScene?.anchorRefreshInterval = Int.MAX_VALUE
                observePoca()
            }
            val frame = arSceneView.arFrame ?: return@addOnUpdateListener
            if (frame.camera.trackingState != TrackingState.TRACKING) {
                return@addOnUpdateListener
            }
            locationScene?.processFrame(frame)
        }
    }


    private fun observePoca() {
        viewModel.pocas.observe(this) { pocas ->
            val closedPoca = pocas.minByOrNull { poca ->
                distanceOf(
                    poca.latitude,
                    poca.longitude,
                    gpsTracker.userlatitude,
                    gpsTracker.userlongitude,
                    "meter"
                )
            }

            ArPocaDistanceType.findByDistance(
                distanceOf(
                    closedPoca?.latitude ?: 0.0,
                    closedPoca?.longitude ?: 0.0,
                    gpsTracker.userlatitude,
                    gpsTracker.userlongitude,
                    "meter"
                )
            ).let {
                binding.scanningTv.text = it.text
                val resourceLoader = ResourceStreamLoader(this@ArActivity, it.imgage)
                val apngDrawable = APNGDrawable(resourceLoader)
                binding.getPocaIv.setImageDrawable(apngDrawable)
            }


            closedPoca?.let {
                val locationMarker = LocationMarker(
                    it.longitude,
                    it.latitude,
                    getArView(it)
                ).apply {
                    setScaleAtDistance(false)
                    this.node.apply {
                        val lookRotation =
                            Quaternion.lookRotation(Vector3.zero(), Vector3.zero())
                        worldRotation = lookRotation
                        worldPosition = Vector3.one()
                        worldScale = Vector3.one()
                        setRenderEvent {
                            worldScale = Vector3.one()
                        }
                    }
                }
                locationScene?.mLocationMarkers?.add(locationMarker)
            }
        }
    }

    private fun completeArLayout() {
        CompletableFuture
            .allOf(arLayout)
            .handle<Any?> { _: Void?, throwable: Throwable? ->
                if (throwable != null) {
                    DemoUtils.displayError(this, "Unable to load renderables", throwable)
                    return@handle null
                }
                try {
                    arLayoutRenderable = arLayout.get()
                    viewModel.hasFinishedLoading = true
                } catch (ex: InterruptedException) {
                    DemoUtils.displayError(this, "Unable to load renderables", ex)
                } catch (ex: ExecutionException) {
                    DemoUtils.displayError(this, "Unable to load renderables", ex)
                }
                null
            }
    }

    /** ArActivity에 필요한 리스너를 정의 */
    @RequiresApi(VERSION_CODES.O)
    @SuppressLint("ClickableViewAccessibility")
    private fun initListener() {
        binding.apply {
            /** 카드모션 획득 끝났을 때 돌리는 이미지 */
            getMotionEndedIv.setOnTouchListener(object :
                OnSwipeTouchListener(context = this@ArActivity) {
                override fun onSwiperVertical() {
                    viewModel.onCardSwipeVertical(
                        context = this@ArActivity,
                        onDrawableReady = { animateDrawable ->
                            rotationIv.setImageDrawable(animateDrawable)
                            animateDrawable?.start()
                            binding.apply {
                                getMotionEndedIv.hide()
                                rotationIv.show()
                                Handler(Looper.getMainLooper()).postDelayed({
                                    onFinish(MOVE_DETAIL)
                                }, 2000)
                            }
                        }
                    )
                }
            })

            infoIv.setOnClickListener {
                AlertDialog.Builder(this@ArActivity)
                    .setView(R.layout.dialog_ar_info)
                    .show()
                    .also { alertDialog ->
                        if (alertDialog == null) {
                            return@also
                        }
                    }
            }

            exitIv.setOnClickListener {
                onFinish(MOVE_MAIN)
            }
            icMapIv.setOnClickListener {
                onFinish(MOVE_MAP)
            }
            icBinderIv.setOnClickListener {
                onFinish(MOVE_BINDER)
            }

        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data!!)
        }
    }

    private fun getArView(item: Poca): Node {
        val base = Node()
        base.renderable = arLayoutRenderable

        base.setOnTouchListener { _, _ ->
            with(viewModel) {
                if (isLoading.value == true) return@setOnTouchListener false
                setLoadingTrue()
                setUserDataWithPack(
                    userId = userId,
                    packId = packId,
                    pocaId = item.id,
                    onError = {
                        showToastMessage(it)
                        setLoadingFalse()
                    },
                    onSuccess = {
                        onPocaClick(
                            context = this@ArActivity,
                            onDrawableReady = { animatedDrawable ->
                                binding.getMotionIv.setImageDrawable(animatedDrawable)
                                animatedDrawable?.start()
                                onTouchMarker()
                                Handler(Looper.getMainLooper()).postDelayed({
                                    binding.getMotionEndedIv.setImageWithUrl(
                                        this@ArActivity,
//                                        url = IMAGE_PATH.format(packId, packInfo.value?.cardImg)
                                        url = "https://chipsterplay.soundgram.co.kr/media/arpoca/307/pack_img/pack_img_1.png"
                                    )
                                    binding.getMotionEndedIv.show()
                                    binding.getMotionTv.text = "카드를 위아래로 휘리릭 돌려 보라구~!"
                                    binding.getMotionIv.setImageResource(0)
                                }, 3200)
                            },
                        )
                    })
            }
            false
        }
        return base
    }

    private fun onTouchMarker() {
        binding.apply {
            scanningTv.hide()
            scanningIv.hide()
            getMotionIv.show()
            getMotionTv.show()
            getMotionTv.text = "잡았다!! 요놈!!"

            val resourceLoader = ResourceStreamLoader(this@ArActivity, R.drawable.img_distance_05)
            val apngDrawable = APNGDrawable(resourceLoader)
            binding.getPocaIv.setImageDrawable(apngDrawable)
        }
    }

    private fun onFinish(requestCode: Int) {
        setResult(requestCode, intent.apply {
            this.putExtra("packId", packId)
        })
        finish()
    }

    override fun onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            setResult(RESULT_OK)
            super.onBackPressed()
            return
        }
        doubleBackToExitPressedOnce = true
        showToastMessage("한번 더 누를시 AR이 종료됩니다.")
        Handler(Looper.getMainLooper()).postDelayed(
            { doubleBackToExitPressedOnce = false },
            2000
        )
    }

    override fun onResume() {
        super.onResume()
        if (arSceneView.session == null) {
            // 세션이 아직 만들어지지 않은 경우 렌더링을 다시 시작하지 마십시오.
            // ARCore를 업데이트해야 하거나 사용 권한이 아직 부여되지 않은 경우 이 문제가 발생할 수 있습니다
            try {
                val session: Session? =
                    DemoUtils.createArSession(this@ArActivity, viewModel.installRequested)
                if (session == null) {
                    viewModel.installRequested = ARLocationPermissionHelper.hasPermission(this)
                    return
                } else {
                    arSceneView.setupSession(session)
                }
            } catch (e: UnavailableException) {
                DemoUtils.handleSessionException(this, e)
            }
        }
        try {
            arSceneView.resume()
            locationScene?.resume()
        } catch (ex: CameraNotAvailableException) {
            DemoUtils.displayError(this, "Unable to get camera", ex)
            onFinish(MOVE_MAIN)
            return
        }
        val cameraConfigFilter = CameraConfigFilter(arSceneView.session)
        cameraConfigFilter.facingDirection = CameraConfig.FacingDirection.FRONT
        val cameraConfigs =
            arSceneView.session?.getSupportedCameraConfigs(cameraConfigFilter)
        if (cameraConfigs?.isNotEmpty() == true) {
            arSceneView.session?.cameraConfig = cameraConfigs[0]!!
        }
        gpsTracker = GpsTracker(this)
        viewModel.userLat = gpsTracker.userlatitude
        viewModel.userLong = gpsTracker.userlongitude
    }

    public override fun onPause() {
        super.onPause()
        locationScene?.pause()
        arSceneView.pause()
    }

    public override fun onDestroy() {
        super.onDestroy()
        showSystemUI(window, binding.root)
        arSceneView.destroy()
        _binding = null
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, results: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, results)
        if (!ARLocationPermissionHelper.hasPermission(this)) {
            if (!ARLocationPermissionHelper.shouldShowRequestPermissionRationale(this)) {
                ARLocationPermissionHelper.launchPermissionSettings(this)
            } else {
                showToastMessage("카메라 및 위치 사용권한을 승인해주세요.")
            }
            onFinish(MOVE_MAIN)
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemUI(window, binding.root)
        }
    }

    companion object {
        private const val DISTANCE_METER_FROM_USER = 100
    }
}
