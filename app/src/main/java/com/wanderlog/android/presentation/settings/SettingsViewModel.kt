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
const val KEY_OPENAI_MODEL = "openai_model"

data class OpenAiModelOption(
    val id: String,
    val label: String,
    val usageTier: String
)

object OpenAiModels {
    const val DEFAULT_MODEL = "gpt-5.4-mini"

    val options = listOf(
        OpenAiModelOption("gpt-5.4-mini", "GPT-5.4 Mini", "2.5M tokens/day tier"),
        OpenAiModelOption("gpt-5.4", "GPT-5.4", "250K tokens/day tier"),
        OpenAiModelOption("gpt-5.5", "GPT-5.5", "Model availability depends on your account"),
        OpenAiModelOption("gpt-5.4-nano", "GPT-5.4 Nano", "2.5M tokens/day tier"),
        OpenAiModelOption("gpt-5.2", "GPT-5.2", "250K tokens/day tier"),
        OpenAiModelOption("gpt-5.1", "GPT-5.1", "250K tokens/day tier"),
        OpenAiModelOption("gpt-5.1-codex", "GPT-5.1 Codex", "250K tokens/day tier"),
        OpenAiModelOption("gpt-5.1-codex-mini", "GPT-5.1 Codex Mini", "2.5M tokens/day tier"),
        OpenAiModelOption("gpt-5", "GPT-5", "250K tokens/day tier"),
        OpenAiModelOption("gpt-5-codex", "GPT-5 Codex", "250K tokens/day tier"),
        OpenAiModelOption("gpt-5-chat-latest", "GPT-5 Chat Latest", "250K tokens/day tier"),
        OpenAiModelOption("gpt-5-mini", "GPT-5 Mini", "2.5M tokens/day tier"),
        OpenAiModelOption("gpt-5-nano", "GPT-5 Nano", "2.5M tokens/day tier"),
        OpenAiModelOption("gpt-4.1", "GPT-4.1", "250K tokens/day tier"),
        OpenAiModelOption("gpt-4.1-mini", "GPT-4.1 Mini", "2.5M tokens/day tier"),
        OpenAiModelOption("gpt-4.1-nano", "GPT-4.1 Nano", "2.5M tokens/day tier"),
        OpenAiModelOption("gpt-4o", "GPT-4o", "250K tokens/day tier"),
        OpenAiModelOption("gpt-4o-mini", "GPT-4o Mini", "2.5M tokens/day tier"),
        OpenAiModelOption("o1", "o1", "250K tokens/day tier"),
        OpenAiModelOption("o1-mini", "o1 Mini", "2.5M tokens/day tier"),
        OpenAiModelOption("o3", "o3", "250K tokens/day tier"),
        OpenAiModelOption("o3-mini", "o3 Mini", "2.5M tokens/day tier"),
        OpenAiModelOption("o4-mini", "o4 Mini", "2.5M tokens/day tier"),
        OpenAiModelOption("codex-mini-latest", "Codex Mini Latest", "2.5M tokens/day tier")
    )

    private val validIds = options.mapTo(linkedSetOf()) { it.id }

    fun sanitize(model: String?): String {
        val candidate = model?.trim().orEmpty()
        return if (candidate in validIds) candidate else DEFAULT_MODEL
    }

    fun labelFor(model: String): String =
        options.firstOrNull { it.id == model }?.label ?: DEFAULT_MODEL
}

data class SettingsState(
    val openAiKey: String = "",
    val mapsKey: String = "",
    val openAiModel: String = OpenAiModels.DEFAULT_MODEL,
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
                mapsKey = prefs.getString(KEY_MAPS_API_KEY, "") ?: "",
                openAiModel = OpenAiModels.sanitize(prefs.getString(KEY_OPENAI_MODEL, OpenAiModels.DEFAULT_MODEL))
            )
        }
    }

    fun onOpenAiKeyChange(v: String) = _state.update { it.copy(openAiKey = v) }
    fun onMapsKeyChange(v: String) = _state.update { it.copy(mapsKey = v) }
    fun onOpenAiModelChange(v: String) = _state.update { it.copy(openAiModel = OpenAiModels.sanitize(v)) }

    fun save() {
        prefs.edit()
            .putString(KEY_OPENAI_API_KEY, _state.value.openAiKey.trim())
            .putString(KEY_MAPS_API_KEY, _state.value.mapsKey.trim())
            .putString(KEY_OPENAI_MODEL, OpenAiModels.sanitize(_state.value.openAiModel))
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

        fun getOpenAiModel(context: Context): String {
            val masterKey = MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
            val prefs = EncryptedSharedPreferences.create(context, PREFS_NAME, masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM)
            return OpenAiModels.sanitize(prefs.getString(KEY_OPENAI_MODEL, OpenAiModels.DEFAULT_MODEL))
        }
    }
}
