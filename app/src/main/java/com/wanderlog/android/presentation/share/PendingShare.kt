package com.wanderlog.android.presentation.share

/**
 * Ephemeral holder for text received via ACTION_SEND. Populated by MainActivity, consumed
 * (once) by ShareImportScreen. Lives outside nav args because shared email bodies can
 * exceed intent URI length limits.
 */
object PendingShare {
    var text: String? = null
}
