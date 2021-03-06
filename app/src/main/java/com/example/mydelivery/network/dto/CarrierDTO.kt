package com.example.mydelivery.network.dto

import com.google.gson.annotations.SerializedName

data class CarrierDTO(
    @SerializedName("id")
    var id: String = "",

    @SerializedName("name")
    var name: String = "",

    @SerializedName("tel")
    var tel: String = ""
) {
    override fun toString(): String {
        return "CarriersDTO(id='$id', name='$name', tel='$tel')"
    }
}