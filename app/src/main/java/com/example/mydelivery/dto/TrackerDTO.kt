package com.example.mydelivery.dto

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class TrackerDTO(
    @SerializedName("from")
    @Expose
    var from: InfoDTO ,

    @SerializedName("to")
    var to: InfoDTO,

    @SerializedName("state")
    @Expose
    var state: StateDTO,

    @SerializedName("progresses")
    @Expose
    var progresses: List<ProgressDTO>,

    @SerializedName("carrier")
    @Expose
    var carrier: CarrierDTO
) {
    override fun toString(): String {
        return "TrackerDTO(from=$from, to=$to, state=$state, progresses=$progresses, carrier=$carrier)"
    }
}

data class InfoDTO(
    @SerializedName("name")
    var name: String = "",

    @SerializedName("time")
    var time: String = ""
)

data class StateDTO(
    @SerializedName("id")
    var id: String = "",

    @SerializedName("text")
    var text: String = ""
)

data class LocationDTO(@SerializedName("name") var name: String = "")

data class ProgressDTO(
    @SerializedName("time")
    var time: String = "",

    @SerializedName("status")
    @Expose
    var status: StateDTO,

    @SerializedName("location")
    @Expose
    var location: LocationDTO,

    @SerializedName("description")
    var description: String = ""
)