package com.soundgram.chipster.view.ar

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.AnimationDrawable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soundgram.chipster.domain.model.arpoca.*
import com.soundgram.chipster.domain.repository.ArpocaRepository
import com.soundgram.chipster.network.ArService
import com.soundgram.chipster.network.ResponseCodes.ERROR_ALREAY_GET_CARDED
import com.soundgram.chipster.network.ResponseCodes.ONSUCCESS
import com.soundgram.chipster.network.RestfulAdapter
import com.soundgram.chipster.view.ar.model.ArPlayerType
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import oupson.apng.Loader
import oupson.apng.decoder.ApngDecoder
import java.io.ByteArrayInputStream
import java.net.URL

class ArpocaViewModel : ViewModel() {

//    val retrofit = Retrofit
//        .Builder()
//        .baseUrl("http://devapi.soundgram.co.kr:10080/")
//        .client(
//            OkHttpClient.Builder().addInterceptor(
//                HttpLoggingInterceptor().apply {
//                    level = HttpLoggingInterceptor.Level.BODY
//                }).build()
//        ).addConverterFactory(GsonConverterFactory.create())
//        .build()

    private val arpocaRepository: ArpocaRepository = ArpocaRepository(
        service = RestfulAdapter.createService(
            null,
            ArService::class.java,
            "root", "1Howtobiz!"
        )
    )

    // 카드가 회전하고 있는지
    var isRotating = false
    var installRequested = false
    var hasFinishedLoading = false

    // 전체포카에 대한 정보들
    var pocaList: PocaList = PocaList(emptyList())
    var packList = listOf<Pack>()
    lateinit var categories: CategoryList
    lateinit var locations: LocationList

    var userLat = 0.0
    var userLong = 0.0

    private val _arPocaLocations: MutableLiveData<List<Location>> = MutableLiveData(emptyList())
    val arPocaLocations: LiveData<List<Location>> = _arPocaLocations

    private val _selectedPoca: MutableLiveData<Poca> = MutableLiveData()
    val selectedPoca: LiveData<Poca> get() = _selectedPoca

    private val _packInfo = MutableLiveData<Pack>()
    val packInfo: LiveData<Pack> get() = _packInfo

    private val _isLoading: MutableLiveData<Boolean> = MutableLiveData(false)
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _detailCardVisible: MutableLiveData<Boolean> = MutableLiveData(false)
    val detailCardVisible: LiveData<Boolean> get() = _detailCardVisible

    private val _scanningText: MutableLiveData<String> = MutableLiveData(POCATEXT_LOADING)
    val scanningText: LiveData<String> get() = _scanningText

    fun setLoadingTrue(): Unit = run { _isLoading.value = true }
    fun setLoadingFalse(): Unit = run { _isLoading.value = false }
    fun handleDetailCardVisible(visible: Boolean) {
        _detailCardVisible.value = visible
    }

    fun getTotalCheckInData(
        totId: Int,
        userId: Int,
        onError: (String) -> Unit
    ) = viewModelScope.launch {
        arpocaRepository.getTotalCheckInData(
            totId = totId,
            userId = userId,
            onLoading = ::setLoadingTrue,
            onComplete = ::setLoadingFalse
        ).collectLatest { result ->
            result.handleResponse(onError = onError) { res ->
                when (res.response_code) {
                    ONSUCCESS -> {
                        pocaList =
                            PocaList(res.pocas.filter { it.max_qty == null || it.max_qty > it.cur_qty })
                        categories = CategoryList(res.categories)
                        packList = res.packs
                        // _packInfo.value = res.packs.first() // TODO 팩이 여러개가 될 수도 있을 듯 이부분 수정 필요
                        _arPocaLocations.value = compareQty(res.locations)
                        locations = LocationList(compareQty(res.locations))
                        setScanningText()
                    }
                    else -> {
                        onError("요청 값이 잘못됬습니다.")
                    }
                }
            }
        }
    }

    fun getTotalDataWithPack(
        packId: Int,
        onError: (String) -> Unit
    ) = viewModelScope.launch {
        arpocaRepository.getTotalDataWithPack(
            pacKId = packId,
            onLoading = ::setLoadingTrue,
            onComplete = ::setLoadingFalse
        ).collectLatest { result ->
            result.handleResponse(onError = onError) { res ->
                when (res.response_code) {
                    ONSUCCESS -> {
                        pocaList =
                            PocaList(PocaList(res.pocas.filter { it.max_qty == null || it.max_qty > it.cur_qty }))
                        categories = CategoryList(res.categories)
                        packList = res.packs
//                        _packInfo.value = res.packs.first() // TODO 팩이 여러개가 될 수도 있을 듯 이부분 수정 필요
                        _arPocaLocations.value = compareQty(res.locations)
                        locations = LocationList(compareQty(res.locations))
                        setScanningText()
                    }
                    else -> {
                        onError("요청 값이 잘못됬습니다.")
                    }
                }
            }
        }
    }

