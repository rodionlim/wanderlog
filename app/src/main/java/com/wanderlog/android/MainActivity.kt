package com.wanderlog.android

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.wanderlog.android.core.ui.theme.WanderlogTheme
import com.wanderlog.android.presentation.navigation.WanderlogNavGraph
import com.wanderlog.android.presentation.share.PendingShare
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val startAtShare = consumeShareIntent(intent)
        setContent {
            WanderlogTheme {
                WanderlogNavGraph(startAtShare = startAtShare)
            }
        }
    }

    private fun consumeShareIntent(intent: Intent?): Boolean {
        if (intent?.action != Intent.ACTION_SEND) return false
        val subject = intent.getStringExtra(Intent.EXTRA_SUBJECT).orEmpty()
        val body = intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
        val combined = listOf(subject, body).filter { it.isNotBlank() }.joinToString("\n\n")
        if (combined.isBlank()) return false
        PendingShare.text = combined
        return true
    }
}
