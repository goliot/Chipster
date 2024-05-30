package com.oddlemon.chipsterplay.view.ar

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
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.codemonkeylabs.fpslibrary.TinyDancer
import com.github.penfeizhou.animation.apng.APNGDrawable
import com.github.penfeizhou.animation.loader.ResourceStreamLoader
import com.google.ar.core.*
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.core.exceptions.UnavailableException
import com.google.ar.sceneform.ArSceneView
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.FixedWidthViewSizer
import com.google.ar.sceneform.rendering.ViewRenderable
import com.google.zxing.integration.android.IntentIntegrator.REQUEST_CODE
import com.oddlemon.chipsterplay.databinding.ActivityArBinding
import com.oddlemon.chipsterplay.domain.model.ArPocaDistanceType
import com.oddlemon.chipsterplay.domain.model.Poca
import com.oddlemon.chipsterplay.util.*
import com.oddlemon.chipsterplay.util.Constants.DEFAULT_USER_ID
import com.oddlemon.chipsterplay.util.Constants.DEFAULT_PACK_ID
import com.oddlemon.chipsterplay.util.Constants.MOVE_BINDER
import com.oddlemon.chipsterplay.util.Constants.MOVE_DETAIL
import com.oddlemon.chipsterplay.util.Constants.MOVE_MAIN
import com.oddlemon.chipsterplay.util.Constants.MOVE_MAP
import com.oddlemon.chipsterplay.view.ar.ArpocaViewModel.Companion.POCATEXT_LOADING
import com.oddlemon.chipsterplay.view.ar.ArpocaViewModel.Companion.POCATEXT_1KM
import kotlinx.coroutines.*
import uk.co.appoly.arcorelocation.LocationMarker
import uk.co.appoly.arcorelocation.LocationScene
import uk.co.appoly.arcorelocation.utils.ARLocationPermissionHelper
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import com.oddlemon.chipsterplay.R

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
    private var latency = 0L
    private var fpsVisible = false

    @RequiresApi(VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ARLocationPermissionHelper.requestPermission(this)
        init()
        setContentView(binding.root)
        initObserver()
        initListener()
        setArView()

        binding.getPocaIv.setOnLongClickListener { //여긴가???
            if (fpsVisible) {
                binding.frameTv.visibility = View.GONE
                binding.latencyTv.visibility = View.GONE
                TinyDancer.hide(this)
                fpsVisible = false
            } else {
                binding.frameTv.visibility = View.VISIBLE
                binding.latencyTv.visibility = View.VISIBLE
                if (latency == 0L) {
                    binding.latencyTv.text = "Latency : 1ms"
                } else {
                    binding.latencyTv.text = "Latency : ${latency}ms"
                }
                TinyDancer.create()
                    .redFlagPercentage(.0f) // set red indicator for 10%....different from default
                    .startingXPosition(0)
                    .startingYPosition(1000)
                    .startingGravity(Gravity.START)
                    .addFrameDataCallback { previousFrameNS, currentFrameNS, droppedFrames ->
                        val fps = 1000000000 / (currentFrameNS - previousFrameNS)
                        if (fps <= 30) {
                            _binding?.frameTv?.text =
                                "FPS : " + 1000000000 / (currentFrameNS - previousFrameNS)
                        }
                    }
                    .show(this)
                fpsVisible = true
            }

            true
        }
    }

    private fun init() {
        packId = intent.getIntExtra("packId", DEFAULT_PACK_ID) // 테스트용 1208
        userId = intent.getIntExtra("userId", DEFAULT_USER_ID) // 432
        Log.i("dlgocks1", packId.toString() + userId.toString())
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
                    .error(com.google.android.material.R.drawable.mtrl_ic_error)
                    .into(viewRenderable.view.findViewById(R.id.ar_target_iv))
                viewRenderable.sizer = FixedWidthViewSizer(0.2f)
            }
        }

    }

    /** ARview를 생성한다. */
    private fun setArView() {
        arSceneView = binding.arSceneView

        // ViewRenderable 빌드
        arLayout = ViewRenderable.builder()
            .setView(this, R.layout.ar_target_layout)
            .build()

        // ViewRenderable 빌드 완료 후 처리
        arLayout.thenAccept { viewRenderable ->
            // 텍스처 설정
            Glide.with(this)
                .load("https://chipsterplay.soundgram.co.kr/media/arpoca/307/pack_img/target_img_1.png")
                .error(com.google.android.material.R.drawable.mtrl_ic_error)
                .into(viewRenderable.view.findViewById(R.id.ar_target_iv))
            viewRenderable.sizer = FixedWidthViewSizer(0.2f)

            // AR Scene에 ViewRenderable 추가
            arSceneView.scene.addChild(Node().apply {
                renderable = viewRenderable
                localPosition = Vector3(0f, 0f, -1f) // View의 위치 설정
            })
            //binding.scanningTv.text = POCATEXT_1KM
            observePoca()
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
            // 유저 좌표 정보 출력
//            Log.i(
//                "dlgocks",
//                "lat : " + gpsTracker.userlatitude.toString() + " long : " + gpsTracker.userlongitude.toString()
//            )

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
                val startTime = System.currentTimeMillis()
                val locationMarker = LocationMarker(
                    it.longitude,
                    it.latitude,
                    getArView(it)
                ).apply {
//                    setScaleAtDistance(false)
                    this.node.apply {
//                        val lookRotation =
//                            Quaternion.lookRotation(Vector3.zero(), Vector3.zero())
//                        worldRotation = lookRotation
//                        worldPosition = Vector3.one()
//                        worldScale = Vector3.one()
                        setRenderEvent {
//                            val cameraPosition = arSceneView.scene.camera.worldPosition
//                            val direction = Vector3.subtract(cameraPosition, node.worldPosition)
//                            val lookRotation = Quaternion.lookRotation(direction, Vector3.up())
//                            node.worldRotation = lookRotation
//                            worldScale = Vector3.one()
                            worldScale = Vector3.add(worldScale, Vector3.one())
                        }
                    }
                }
                locationScene?.mLocationMarkers?.add(locationMarker)

                val endTime = System.currentTimeMillis()
                latency = endTime - startTime
            }
        }
    }

