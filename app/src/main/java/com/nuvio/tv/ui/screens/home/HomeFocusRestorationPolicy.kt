package com.nuvio.tv.ui.screens.home

/**
 * A saved row identity is stronger than its previous numeric position.
 * Personalized rows and asynchronous catalogs can be inserted above it.
 */
internal object HomeFocusRestorationPolicy {
    /**
     * In the modern home, wait for a missing saved row while catalogs are
     * loading. Once loading finishes, use the first row as a safe fallback.
     */
    fun shouldWaitForSavedRow(
        savedRowKey: String?,
        availableRowKeys: Set<String>,
        catalogsLoading: Boolean
    ): Boolean = !savedRowKey.isNullOrBlank() &&
        savedRowKey !in availableRowKeys &&
        catalogsLoading

    /**
     * In the classic home, prefer the stable key whenever it exists.
     * The legacy index is used only if the saved key is unavailable after
     * loading or older stored focus data lacks a key.
     */
    fun shouldRestoreClassicRow(
        restoring: Boolean,
        savedRowKey: String?,
        savedRowIndex: Int,
        currentRowKey: String,
        currentRowIndex: Int,
        availableRowKeys: Set<String>,
        catalogsLoading: Boolean
    ): Boolean {
        if (!restoring) return false
        if (!savedRowKey.isNullOrBlank()) {
            if (savedRowKey in availableRowKeys) return savedRowKey == currentRowKey
            if (catalogsLoading) return false
        }
        return savedRowIndex == currentRowIndex
    }
}
