package com.soundgram.chipster.network

import android.content.Context
import android.text.TextUtils
import okhttp3.Credentials.basic
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory


object RestfulAdapter {

    val serviceApi: MarkerService
        get() {
            val loggingInterceptor = HttpLoggingInterceptor()
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY)
            val client: OkHttpClient =
                OkHttpClient.Builder().addInterceptor(loggingInterceptor).build()
            val retrofit = Retrofit.Builder()
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .baseUrl(BASE_URI)
                .build()
            return retrofit.create(MarkerService::class.java)
        }
    val simpleApi: MarkerService
        get() {
            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URI)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            return retrofit.create(MarkerService::class.java)
        }

    //    private static final String BASE_URI = "http://api.soundgram.co.kr:10080/";
    private const val BASE_URI = "http://devapi.soundgram.co.kr:10080/"
    private val httpClient: OkHttpClient.Builder = OkHttpClient.Builder()
    private val builder = Retrofit.Builder()
        .baseUrl(BASE_URI)
        .addConverterFactory(GsonConverterFactory.create())
        .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
    private var retrofit = builder.build()
    fun <S> createService(
        context: Context?,
        serviceClass: Class<S>?, username: String?, password: String?
    ): S {
        if (!TextUtils.isEmpty(username)
            && !TextUtils.isEmpty(password)
        ) {
            val authToken = basic(username!!, password!!)
            return createService(context, serviceClass, authToken)
        }
        return createService(context, serviceClass, null)
    }

    fun <S> createService(
        context: Context?,
        serviceClass: Class<S>?, authToken: String?
    ): S {
        if (!TextUtils.isEmpty(authToken)) {
            val interceptor = AuthenticationInterceptor(authToken!!)
            val loggingInterceptor = HttpLoggingInterceptor()
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY)
            if (!httpClient.interceptors().contains(interceptor)) {
                retrofit = builder.build()
            }
        }
        return retrofit.create(serviceClass)
    }
}
