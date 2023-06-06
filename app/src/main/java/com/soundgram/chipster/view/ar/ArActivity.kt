package com.soundgram.chipster.view.ar

import OnSwipeTouchListener
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.ImageReader.OnImageAvailableListener
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.google.ar.core.*
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.core.exceptions.UnavailableException
import com.google.ar.sceneform.ArSceneView
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.FixedWidthViewSizer
import com.google.ar.sceneform.rendering.MaterialFactory
import com.google.ar.sceneform.rendering.ShapeFactory
import com.google.ar.sceneform.rendering.ViewRenderable
import com.google.zxing.integration.android.IntentIntegrator.REQUEST_CODE
import com.soundgram.chipster.R
import com.soundgram.chipster.databinding.ActivityArBinding
import com.soundgram.chipster.domain.model.ArPlayerType
import com.soundgram.chipster.domain.model.Poca
import com.soundgram.chipster.service.MediaProjectionAccessService
import com.soundgram.chipster.util.*
import com.soundgram.chipster.util.Constants.IMAGE_PATH
import kotlinx.coroutines.*
import uk.co.appoly.arcorelocation.LocationMarker
import uk.co.appoly.arcorelocation.LocationScene
import uk.co.appoly.arcorelocation.utils.ARLocationPermissionHelper
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException


class ArActivity : AppCompatActivity() {

    private var doubleBackToExitPressedOnce = false
    private var arPlayerType = ArPlayerType.POCA
    private var packId = 0
    private var totId = 0
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
    private lateinit var mediaprojectionIntent: Intent
    private val showToastMessage: (String) -> Unit = {
        Toast.makeText(this@ArActivity, it, Toast.LENGTH_SHORT).show()
    }


    @RequiresApi(VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mediaprojectionIntent = Intent(this, MediaProjectionAccessService::class.java)
        ARLocationPermissionHelper.requestPermission(this)
        mediaProjectionManager =
            getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startForegroundService(mediaprojectionIntent)
        val projectionIntent = mediaProjectionManager.createScreenCaptureIntent()
        startActivityForResult(projectionIntent, REQUEST_CODE)

        init()
        setContentView(binding.root)
        initObserver()
        initListener()
        setArView()
    }

    private fun init() {
        totId = intent.getIntExtra("totId", 0) // 테스트용 185
        packId = intent.getIntExtra("packId", 307) // 테스트용 185
        userId = intent.getIntExtra("userId", 0) // 1025
        arPlayerType = if (totId != 0) {
            ArPlayerType.CHECKIN
        } else {
            ArPlayerType.POCA
        }
        _binding = ActivityArBinding.inflate(layoutInflater)
        viewModel = ViewModelProvider(this)[ArpocaViewModel::class.java]
        binding.arSceneView.planeRenderer.isVisible = false

        when (arPlayerType) {
            ArPlayerType.CHECKIN -> {
            }
            ArPlayerType.POCA -> {
                viewModel.getPocasWithPackId(
                    packId = packId,
                    onError = showToastMessage
                )
            }
        }
    }

