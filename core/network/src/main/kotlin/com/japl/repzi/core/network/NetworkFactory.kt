package com.japl.repzi.core.network

import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

internal const val WGER_BASE_URL = "https://wger.de/api/v2/"

/** Plain OkHttp client: a generic `User-Agent` and short timeouts, nothing that logs bodies. */
internal fun createHttpClient(): OkHttpClient = OkHttpClient.Builder()
    .addInterceptor { chain ->
        chain.proceed(chain.request().newBuilder().header("User-Agent", "Repzi").build())
    }
    .connectTimeout(30, TimeUnit.SECONDS)
    .readTimeout(30, TimeUnit.SECONDS)
    .build()

internal fun createWgerApi(baseUrl: String, client: OkHttpClient): WgerApi {
    val json = Json { ignoreUnknownKeys = true }
    return Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(WgerApi::class.java)
}
