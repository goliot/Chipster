package com.oddlemon.chipsterplay.domain.model

import com.google.gson.annotations.SerializedName

//data class PackInfo(
//    val allCardSize: Int? = 0,
//    val cardImg: String? = "",
//    val cardMotionImg: String,
//    val codeKey: String,
//    val commentSize: Int? = 0,
//    val content: String,
//    val endTime: String,
//    val getMotionImg: String,
//    val img: String,
//    val level1Msg: String,
//    val level2Msg: String,
//    val level3Msg: String,
//    val level4Msg: String,
//    val level5Msg: String,
//    val makerUserNickName: String? = "",
//    val music: Any,
//    val name: String,
//    val packId: Int,
//    val packStatus: Int,
//    val packType: Int,
//    val pinDoneImg: String,
//    val pinImg: String,
//    val pocaList: Any,
//    val pocaSize: Any,
//    val registerTime: String,
//    val representPocaAddress: Any,
//    val startTime: String,
//    val targetImg: String,
//    val timestamp: String,
//    val totType: Int,
//    val totalPocaSize: Any,
//    val updateTime: String,
//    val userSize: Any
//)

data class PackInfo(
    @SerializedName("pack") val pack: Pack
)

data class Pack(
    @SerializedName("id") val id: Int,
    @SerializedName("maker_userid") val makerUserId: Int,
    @SerializedName("pack_type") val packType: Int,
    @SerializedName("pack_name") val packName: String,
    @SerializedName("pack_content") val packContent: String,
    @SerializedName("pack_max_user") val packMaxUser: Int,
    @SerializedName("pack_img") val packImg: String,
    @SerializedName("pack_pin_img") val packPinImg: String,
    @SerializedName("level1msg") val level1msg: String,
    @SerializedName("pocas") val pocas: List<Poca>
)