    /** 뷰모델에서 옵저빙할 값들을 정의한다. */
    @RequiresApi(VERSION_CODES.N)
    private fun initObserver() {

        viewModel.selectedPoca.observe(this) { poca ->
            if (poca == null) return@observe
            binding.apply {
                detailCircleIv.setImageWithUrl(
                    this@ArActivity,
                    url = IMAGE_PATH.format(packId, viewModel.packInfo.value?.targetImg),
                )
                photoContentIv.setImageWithUrl(
                    this@ArActivity,
                    url = poca.img,
                )
                detailPhotoIv.setImageWithUrl(
                    this@ArActivity,
                    url = poca.img,
                )

//                pocaCategoryTv.text = viewModel.categories.find(poca.poca_category_id)

                val pocaLevelText = when (poca.level) {
                    1 -> viewModel.packInfo.value?.level1Msg.toString()
                    2 -> viewModel.packInfo.value?.level2Msg.toString()
                    3 -> viewModel.packInfo.value?.level3Msg.toString()
                    4 -> viewModel.packInfo.value?.level4Msg.toString()
                    else -> viewModel.packInfo.value?.level5Msg.toString()
                }
                pocaLevelTv.text = pocaLevelText


                val registerTimeText = StringBuilder()
//                registerTimeText.append("획득 날짜 : ${poca.register_time}\n")
//                registerTimeText.append(
//                    "획득 지역 : ${
//                        viewModel.locations.find(poca.location_id)
//                    }"
//                )
                registerTimeTv.text = registerTimeText
                setRankingLayout(poca.level ?: 1)
            }
        }

        /** 팩 정보가 확인되면 Ar의 이미지를 변경한다. */
        viewModel.packInfo.observe(this) {
            arLayout.thenAccept { viewRenderable ->
                Glide.with(this)
                    .load(IMAGE_PATH.format(packId, viewModel.packInfo.value?.targetImg))
                    .error(R.drawable.bottom_04)
                    .into(viewRenderable.view.findViewById(R.id.ar_target_iv))
                viewRenderable.sizer = FixedWidthViewSizer(0.2f)
            }
        }

        viewModel.scanningText.observe(this) {
            binding.scanningTv.text = it
        }

    }

    /** ARview를 생성한다. */
    @RequiresApi(VERSION_CODES.N)
    private fun setArView() {
        arSceneView = binding.arSceneView
        arLayout = ViewRenderable.builder()
            .setView(this, R.layout.ar_target_layout)
            .build()
        completeArLayout()

        MaterialFactory.makeOpaqueWithColor(this, com.google.ar.sceneform.rendering.Color(0x000000))
            .thenAccept { material ->
//                val node = Node()
//                node.setParent(binding.arSceneView.scene)
//                node.renderable =
//                    ShapeFactory.makeSphere(0.1f, Vector3(0.0f, 0.15f, 0.0f), material)
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

                    val camera = binding.arSceneView.scene.camera
                    val ray = camera.screenPointToRay(1080 / 2f, 1920 / 2f)
                    val newPosition = ray.getPoint(1f)
//                    node.localPosition = newPosition

                    locationScene?.processFrame(frame)
                }
            }
        // 신이 업데이트되면 계속 진행됨
    }

