package com.example.finanzas_independientes_app.data.remote

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    // BORRA LA LÍNEA ANTIGUA:
    private const val BASE_URL = "http://10.0.2.2:8080/"

    // PON TU NUEVA URL DE PRODUCCIÓN:
    // private const val BASE_URL = "https://businesscontrol.azurewebsites.net/"

    val apiService: FinanzasApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(FinanzasApi::class.java)
    }
}