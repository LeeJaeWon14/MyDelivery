package com.example.mydelivery

import com.example.mydelivery.dto.CarrierDTO
import com.example.mydelivery.dto.TrackerDTO
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

interface DeliveryService {
    @GET("carriers")
    fun getCarriers() : Call<List<CarrierDTO>>

    @GET("carriers/{company}/tracks/{delivery_number}")
    fun getTracker(
        @Path("company") company : String,
        @Path("delivery_number") deliveryNumber : String
    ) : Call<TrackerDTO>
}