package com.example.di

import android.content.Context
import androidx.room.Room
import com.example.data.local.AppDatabase
import com.example.data.local.daos.EcoTrackDao
import com.example.data.remote.GeminiApiService
import com.example.data.remote.PerformanceTracer
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Enterprise-grade clean Application Container managing Singletons.
 * Provides constructor-injectable components conforming to SOLID principles.
 */
class AppContainer(context: Context) {

    // 1. Thread-safe Local Room database
    val database: AppDatabase by lazy {
        Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "ecotrack_ai_database"
        )
        .fallbackToDestructiveMigration() // production fallback or migrations
        .build()
    }

    val ecoTrackDao: EcoTrackDao by lazy {
        database.ecoTrackDao()
    }

    // 2. Moshi Json Parser
    private val moshi: Moshi by lazy {
        Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    // 3. OkHttp Client with robust timeouts (60s as recommended for generative AI latency)
    private val okHttpClient: OkHttpClient by lazy {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(loggingInterceptor)
            .addInterceptor(PerformanceTracer.TelemetryInterceptor())
            .build()
    }

    // 4. Retrofit Client
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://generativelanguage.googleapis.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    private val geminiApiService: GeminiApiService by lazy {
        retrofit.create(GeminiApiService::class.java)
    }

    // 5. Consolidated Offline-First Repository
    val repository: com.example.data.repository.EcoTrackRepository by lazy {
        com.example.data.repository.EcoTrackRepository(
            ecoTrackDao = ecoTrackDao,
            geminiApiService = geminiApiService
        )
    }
}
