package com.soundgram.chipster.view.ar

import OnSwipeTouchListener
import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.core.exceptions.UnavailableException
import com.google.ar.sceneform.ArSceneView
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.FixedWidthViewSizer
import com.google.ar.sceneform.rendering.ViewRenderable
import com.soundgram.chipster.R
import com.soundgram.chipster.databinding.ActivityArBinding
import com.soundgram.chipster.util.*
import com.soundgram.chipster.view.ar.model.ArPlayerType
import kotlinx.coroutines.*
import uk.co.appoly.arcorelocation.LocationMarker
import uk.co.appoly.arcorelocation.LocationScene
import uk.co.appoly.arcorelocation.utils.ARLocationPermissionHelper
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

    private val showToastMessage: (String) -> Unit = {
        Toast.makeText(this@ArActivity, it, Toast.LENGTH_SHORT).show()
    }

    @RequiresApi(VERSION_CODES.N)
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
        totId = intent.getIntExtra("totId", 0) // 테스트용 185
        packId = intent.getIntExtra("packId", 0) // 테스트용 185
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
                viewModel.getTotalCheckInData(
                    totId = totId,
                    userId = userId,
                    onError = showToastMessage
                )
            }
            ArPlayerType.POCA -> {
                viewModel.getTotalDataWithPack(
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
                    url = viewModel.packInfo.value?.pack_target_img,
                )
                photoContentIv.setImageWithUrl(
                    this@ArActivity,
                    url = poca.poca_img,
                )
                detailPhotoIv.setImageWithUrl(
                    this@ArActivity,
                    url = poca.poca_img,
                )

                pocaCategoryTv.text = viewModel.categories.find(poca.poca_category_id)

                val pocaLevelText = when (poca.poca_level) {
                    1 -> viewModel.packInfo.value?.level1msg.toString()
                    2 -> viewModel.packInfo.value?.level2msg.toString()
                    3 -> viewModel.packInfo.value?.level3msg.toString()
                    4 -> viewModel.packInfo.value?.level4msg.toString()
                    else -> viewModel.packInfo.value?.level5msg.toString()
                }
                pocaLevelTv.text = pocaLevelText


                val registerTimeText = StringBuilder()
                registerTimeText.append("획득 날짜 : ${poca.register_time}\n")
                registerTimeText.append(
                    "획득 지역 : ${
                        viewModel.locations.find(poca.location_id)
                    }"
                )
                registerTimeTv.text = registerTimeText
                setRankingLayout(poca.poca_level)
            }
        }

        /** 팩 정보가 확인되면 Ar의 이미지를 변경한다. */
        viewModel.packInfo.observe(this) {
            arLayout.thenAccept { viewRenderable ->
                Glide.with(this)
                    .load(viewModel.packInfo.value?.pack_target_img)
                    .error(R.drawable.ic_map)
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
        completeArLayout()
    }

    private fun observePoca() {
        viewModel.arPocaLocations.observe(this) {
            it?.minByOrNull { location ->
                distanceOf(
                    location.latitude,
                    location.longitude,
                    gpsTracker.userlatitude,
                    gpsTracker.userlongitude,
                    "meter"
                )
            }?.let { item ->
                Log.i("ArpocaActivity", item.toString())
                // 100미터 이하라면 추가
//                if (it.minOf { location ->
//                        distance(
//                            location.latitude,
//                            location.longitude,
//                            gpsTracker.userlatitude,
//                            gpsTracker.userlongitude,
//                            "meter"
//                        )
//                    } > DISTANCE_METER_FROM_USER) return@let
                viewModel.updatePack(item.pack_id)
                Log.i("dlgocks1 - selectedPack", viewModel.packInfo.toString())
                val locationMarker = LocationMarker(
                    item.longitude,
                    item.latitude,
                    getArView(item)
                ).apply {
                    setScaleAtDistance(false)
                    height = 0f
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
//            locationScene.mLocationMarkers?.add(locationMarker)
//        }
//    }

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
    @SuppressLint("ClickableViewAccessibility")
    private fun initListener() {
        binding.apply {
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
        }
    }

    private fun getArView(item: com.soundgram.chipster.domain.model.arpoca.Location): Node {
        val base = Node()
        base.renderable = arLayoutRenderable

        base.setOnTouchListener { _, _ ->
            with(viewModel) {
                if (isLoading.value == true) return@setOnTouchListener false
                setLoadingTrue()
                setSelectedPoca(item.poca_id)

                setUserDataWithPack(
                    userId = userId,
                    userLocationId = item.id,
                    onError = {
                        showToastMessage(it)
                        setLoadingFalse()
                    },
                    onSuccess = {
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
                                        url = packInfo.value?.pack_card_img
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
                    })
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
        Handler(Looper.getMainLooper()).postDelayed({ doubleBackToExitPressedOnce = false }, 2000)
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
        gpsTracker = GpsTracker(this)
        viewModel.userLat = gpsTracker.userlatitude
        viewModel.userLong = gpsTracker.userlongitude
    }

    /**
     * Make sure we call locationScene.pause();
     */
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
        const val MOVE_MAIN = 1
        const val MOVE_BINDER = 2
        const val MOVE_MAP = 3
    }


}