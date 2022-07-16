package com.owenlejeune.whosinspace.api.model

import com.owenlejeune.whosinspace.api.NasaApi
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class NasaService {

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.nasa.gov/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    fun createService(): NasaApi {
        return retrofit.create(NasaApi::class.java)
    }

}