package com.soundgram.chipster.view.ar

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.AnimationDrawable
import android.location.Location
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soundgram.chipster.domain.repository.ArpocaRepository
import com.soundgram.chipster.network.ChipsterService
import com.soundgram.chipster.network.RestfulAdapter
import com.soundgram.chipster.domain.model.PackInfo
import com.soundgram.chipster.domain.model.Poca
import com.soundgram.chipster.domain.model.ArPlayerType
import com.soundgram.chipster.network.RestfulAdapter.chipsterService
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


    private val arpocaRepository: ArpocaRepository = ArpocaRepository(
        service = RestfulAdapter.chipsterService
    )

    // 카드가 회전하고 있는지
    var isRotating = false
    var installRequested = false
    var hasFinishedLoading = false

    var userLat = 0.0
    var userLong = 0.0

    private val _arPocaLocations: MutableLiveData<List<Location>> = MutableLiveData(emptyList())
    val arPocaLocations: LiveData<List<Location>> = _arPocaLocations

    private val _pocas: MutableLiveData<List<Poca>> = MutableLiveData(emptyList())
    val pocas: LiveData<List<Poca>> get() = _pocas

    private val _selectedPoca: MutableLiveData<Poca> = MutableLiveData()
    val selectedPoca: LiveData<Poca> get() = _selectedPoca

    private val _packInfo = MutableLiveData<PackInfo>()
    val packInfo: LiveData<PackInfo> get() = _packInfo

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

    //    fun getTotalCheckInData(
//        totId: Int,
//        userId: Int,
//        onError: (String) -> Unit
//    ) = viewModelScope.launch {
//        arpocaRepository.getTotalCheckInData(
//            totId = totId,
//            userId = userId,
//            onLoading = ::setLoadingTrue,
//            onComplete = ::setLoadingFalse
//        ).collectLatest { result ->
//            result.handleResponse(onError = onError) { res ->
//                when (res.response_code) {
//                    ONSUCCESS -> {
//                        pocaList =
//                            PocaList(res.pocas.filter { it.max_qty == null || it.max_qty > it.cur_qty })
//                        categories = CategoryList(res.categories)
//                        packList = res.packs
//                        // _packInfo.value = res.packs.first() // TODO 팩이 여러개가 될 수도 있을 듯 이부분 수정 필요
//                        _arPocaLocations.value = compareQty(res.locations)
//                        locations = LocationList(compareQty(res.locations))
//                        setScanningText()
//                    }
//                    else -> {
//                        onError("요청 값이 잘못됬습니다.")
//                    }
//                }
//            }
//        }
//    }
//
    fun getPocasWithPackId(
        packId: Int,
        onError: (String) -> Unit
    ) = viewModelScope.launch {
        arpocaRepository.getPocasWithPackId(
            packId = packId,
            onLoading = ::setLoadingTrue,
            onComplete = ::setLoadingFalse
        ).collectLatest { result ->
            result.handleResponse(onError = onError) { res ->
                _packInfo.value = res.packInfo
                _pocas.value = res.pocas
                setScanningText()
            }
        }
    }

    private fun setScanningText() {
//        val minDistance = locations.getMinDistance(userLat, userLong) ?: 1500.0
//        if (pocaList.isEmpty()) {
//            _scanningText.value = "획득가능한 포카가 없어요..!"
//            return
//        }
        val minDistance = 600

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
    fun setSelectedPoca(poca: Poca) {
        _selectedPoca.value = poca
    }

    /** 포카를 클릭했을 때 클릭했다는 값을 서버로 넘긴다. */
//    fun setUserDataWithPack(
//        userId: Int,
//        userLocationId: Int,
//        onSuccess: () -> Unit,
//        onError: (String) -> Unit,
//    ) = viewModelScope.launch {
//        selectedPoca.value?.let { poca ->
//            arpocaRepository.setUserDataWithPack(
//                packId = poca.pack_id,
//                userId = userId,
//                pocaId = poca.id,
//                locationId = userLocationId,
//            ).collectLatest { result ->
//                result.handleResponse(
//                    onError = onError,
//                    emptyMsg = "카드를 획득할 수 없어요."
//                ) { res ->
//                    when (res.response_code) {
//                        ERROR_ALREAY_GET_CARDED -> {
//                            onError("이미 획득한 카드예요!")
//                        }
//                        ONSUCCESS -> {
//                            /* 선택된 포카의 location_id만 가져온다. */
//                            _selectedPoca.value =
//                                _selectedPoca.value?.copy(
//                                    location_id = res.poca_info.location_id.toInt(),
//                                    register_time = res.poca_info.register_time
//                                )
//                            onSuccess()
//                        }
//                        else -> {
//                            onError("카드를 획득할 수 없어요.")
//                        }
//                    }
//                }
//            }
//        }
//    }

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
            deCodeUrlToAnimationDrawable(context, URL(packInfo.value?.getMotionImg))
        }
        viewModelScope.launch(Dispatchers.Main) {
            onDrawableReady(drawable.await())
        }
        when (arPlayerType) {
            ArPlayerType.CHECKIN -> {
//                getTotalCheckInData(totId = totId, userId = userId, onError = onError)
            }
            ArPlayerType.POCA -> {
//                getTotalDataWithPack(packId = packId, onError = onError
                //                )
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
                deCodeUrlToAnimationDrawable(context, URL(packInfo.value?.cardMotionImg))
            }
            viewModelScope.launch(Dispatchers.Main) {
                onDrawableReady(drawable.await())
            }
        }
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