//    private fun testMarkers() {
//        val locaionList = listOf(
//            Location( // 벤쳐기업센터
//                id = 102,
//                pack_id = 183,
//                register_time = "2021-08-12 13:35:00",
//                poca_id = null,
//                address = "LG유플러스",
//                latitude = 37.598,
//                longitude = 126.8652
//            ),
//            Location( //전자관
//                id = 103,
//                pack_id = 183,
//                register_time = "2021-08-12 13:35:00",
//                poca_id = null,
//                address = "LG유플러스",
//                latitude = 37.6006,
//                longitude = 126.865
//            ),
//            Location( //화전역
//                id = 102,
//                pack_id = 183,
//                register_time = "2021-08-12 13:35:00",
//                poca_id = null,
//                address = "LG유플러스",
//                latitude = 37.603,
//                longitude = 126.8687
//            ),
//            Location( //덕양 중
//                id = 102,
//                pack_id = 183,
//                register_time = "2021-08-12 13:35:00",
//                poca_id = null,
//                address = "LG유플러스",
//                latitude = 37.6032,
//                longitude = 126.8729
//            ),
//            Location( // 동창회
//                id = 102,
//                pack_id = 183,
//                register_time = "2021-08-12 13:35:00",
//                poca_id = null,
//                address = "LG유플러스",
//                latitude = 37.5997,
//                longitude = 126.8655
//            ),
//            Location( // 도서관
//                id = 102,
//                pack_id = 183,
//                register_time = "2021-08-12 13:35:00",
//                poca_id = null,
//                address = "LG유플러스",
//                latitude = 37.5984,
//                longitude = 126.8642
//            ),
//        )
//        locaionList.forEach { item ->
//            val locationMarker = LocationMarker(
//                item.longitude,
//                item.latitude,
//                getArView(item)
//            ).apply {
//                setScaleAtDistance(false)
//                height = 0f
//                this.node.apply {
//                    val lookRotation =
//                        Quaternion.lookRotation(Vector3.zero(), Vector3.zero())
//                    worldRotation = lookRotation
//                    worldScale = Vector3.one()
//                    setRenderEvent { locationNode ->
//                        worldScale = Vector3.one()
//                    }
//                }
//            }
//            locationScene?.mLocationMarkers?.add(locationMarker)
//        }
//    }

    private fun observePoca() {
        viewModel.pocas.observe(this) { pocas ->
            val closedPoca = pocas?.minByOrNull { poca ->
                distanceOf(
                    poca.latitude,
                    poca.longitude,
                    gpsTracker.userlatitude,
                    gpsTracker.userlongitude,
                    "meter"
                )
            }
            closedPoca?.let {
                val locationMarker = LocationMarker(
                    it.longitude,
                    it.latitude,
                    getArView(it)
                ).apply {
                    setScaleAtDistance(false)
//                    height = 0f
                    this.node.apply {

//                        val lookRotation =
//                            Quaternion.lookRotation(Vector3.zero(), Vector3.zero())
//                        worldRotation = lookRotation
//                        worldPosition = Vector3.one()
//                        worldScale = Vector3.one()
//                        setRenderEvent {
//                            worldScale = Vector3.one()
//                        }
                    }
                }
                locationScene?.mLocationMarkers?.add(locationMarker)
            }
        }
//        viewModel.arPocaLocations.observe(this) {
//            it?.minByOrNull { location ->
//                distanceOf(
//                    location.latitude,
//                    location.longitude,
//                    gpsTracker.userlatitude,
//                    gpsTracker.userlongitude,
//                    "meter"
//                )
//            }?.let { item ->
//                Log.i("ArpocaActivity", item.toString())
//                // 100미터 이하라면 추가
////                if (it.minOf { location ->
////                        distance(
////                            location.latitude,
////                            location.longitude,
////                            gpsTracker.userlatitude,
////                            gpsTracker.userlongitude,
////                            "meter"
////                        )
////                    } > DISTANCE_METER_FROM_USER) return@let
//                Log.i("dlgocks1 - selectedPack", viewModel.packInfo.toString())
//                val locationMarker = LocationMarker(
//                    item.longitude,
//                    item.latitude,
//                    getArView(item)
//                ).apply {
//                    setScaleAtDistance(false)
//                    height = 0f
//                    this.node.apply {
//                        val lookRotation =
//                            Quaternion.lookRotation(Vector3.zero(), Vector3.zero())
//                        worldRotation = lookRotation
//                        worldPosition = Vector3.one()
//                        worldScale = Vector3.one()
//                        setRenderEvent {
//                            worldScale = Vector3.one()
//                        }
//                    }
//                }
//                locationScene?.mLocationMarkers?.add(locationMarker)
//            }
//        }
    }

    @RequiresApi(VERSION_CODES.N)
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
                                    onRotatingEnd()
                                }, 2000)
                            }
                        }
                    )
                }
            })

            cardDetailLayout.setOnClickListener {
                // Do nothing
            }

            cardDetailExitIv.setOnClickListener {
                closeDetailLayout()
            }


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

            binding.confirmBt.setOnClickListener {
                if (!::mediaProjection.isInitialized) return@setOnClickListener
                val metrics = resources.displayMetrics
                val imageReader = ImageReader.newInstance(
                    binding.arLayout.width,
                    binding.arLayout.height,
                    PixelFormat.RGBA_8888,
                    1
                )
                val virtualDisplay = mediaProjection.createVirtualDisplay(
                    "VirtualDisplay",
                    binding.arLayout.width,
                    binding.arLayout.height,
                    metrics.densityDpi,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    imageReader.surface,
                    VirtualDisplayCallback(),
                    null
                )
                val imageAvailableListener =
                    OnImageAvailableListener { reader ->
                        val image = reader.acquireLatestImage()
                        if (image != null) {
                            handleCaptureImage(image)
                            imageReader.setOnImageAvailableListener(null, null)
                            virtualDisplay.release()
                            image.close()
                        }
                    }
                imageReader.setOnImageAvailableListener(imageAvailableListener, null)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data!!)
        }
    }

    inner class VirtualDisplayCallback : VirtualDisplay.Callback() {
        override fun onPaused() {
            super.onPaused()
            Log.e("dlgocks1", "VirtualDisplayCallback: onPaused")
        }

        override fun onResumed() {
            super.onResumed()
            Log.e("dlgocks1", "VirtualDisplayCallback: onResumed")
        }

        override fun onStopped() {
            super.onStopped()
            Log.e("dlgocks1", "VirtualDisplayCallback: onStopped")
        }

    }

    private fun handleCaptureImage(image: Image?) {
        if (image == null) Log.i("dlgocks1", "image가 널임")
        if (image != null) {

            val planes = image.planes
            val buffer: ByteBuffer = planes[0].buffer
            val pixelStride = planes[0].pixelStride
            val rowStride = planes[0].rowStride
            val rowPadding = rowStride - pixelStride * image.width
            val width = image.width
            val height = image.height
            val bitmap = Bitmap.createBitmap(
                width + rowPadding / pixelStride,
                height,
                Bitmap.Config.ARGB_8888
            )
            bitmap.copyPixelsFromBuffer(buffer)
            image.close()
            Bitmap.createBitmap(bitmap, 0, 0, width, height)
            // 이미지 저장
            getImageUri(this@ArActivity, bitmap)
        }
    }


    private fun getImageUri(context: Context, bitmap: Bitmap): Uri {
        val fileName =
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(System.currentTimeMillis())
        return Uri.parse(
            MediaStore.Images.Media.insertImage(
                context.contentResolver,
                bitmap,
                fileName,
                null
            )
        )
    }

    private fun getArView(item: Poca): Node {
        val base = Node()
        base.renderable = arLayoutRenderable

        base.setOnTouchListener { _, _ ->
            with(viewModel) {
                if (isLoading.value == true) return@setOnTouchListener false
                setLoadingTrue()
                setSelectedPoca(item)
//                setUserDataWithPack(
//                    userId = userId,
//                    userLocationId = item.id,
//                    onError = {
//                        showToastMessage(it)
//                        setLoadingFalse()
//                    },
//                    onSuccess = {
                onPocaClick(
                    context = this@ArActivity,
                    packId = packId,
                    userId = userId,
                    totId = totId,
                    arPlayerType = arPlayerType,
                    onDrawableReady = { animatedDrawable ->
                        binding.getMotionIv.setImageDrawable(animatedDrawable)
                        animatedDrawable?.start()
                        onTouchMarker()
                        Handler(Looper.getMainLooper()).postDelayed({
                            binding.getMotionEndedIv.setImageWithUrl(
                                this@ArActivity,
                                url = IMAGE_PATH.format(packId, packInfo.value?.cardImg)
                            )
                            binding.getMotionEndedIv.show()
                            binding.getMotionTv.text = "카드를 위아래로 휘리릭 돌려 보라구~!"
                            binding.getMotionIv.setImageResource(0)
                        }, 3200)
                    },
                    onError = { errormsg ->
                        showToastMessage(errormsg)
                    }
                )
            }
            false
        }
        return base
    }

    private fun closeDetailLayout() {
        viewModel.handleDetailCardVisible(false)
        viewModel.setLoadingFalse()
        binding.apply {
            cardDetailLayout.hide()
            getMotionTv.hide()
            scanningTv.show()
            scanningIv.show()
            getPocaIv.setBackgroundResource(R.drawable.bottom_04)
        }
    }

    private fun onRotatingEnd() {
        viewModel.handleDetailCardVisible(true)
        viewModel.isRotating = false
        binding.apply {
            rotationIv.hide()
            photoIv.show()
            rotationIv.setImageResource(0)
            photoIv.hide()
            val animation: Animation =
                AnimationUtils.loadAnimation(applicationContext, R.anim.expand_scale_80to100)
            cardDetailLayout.startAnimation(animation)
            cardDetailLayout.show()
        }
    }

    private fun onTouchMarker() {
        binding.apply {
            scanningTv.hide()
            scanningIv.hide()
            getMotionIv.show()
            getMotionTv.show()
            getMotionTv.text = "잡았다!! 요놈!!"
            getPocaIv.setBackgroundResource(R.drawable.bottom_05)
        }
    }

    /**
     * 랭킹 입력받아
     * 랭킹 레이아웃의 별 아이콘을 설정한다.
     */
    private fun setRankingLayout(curQty: Int) {
        binding.apply {
            when (curQty) {
                5 -> {
                    ranking1Iv.setColorFilter(Color.parseColor(getString(R.string.rankingColor)))
                    ranking2Iv.setColorFilter(Color.parseColor(getString(R.string.rankingColor)))
                    ranking3Iv.setColorFilter(Color.parseColor(getString(R.string.rankingColor)))
                    ranking4Iv.setColorFilter(Color.parseColor(getString(R.string.rankingColor)))
                    ranking5Iv.setColorFilter(Color.parseColor(getString(R.string.rankingColor)))
                }
                4 -> {
                    ranking1Iv.setColorFilter(Color.parseColor(getString(R.string.rankingColor)))
                    ranking2Iv.setColorFilter(Color.parseColor(getString(R.string.rankingColor)))
                    ranking3Iv.setColorFilter(Color.parseColor(getString(R.string.rankingColor)))
                    ranking4Iv.setColorFilter(Color.parseColor(getString(R.string.rankingColor)))
                    ranking5Iv.setColorFilter(Color.TRANSPARENT)
                }
                3 -> {
                    ranking1Iv.setColorFilter(Color.parseColor(getString(R.string.rankingColor)))
                    ranking2Iv.setColorFilter(Color.parseColor(getString(R.string.rankingColor)))
                    ranking3Iv.setColorFilter(Color.parseColor(getString(R.string.rankingColor)))
                    ranking4Iv.setColorFilter(Color.TRANSPARENT)
                    ranking5Iv.setColorFilter(Color.TRANSPARENT)
                }
                2 -> {
                    ranking1Iv.setColorFilter(Color.parseColor(getString(R.string.rankingColor)))
                    ranking2Iv.setColorFilter(Color.parseColor(getString(R.string.rankingColor)))
                    ranking3Iv.setColorFilter(Color.TRANSPARENT)
                    ranking4Iv.setColorFilter(Color.TRANSPARENT)
                    ranking5Iv.setColorFilter(Color.TRANSPARENT)
                }
                1 -> {
                    ranking1Iv.setColorFilter(Color.parseColor(getString(R.string.rankingColor)))
                    ranking2Iv.setColorFilter(Color.TRANSPARENT)
                    ranking3Iv.setColorFilter(Color.TRANSPARENT)
                    ranking4Iv.setColorFilter(Color.TRANSPARENT)
                    ranking5Iv.setColorFilter(Color.TRANSPARENT)
                }
            }
        }
        binding.ranking1Iv.setColorFilter(Color.parseColor(getString(R.string.rankingColor)))
    }

    private fun onFinish(resultCode: Int) {
        if (viewModel.isLoading.value == false) {
            setResult(resultCode)
            finish()
        }
    }

    override fun onBackPressed() {
        if (viewModel.detailCardVisible.value == true) {
            closeDetailLayout()
            return
        }

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
//        if (arSceneView?.session != null) {
//            showLoadingMessage()
//        }
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
        stopService(mediaprojectionIntent)
        if (::mediaProjection.isInitialized) {
            mediaProjection.stop()
        }
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
        const val MOVE_MAIN = 1
        const val MOVE_BINDER = 2
        const val MOVE_MAP = 3
    }


}