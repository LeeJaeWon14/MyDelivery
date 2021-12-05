package com.example.mydelivery.network

import com.example.mydelivery.network.model.NetworkConstants
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetroClient {
    private var instance: Retrofit? = null
    fun getInstance() : Retrofit {
        instance?.let {
            return it
        } ?: run {
            instance = Retrofit.Builder()
                .baseUrl(NetworkConstants.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            return instance!!
        }
    }
}