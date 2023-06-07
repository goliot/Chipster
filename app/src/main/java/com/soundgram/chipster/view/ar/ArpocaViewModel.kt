package com.soundgram.chipster.view.ar

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.AnimationDrawable
import androidx.annotation.DrawableRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soundgram.chipster.domain.model.ArPocaDistanceType
import com.soundgram.chipster.domain.repository.ArpocaRepository
import com.soundgram.chipster.network.RestfulAdapter
import com.soundgram.chipster.domain.model.PackInfo
import com.soundgram.chipster.domain.model.Poca
import com.soundgram.chipster.network.request.GetArPocaRequest
import com.soundgram.chipster.util.Constants.IMAGE_PATH
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

    private val _pocas: MutableLiveData<List<Poca>> = MutableLiveData(emptyList())
    val pocas: LiveData<List<Poca>> get() = _pocas

    private val _packInfo = MutableLiveData<PackInfo>()
    val packInfo: LiveData<PackInfo> get() = _packInfo

    private val _isLoading: MutableLiveData<Boolean> = MutableLiveData(false)
    val isLoading: LiveData<Boolean> get() = _isLoading

    fun setLoadingTrue(): Unit = run { _isLoading.value = true }
    fun setLoadingFalse(): Unit = run { _isLoading.value = false }

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
            }
        }
    }


    /** 포카를 클릭했을 때 클릭했다는 값을 서버로 넘긴다. */
    fun setUserDataWithPack(
        userId: Int,
        packId: Int,
        pocaId: Int,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    ) = viewModelScope.launch {
        arpocaRepository.getArpocaGet(
            getArPocaRequest = GetArPocaRequest(
                packId = packId,
                userId = userId,
                pocaId = pocaId,
            )
        ).collectLatest { result ->
            result.handleResponse(
                onError = onError,
                emptyMsg = "카드를 획득할 수 없어요."
            ) { res ->
                onSuccess()
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
        onDrawableReady: (AnimationDrawable?) -> Unit,
    ) = viewModelScope.launch {
        val drawable: Deferred<AnimationDrawable?> = async {
            deCodeUrlToAnimationDrawable(
                context,
//                URL(IMAGE_PATH.format(packId, packInfo.value?.getMotionImg))
                URL("https://chipsterplay.soundgram.co.kr/media/arpoca/307/pack_img/getmotion_img_1.png")
            )
        }
        viewModelScope.launch(Dispatchers.Main) {
            onDrawableReady(drawable.await())
        }
    }

    fun onCardSwipeVertical(
        context: Context,
        onDrawableReady: (AnimationDrawable?) -> Unit,
    ) = viewModelScope.launch {
        if (!isRotating) {
            isRotating = true
            val drawable = async {
                deCodeUrlToAnimationDrawable(
                    context,
//                    URL(IMAGE_PATH.format(packInfo.value?.packId, packInfo.value?.cardMotionImg))
                    URL("https://chipsterplay.soundgram.co.kr/media/arpoca/307/pack_img/cardmotion_img_1.png")
                )
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
    }

}
