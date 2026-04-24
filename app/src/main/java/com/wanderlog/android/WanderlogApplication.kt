package com.wanderlog.android

import android.app.Application
import com.google.android.libraries.places.api.Places
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class WanderlogApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Places SDK is initialised lazily in PlacesDataSource once the API key is available
    }
}
