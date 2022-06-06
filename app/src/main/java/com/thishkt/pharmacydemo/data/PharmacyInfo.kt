package com.thishkt.pharmacydemo.data

//可定義自己要的名稱 Serializable
import java.io.Serializable

data class PharmacyInfo(
    val features: List<Feature>,
    val type: String
    //type與JSON資料欄位一樣，才能抓到資料
):Serializable


data class Feature(
    val geometry: Geometry,
    val properties: Properties,
    val type: String
):Serializable

data class Geometry(
    val coordinates: List<Double>,
    val type: String
):Serializable

data class Properties(
    val address: String,
    val available: String,
    val county: String,
    val cunli: String,
    val custom_note: String,
    val id: String,
    val mask_adult: Int,
    val mask_child: Int,
    val name: String,
    val note: String,
    val phone: String,
    val service_periods: String,
    val town: String,
    val updated: String,
    val website: String
):Serializable