//    private fun completeArLayout() {
//        CompletableFuture
//            .allOf(arLayout)
//            .handle<Any?> { _: Void?, throwable: Throwable? ->
//                if (throwable != null) {
//                    DemoUtils.displayError(this, "Unable to load renderables", throwable)
//                    return@handle null
//                }
//                try {
//                    arLayoutRenderable = arLayout.get()
//                    viewModel.hasFinishedLoading = true
//                } catch (ex: InterruptedException) {
//                    DemoUtils.displayError(this, "Unable to load renderables", ex)
//                } catch (ex: ExecutionException) {
//                    DemoUtils.displayError(this, "Unable to load renderables", ex)
//                }
//                null
//            }
//    }
private fun completeArLayout() {
    CompletableFuture
        .allOf(arLayout)
        .handle<Any?> { _: Void?, throwable: Throwable? ->
            if (throwable != null) {
                // 예외가 발생한 경우 처리
                val errorMessage = "Unable to load renderables: ${throwable.message}"
                Log.e("CompleteArLayout", errorMessage, throwable)
                // 사용자에게 적절한 안내 메시지를 표시
                runOnUiThread {
                    // 사용자에게 보여줄 메시지를 작성하고 알림 또는 다른 적절한 방법으로 표시
                    val message = "Sorry, we couldn't load the renderables. Please try again later."
                    showToastMessage(message)
                }
                // 예외 처리 완료 후 null 반환
                return@handle null
            }
            try {
                // 모든 CompletableFuture가 완료된 후 실행할 코드
                arLayoutRenderable = arLayout.get()
                viewModel.hasFinishedLoading = true
            } catch (ex: InterruptedException) {
                // InterruptedException 예외 처리
                handleException(ex)
            } catch (ex: ExecutionException) {
                // ExecutionException 예외 처리
                handleException(ex)
            }
            // 예외 처리 완료 후 null 반환
            null
        }
}

    // 예외 처리를 위한 함수
    private fun handleException(ex: Exception) {
        Log.e("CompleteArLayout", "Exception occurred", ex)
        runOnUiThread {
            // 사용자에게 보여줄 메시지를 작성하고 알림 또는 다른 적절한 방법으로 표시
            val message = "An error occurred: ${ex.message}"
            showToastMessage(message)
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
        // 이 함수 실행 시간을 측정
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
                                        url = "https://chipsterplay.soundgram.co.kr/media/arpoca/307/pack_img/pack_img_1.png"
                                    )
                                    binding.getMotionEndedIv.show()
                                    binding.getMotionTv.text = "카드를 위아래로 휘리릭 돌려 보라구~!"
                                    binding.getMotionIv.setImageResource(0)
                                }, 3200)
                            },
                        )
                        setLoadingFalse()
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
            this.putExtra("pocaId", viewModel.selectedPocaId.value)
        })
        finish()
    }


    /**
     * 랭킹 입력받아
     * 랭킹 레이아웃의 별 아이콘을 설정한다.
     */
