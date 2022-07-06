package com.owenlejeune.whosinspace.api.model

import com.owenlejeune.whosinspace.api.OpenNotifyApi
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class OpenNotifyService {

    private val retrofit = Retrofit.Builder()
        .baseUrl("http://api.open-notify.org/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    fun createService(): OpenNotifyApi {
        return retrofit.create(OpenNotifyApi::class.java)
    }

}