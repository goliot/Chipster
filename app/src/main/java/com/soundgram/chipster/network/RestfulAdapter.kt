package com.soundgram.chipster.network

import android.content.Context
import android.text.TextUtils
import okhttp3.Credentials.basic
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit


object RestfulAdapter {

    private const val BASE_URI = "https://chipsterplay.soundgram.co.kr/"
    private val builder = Retrofit.Builder()
        .baseUrl(BASE_URI)
        .client(
            OkHttpClient.Builder()
                .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS).writeTimeout(30, TimeUnit.SECONDS)
                .build()
        )
        .addConverterFactory(GsonConverterFactory.create())
        .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
    private val retrofit: Retrofit = builder.build()
    val chipsterService = retrofit.create(ChipsterService::class.java)

}
