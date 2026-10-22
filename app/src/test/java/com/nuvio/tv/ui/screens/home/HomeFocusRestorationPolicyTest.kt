package com.nuvio.tv.ui.screens.home

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeFocusRestorationPolicyTest {
    private val catalog = "addon_series_classics"
    private val personal = "fr.joesm0ke.animesama.personal_series_as_personal_list"

    @Test fun modernWaitsForSavedCatalogInsteadOfStealingFocusWithPersonalRow() {
        assertTrue(HomeFocusRestorationPolicy.shouldWaitForSavedRow(
            catalog, setOf(personal), catalogsLoading = true
        ))
        assertFalse(HomeFocusRestorationPolicy.shouldWaitForSavedRow(
            catalog, setOf(personal, catalog), catalogsLoading = true
        ))
    }

    @Test fun modernAllowsFallbackOnceLoadingFinishes() {
        assertFalse(HomeFocusRestorationPolicy.shouldWaitForSavedRow(
            catalog, setOf(personal), catalogsLoading = false
        ))
        assertFalse(HomeFocusRestorationPolicy.shouldWaitForSavedRow(
            null, setOf(personal), catalogsLoading = true
        ))
    }

    @Test fun classicPrioritizesRowIdentityAfterPersonalRowWasInserted() {
        val available = setOf(personal, catalog)
        assertFalse(HomeFocusRestorationPolicy.shouldRestoreClassicRow(
            true, catalog, 0, personal, 0, available, false
        ))
        assertTrue(HomeFocusRestorationPolicy.shouldRestoreClassicRow(
            true, catalog, 0, catalog, 1, available, false
        ))
    }

    @Test fun classicUsesLegacyIndexWhenThereWasNoStableKey() {
        assertTrue(HomeFocusRestorationPolicy.shouldRestoreClassicRow(
            true, null, 1, catalog, 1, setOf(catalog), false
        ))
        assertFalse(HomeFocusRestorationPolicy.shouldRestoreClassicRow(
            true, null, 1, catalog, 0, setOf(catalog), false
        ))
    }

    @Test fun classicDefersAbsentSavedRowWhileCatalogsLoad() {
        assertFalse(HomeFocusRestorationPolicy.shouldRestoreClassicRow(
            true, catalog, 0, personal, 0, setOf(personal), true
        ))
        assertTrue(HomeFocusRestorationPolicy.shouldRestoreClassicRow(
            true, catalog, 0, personal, 0, setOf(personal), false
        ))
    }

    @Test fun classicDoesNotRestoreWithoutSavedFocus() {
        assertFalse(HomeFocusRestorationPolicy.shouldRestoreClassicRow(
            false, catalog, 0, catalog, 0, setOf(catalog), false
        ))
    }
}
