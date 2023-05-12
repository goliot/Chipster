package com.soundgram.chipster.domain.model.arpoca

import com.soundgram.chipster.view.ar.ArpocaViewModel


class PocaList(private val pocaList: List<Poca>) : List<Poca> by pocaList {

    init {
    }

    fun isExistNullLocationId() = pocaList.none { it.location_id == null }

    override fun toString(): String {
        return pocaList.toString()
    }

    fun select(id: Int) = pocaList.find {
        it.id == id
    } ?: throw java.lang.IllegalStateException("선택된 포카 Id를 가진 값이 없어요.")

    /**
     * 포카 난이도에 기반하여 랜덤 Id를 뽑는다.
     */
    fun randomPocaId(): Int = pocaList
        .filter { it.location_id == null }
        .filter { it.max_qty == null || it.max_qty > it.cur_qty }
        .let {
            require(it.isNotEmpty()) {
                "poca_id == null인데 location_id == null인 포카가 없을 경우가 발생"
            }
            generateWeightTable(it)
        }
        .shuffled()
        .first()

    private fun generateWeightTable(pocas: List<Poca>): MutableList<Int> {
        val randomPocaWeight = mutableListOf<Int>()
        pocas.map { poca ->
            repeat(ArpocaViewModel.MAX_WEIGHT - poca.poca_level) {
                randomPocaWeight.add(poca.id)
            }
        }
        return randomPocaWeight
    }
}