package com.makita.etiquetadopdf417

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


object RetrofitClient {

   // private const val BASE_URL = "http://172.16.1.234:3024/"  // Reemplaza con tu dirección IP o dominio Local
    private const val BASE_URL = "http://172.16.1.206:3024/"  // Produccion
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }


}