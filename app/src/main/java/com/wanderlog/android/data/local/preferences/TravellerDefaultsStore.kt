package com.wanderlog.android.data.local.preferences

import android.content.Context
import com.wanderlog.android.core.util.TravellerProfilesCodec
import com.wanderlog.android.domain.model.TravellerProfile
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TravellerDefaultsStore @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getTravellerProfiles(): List<TravellerProfile> =
        TravellerProfilesCodec.decode(prefs.getString(KEY_DEFAULT_TRAVELLERS, null))

    fun saveTravellerProfiles(profiles: List<TravellerProfile>) {
        prefs.edit()
            .putString(KEY_DEFAULT_TRAVELLERS, TravellerProfilesCodec.encode(profiles))
            .apply()
    }

    private companion object {
        const val PREFS_NAME = "wanderlog_trip_defaults"
        const val KEY_DEFAULT_TRAVELLERS = "default_travellers"
    }
}
