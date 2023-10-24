package com.soundgram.chipster.domain.model

import android.util.Log
import androidx.annotation.DrawableRes
import com.soundgram.chipster.R
import com.soundgram.chipster.view.ar.ArpocaViewModel.Companion.POCATEXT_100M
import com.soundgram.chipster.view.ar.ArpocaViewModel.Companion.POCATEXT_1KM
import com.soundgram.chipster.view.ar.ArpocaViewModel.Companion.POCATEXT_200M
import com.soundgram.chipster.view.ar.ArpocaViewModel.Companion.POCATEXT_500M

enum class ArPocaDistanceType(
    val text: String,
    @DrawableRes val imgage: Int,
    val distance: Int // Meter
) {

    POCA_1KM(POCATEXT_1KM, R.drawable.img_distance_01, 1_000),
    POCA_500M(POCATEXT_500M, R.drawable.img_distance_02, 500),
    POCA_200M(POCATEXT_200M, R.drawable.img_distance_03, 200),
    POCA_100M(POCATEXT_100M, R.drawable.img_distance_04, 100);


    companion object {
        fun findByDistance(distance: Double): ArPocaDistanceType {
            return when {
                distance >= POCA_1KM.distance -> POCA_1KM
                distance >= POCA_500M.distance -> POCA_500M
                distance >= POCA_200M.distance -> POCA_200M
                else -> POCA_100M
            }
        }
    }
}