//    private fun setRankingLayout(curQty: Int) {
//        binding.apply {
//            when (curQty) {
//                5 -> {
//                    ranking1Iv.setColorFilter(Color.parseColor(getString(R.string.rankingColor)))
//                    ranking2Iv.setColorFilter(Color.parseColor(getString(R.string.rankingColor)))
//                    ranking3Iv.setColorFilter(Color.parseColor(getString(R.string.rankingColor)))
//                    ranking4Iv.setColorFilter(Color.parseColor(getString(R.string.rankingColor)))
//                    ranking5Iv.setColorFilter(Color.parseColor(getString(R.string.rankingColor)))
//                }
//
//                4 -> {
//                    ranking1Iv.setColorFilter(Color.parseColor(getString(R.string.rankingColor)))
//                    ranking2Iv.setColorFilter(Color.parseColor(getString(R.string.rankingColor)))
//                    ranking3Iv.setColorFilter(Color.parseColor(getString(R.string.rankingColor)))
//                    ranking4Iv.setColorFilter(Color.parseColor(getString(R.string.rankingColor)))
//                    ranking5Iv.setColorFilter(Color.TRANSPARENT)
//                }
//
//                3 -> {
//                    ranking1Iv.setColorFilter(Color.parseColor(getString(R.string.rankingColor)))
//                    ranking2Iv.setColorFilter(Color.parseColor(getString(R.string.rankingColor)))
//                    ranking3Iv.setColorFilter(Color.parseColor(getString(R.string.rankingColor)))
//                    ranking4Iv.setColorFilter(Color.TRANSPARENT)
//                    ranking5Iv.setColorFilter(Color.TRANSPARENT)
//                }
//
//                2 -> {
//                    ranking1Iv.setColorFilter(Color.parseColor(getString(R.string.rankingColor)))
//                    ranking2Iv.setColorFilter(Color.parseColor(getString(R.string.rankingColor)))
//                    ranking3Iv.setColorFilter(Color.TRANSPARENT)
//                    ranking4Iv.setColorFilter(Color.TRANSPARENT)
//                    ranking5Iv.setColorFilter(Color.TRANSPARENT)
//                }
//
//                1 -> {
//                    ranking1Iv.setColorFilter(Color.parseColor(getString(R.string.rankingColor)))
//                    ranking2Iv.setColorFilter(Color.TRANSPARENT)
//                    ranking3Iv.setColorFilter(Color.TRANSPARENT)
//                    ranking4Iv.setColorFilter(Color.TRANSPARENT)
//                    ranking5Iv.setColorFilter(Color.TRANSPARENT)
//                }
//            }
//        }
//        binding.ranking1Iv.setColorFilter(Color.parseColor(getString(R.string.rankingColor)))
//    }


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

