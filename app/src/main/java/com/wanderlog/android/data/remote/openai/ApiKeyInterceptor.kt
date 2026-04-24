package com.wanderlog.android.data.remote.openai

import android.content.Context
import com.wanderlog.android.presentation.settings.SettingsViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class ApiKeyInterceptor @Inject constructor(
    @ApplicationContext private val context: Context
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val key = SettingsViewModel.getOpenAiKey(context)
        val request = if (key.isNotBlank()) {
            chain.request().newBuilder()
                .header("Authorization", "Bearer $key")
                .build()
        } else {
            chain.request()
        }
        return chain.proceed(request)
    }
}
