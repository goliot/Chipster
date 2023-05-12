package com.soundgram.chipster.domain.model.arpoca

class CategoryList(private val categoryList: List<Category>) : List<Category> by categoryList {

    fun find(id: Int) = categoryList.find {
        it.id == id
    }?.poca_category_name ?: "카테고리를 찾지 못했어요."
}