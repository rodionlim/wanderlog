package com.wanderlog.android.core.di

import android.content.pm.PackageManager
import android.content.Context
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.net.PlacesClient
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.wanderlog.android.data.remote.openai.OpenAiService
import com.wanderlog.android.presentation.settings.SettingsViewModel
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideMoshi(): Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    @Provides
    @Singleton
    fun provideOkHttpClient(apiKeyInterceptor: com.wanderlog.android.data.remote.openai.ApiKeyInterceptor): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(apiKeyInterceptor)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            })
            .build()

    @Provides
    @Singleton
    fun provideOpenAiService(okHttpClient: OkHttpClient, moshi: Moshi): OpenAiService =
        Retrofit.Builder()
            .baseUrl("https://api.openai.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(OpenAiService::class.java)

    @Provides
    @Singleton
    fun providePlacesClient(@ApplicationContext context: Context): PlacesClient {
        val runtimeKey = SettingsViewModel.getMapsKey(context).trim()
        val manifestKey = runCatching {
            val appInfo = context.packageManager.getApplicationInfo(
                context.packageName,
                PackageManager.GET_META_DATA
            )
            appInfo.metaData?.getString("com.google.android.geo.API_KEY").orEmpty()
        }.getOrDefault("").trim()
        val apiKey = runtimeKey.ifBlank { manifestKey }

        require(apiKey.isNotBlank()) {
            "Google Maps API key is required before using Places. Set it in Settings or local.properties."
        }

        if (!Places.isInitialized()) {
            Places.initialize(context.applicationContext, apiKey)
        }
        return Places.createClient(context)
    }
}
