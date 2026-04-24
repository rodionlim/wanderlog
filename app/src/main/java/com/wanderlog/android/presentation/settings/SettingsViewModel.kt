package com.wanderlog.android.presentation.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

private const val PREFS_NAME = "wanderlog_secure"
const val KEY_OPENAI_API_KEY = "openai_api_key"
const val KEY_MAPS_API_KEY = "maps_api_key"

data class SettingsState(
    val openAiKey: String = "",
    val mapsKey: String = "",
    val saved: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val prefs by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    init {
        _state.update {
            it.copy(
                openAiKey = prefs.getString(KEY_OPENAI_API_KEY, "") ?: "",
                mapsKey = prefs.getString(KEY_MAPS_API_KEY, "") ?: ""
            )
        }
    }

    fun onOpenAiKeyChange(v: String) = _state.update { it.copy(openAiKey = v) }
    fun onMapsKeyChange(v: String) = _state.update { it.copy(mapsKey = v) }

    fun save() {
        prefs.edit()
            .putString(KEY_OPENAI_API_KEY, _state.value.openAiKey.trim())
            .putString(KEY_MAPS_API_KEY, _state.value.mapsKey.trim())
            .apply()
        _state.update { it.copy(saved = true) }
    }

    companion object {
        fun getOpenAiKey(context: Context): String {
            val masterKey = MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
            val prefs = EncryptedSharedPreferences.create(context, PREFS_NAME, masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM)
            return prefs.getString(KEY_OPENAI_API_KEY, "") ?: ""
        }
    }
}
