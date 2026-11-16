package com.example.andriod.data.api

import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface OpenFdaApi {
    @GET("drug/label.json")
    suspend fun searchDrugLabel(
        @Query("search", encoded = true) search: String,
        @Query("limit") limit: Int = 1
    ): Response<OpenFdaResponse>

    companion object {
        private const val BASE_URL = "https://api.fda.gov/"

        fun create(): OpenFdaApi {
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(OpenFdaApi::class.java)
        }
    }
}
