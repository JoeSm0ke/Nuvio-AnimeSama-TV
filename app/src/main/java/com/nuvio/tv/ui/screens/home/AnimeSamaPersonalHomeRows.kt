package com.nuvio.tv.ui.screens.home

import com.nuvio.tv.domain.model.CatalogRow
import com.nuvio.tv.domain.model.ContentType
import com.nuvio.tv.domain.model.LibraryEntry
import com.nuvio.tv.domain.model.MetaPreview

/**
 * A read-only home projection of the existing library. It does not store,
 * change or duplicate favorites or episode progress.
 */
internal object AnimeSamaPersonalHomeRows {
    const val CATALOG_ID = "as_personal_list"
    private const val MAX_HOME_ITEMS = 18
    private const val ADDON_ID = "fr.joesm0ke.animesama.personal"

    /** Never navigate to non-resolvable synthetic IDs or non-series entries. */
    private val supportedContentId = Regex("^(tt[0-9]{5,15}|tmdb:[0-9]+)$")

    fun isPersonalShelf(row: CatalogRow): Boolean =
        row.catalogId == CATALOG_ID && row.addonId == ADDON_ID

    /** Synthetic favorites have no pageable See All endpoint. */
    fun canShowSeeAll(row: CatalogRow): Boolean =
        !isPersonalShelf(row) && (row.hasMore || row.items.size >= 15)

    fun isEligibleEntry(entry: LibraryEntry): Boolean =
        entry.type.equals("series", ignoreCase = true) &&
            supportedContentId.matches(entry.id) &&
            entry.name.isNotBlank()

    fun build(entries: List<LibraryEntry>): CatalogRow? {
        val items = entries.asSequence()
            .filter(::isEligibleEntry)
            .sortedByDescending { it.listedAt }
            .distinctBy { it.id }
            .take(MAX_HOME_ITEMS)
            .map(LibraryEntry::toMetaPreview)
            .toList()
        return buildFromPreviews(items)
    }

    fun buildFromPreviews(items: List<MetaPreview>): CatalogRow? {
        if (items.isEmpty()) return null
        return CatalogRow(
            addonId = ADDON_ID,
            addonName = "Anime-Sama",
            // Empty URL delegates detail lookup to the installed metadata addons.
            addonBaseUrl = "",
            catalogId = CATALOG_ID,
            catalogName = "Ma liste",
            type = ContentType.fromString("series"),
            rawType = "series",
            items = items,
            hasMore = false,
            supportsSkip = false
        )
    }

    /** Idempotent: catalog refresh cannot duplicate the personal shelf. */
    fun prependPersonalShelf(rows: List<HomeRow>, shelf: CatalogRow?): List<HomeRow> {
        val withoutOld = rows.filterNot {
            it is HomeRow.Catalog && isPersonalShelf(it.row)
        }
        return if (shelf == null) withoutOld else listOf(HomeRow.Catalog(shelf)) + withoutOld
    }
}