//    override fun onResume() {
//        super.onResume()
//        if (arSceneView.session == null) {
//            // 세션이 아직 만들어지지 않은 경우 렌더링을 다시 시작하지 마십시오.
//            // ARCore를 업데이트해야 하거나 사용 권한이 아직 부여되지 않은 경우 이 문제가 발생할 수 있습니다
//            try {
//                val session: Session? =
//                    DemoUtils.createArSession(this@ArActivity, viewModel.installRequested)
//                if (session == null) {
//                    viewModel.installRequested = ARLocationPermissionHelper.hasPermission(this)
//                    return
//                } else {
//                    arSceneView.setupSession(session)
//                }
//            } catch (e: UnavailableException) {
//                DemoUtils.handleSessionException(this, e)
//            }
//        }
//        try {
//            arSceneView.resume()
//            locationScene?.resume()
//        } catch (ex: CameraNotAvailableException) {
//            DemoUtils.displayError(this, "Unable to get camera", ex)
//            onFinish(MOVE_MAIN)
//            return
//        }
//        val cameraConfigFilter = CameraConfigFilter(arSceneView.session)
//        cameraConfigFilter.facingDirection = CameraConfig.FacingDirection.FRONT
//        val cameraConfigs =
//            arSceneView.session?.getSupportedCameraConfigs(cameraConfigFilter)
//        if (cameraConfigs?.isNotEmpty() == true) {
//            arSceneView.session?.cameraConfig = cameraConfigs[0]!!
//        }
//        gpsTracker = GpsTracker(this)
//        viewModel.userLat = gpsTracker.userlatitude
//        viewModel.userLong = gpsTracker.userlongitude
//    }
    override fun onResume() { //정상 작동 함수임 건들지 말것_240529_차수민
        super.onResume()
        if (arSceneView.session == null) {
            try {
                val session: Session? = DemoUtils.createArSession(this@ArActivity, viewModel.installRequested)
                if (session == null) {
                    viewModel.installRequested = ARLocationPermissionHelper.hasPermission(this)
                    return
                } else {
                    arSceneView.setupSession(session)
                    session.resume() // AR 세션을 설정한 후에 재개
                    val cameraConfigFilter = CameraConfigFilter(session)
                    cameraConfigFilter.facingDirection = CameraConfig.FacingDirection.FRONT
                    val cameraConfigs = session.getSupportedCameraConfigs(cameraConfigFilter)
                    if (cameraConfigs.isNotEmpty()) {
                        arSceneView.session?.cameraConfig = cameraConfigs[0]
                    }
                }
            } catch (e: UnavailableException) {
                DemoUtils.handleSessionException(this, e)
                return
            }
        }
        try {
            arSceneView.resume()
            locationScene?.resume()
            // GPS 트래커 초기화 코드는 그대로 유지됩니다.
            gpsTracker = GpsTracker(this)
            viewModel.userLat = gpsTracker.userlatitude
            viewModel.userLong = gpsTracker.userlongitude
            print(viewModel.userLat)
            print('\n')
            print(viewModel.userLong)
        } catch (ex: CameraNotAvailableException) {
            DemoUtils.displayError(this, "Unable to get camera", ex)
            onFinish(MOVE_MAIN)
            return
        }
    }

    public override fun onPause() {
        super.onPause()
        locationScene?.pause()
        arSceneView.pause()
    }

    override fun onStop() {
        super.onStop()
    }

    public override fun onDestroy() {
        super.onDestroy()
        if (fpsVisible) {
            TinyDancer.hide(this)
        }
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
