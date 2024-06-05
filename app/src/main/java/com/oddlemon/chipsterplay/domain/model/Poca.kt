package com.oddlemon.chipsterplay.domain.model
import com.google.gson.annotations.SerializedName

//data class Poca(
//    val id: Int,
//    val address: String,
//    val content: String,
//    val img: String,
//    val latitude: Double,
//    val level: Int? = 1,
//    val longitude: Double,
//    val name: String,
//    val number: Int,
//    val representStatus: Int
//)

data class Poca(
    @SerializedName("id") val id: Int,
    @SerializedName("pack_id") val packId: Int,
    @SerializedName("poca_name") val pocaName: String,
    @SerializedName("poca_content") val pocaContent: String,
    @SerializedName("poca_number") val pocaNumber: Int,
    @SerializedName("poca_representstatus") val pocaRepresentStatus: Int,
    @SerializedName("poca_level") val pocaLevel: Int,
    @SerializedName("poca_img") val pocaImg: String,
    @SerializedName("location_id") val locationId: Int,
    @SerializedName("address") val address: String,
    @SerializedName("register_time") val registerTime: String,
    @SerializedName("update_time") val updateTime: String,
    //@SerializedName("locations") val locations: List<Location>,
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double
)


data class Location(
    @SerializedName("id") val id: Int,
    @SerializedName("address") val address: String,
    @SerializedName("latitude") val latitude: String,
    @SerializedName("longitude") val longitude: String,
    @SerializedName("register_time") val registerTime: String
)