    /** max_qty, cur_qty를 비교하여 올바른 location값만 추출한다. */
    private fun compareQty(locations: List<Location>): List<Location> {
        val result = mutableListOf<Location>()
        val flag = pocaList.filter { it.location_id == null }
            .filter { it.max_qty == null || it.max_qty > it.cur_qty }.isNotEmpty()
        locations.forEach { item ->
            /** poca_id가 null일 때 location_id가 null인 포카들 중 획득할 포카가 있는 경우에만 추가한다. */
            if (item.poca_id == null && flag) {
                result.add(item)
            }

            /** poca_id가 null이 아닌데 max_qty > cur_qty일 경우 추가한다.*/
            item.poca_id?.let { id ->
                pocaList.find { poca ->
                    poca.id == id
                }.let { poca ->
                    if (poca?.max_qty == null) {
                        result.add(item)
                        return@forEach
                    }
                    if (poca.max_qty > poca.cur_qty) {
                        result.add(item)
                    }
                }
            }
        }
        return result.toList()
    }

    private fun setScanningText() {
        val minDistance = locations.getMinDistance(userLat, userLong) ?: 1500.0

        if (pocaList.isEmpty()) {
            _scanningText.value = "획득가능한 포카가 없어요..!"
            return
        }

        if (minDistance > 500) {
            _scanningText.value = POCATEXT_1KM
            return
        }
        if (minDistance >= 200) {
            _scanningText.value = POCATEXT_500M
            return
        }
        if (minDistance > 100) {
            _scanningText.value = POCATEXT_200M
            return
        }
        _scanningText.value = POCATEXT_100M
    }

    /** poca_id를 사용해 선택중인 포카를 바꾼다. */
    fun setSelectedPoca(pocaId: Int?) {
        _selectedPoca.value = pocaList.select(pocaId ?: pocaList.randomPocaId())
    }

    /** 포카를 클릭했을 때 클릭했다는 값을 서버로 넘긴다. */
    fun setUserDataWithPack(
        userId: Int,
        userLocationId: Int,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    ) = viewModelScope.launch {
        selectedPoca.value?.let { poca ->
            arpocaRepository.setUserDataWithPack(
                packId = poca.pack_id,
                userId = userId,
                pocaId = poca.id,
                locationId = userLocationId,
            ).collectLatest { result ->
                result.handleResponse(
                    onError = onError,
                    emptyMsg = "카드를 획득할 수 없어요."
                ) { res ->
                    when (res.response_code) {
                        ERROR_ALREAY_GET_CARDED -> {
                            onError("이미 획득한 카드예요!")
                        }
                        ONSUCCESS -> {
                            /* 선택된 포카의 location_id만 가져온다. */
                            _selectedPoca.value =
                                _selectedPoca.value?.copy(
                                    location_id = res.poca_info.location_id.toInt(),
                                    register_time = res.poca_info.register_time
                                )
                            onSuccess()
                        }
                        else -> {
                            onError("카드를 획득할 수 없어요.")
                        }
                    }
                }
            }
        }
    }

    private suspend fun deCodeUrlToAnimationDrawable(
        context: Context,
        url: URL
    ): AnimationDrawable? {
        return try {
            (ApngDecoder.decodeApng(
                context,
                ByteArrayInputStream(
                    Loader.load(url)
                ),
                1f,
                Bitmap.Config.ARGB_8888
            ) as AnimationDrawable).apply {
                isOneShot = true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun onPocaClick(
        context: Context,
        packId: Int,
        onDrawableReady: (AnimationDrawable?) -> Unit,
        onError: (String) -> Unit,
        arPlayerType: ArPlayerType,
        totId: Int,
        userId: Int,
    ) = viewModelScope.launch {
        val drawable: Deferred<AnimationDrawable?> = async {
            deCodeUrlToAnimationDrawable(context, URL(packInfo.value?.pack_getmotion_img))
        }
        viewModelScope.launch(Dispatchers.Main) {
            onDrawableReady(drawable.await())
        }
        when (arPlayerType) {
            ArPlayerType.CHECKIN -> {
                getTotalCheckInData(totId = totId, userId = userId, onError = onError)
            }
            ArPlayerType.POCA -> {
                getTotalDataWithPack(packId = packId, onError = onError)
            }
        }
    }

    fun onCardSwipeVertical(
        context: Context,
        onDrawableReady: (AnimationDrawable?) -> Unit,
    ) = viewModelScope.launch {
        if (!isRotating) {
            isRotating = true
            val drawable = async {
                deCodeUrlToAnimationDrawable(context, URL(packInfo.value?.pack_cardmotion_img))
            }
            viewModelScope.launch(Dispatchers.Main) {
                onDrawableReady(drawable.await())
            }
        }
    }

    fun updatePack(packId: Int) {
        _packInfo.value = packList.find { it.id == packId } ?: return
    }

    companion object {
        const val POCATEXT_1KM = "랜덤 포카의 위치는 맵으로 확인할 수 있지.\n모두 찾아서 ★올카★를 완성하자!"
        const val POCATEXT_500M = "왠지 동네에 있는 느낌적인 느낌!\n하루 30분 걷기는 건강한 덕질에 도움이 되지."
        const val POCATEXT_200M = "두근두근두근, 꽤 근처에 있는 것 같아!"
        const val POCATEXT_100M = "포카가 나타났다! GET IT!"
        const val POCATEXT_LOADING = "어서와, AR 포카는 처음이지?\n처음 만나는 AR 포카, 이제 곧 열릴 거야!"
        const val MAX_WEIGHT = 6
    }

}