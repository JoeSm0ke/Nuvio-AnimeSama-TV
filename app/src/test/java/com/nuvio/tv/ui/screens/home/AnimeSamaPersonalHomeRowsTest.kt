package com.nuvio.tv.ui.screens.home

import com.nuvio.tv.domain.model.LibraryEntry
import org.junit.Assert.*
import org.junit.Test

class AnimeSamaPersonalHomeRowsTest {
    private fun entry(
        id: String,
        type: String = "series",
        listedAt: Long = 0L,
        name: String = "Anime title"
    ) = LibraryEntry(
        id = id,
        type = type,
        name = name,
        poster = null,
        background = null,
        logo = null,
        description = null,
        releaseInfo = null,
        imdbRating = null,
        genres = emptyList(),
        addonBaseUrl = null,
        listedAt = listedAt,
        mediaCategory = "anime"
    )

    @Test fun personalShelfFiltersInvalidIdsAndMovies() {
        val row = AnimeSamaPersonalHomeRows.build(
            listOf(entry("tt0409591"), entry("xx:unplayable"),
                entry("tt0100000", type = "movie"), entry("tmdb:12345"))
        )
        assertEquals("Ma liste", row?.catalogName)
        assertEquals(listOf("tt0409591", "tmdb:12345"), row?.items?.map { it.id })
        assertFalse(row!!.hasMore)
    }

    @Test fun personalShelfShowsMostRecentFirstAndNoDuplicates() {
        val row = AnimeSamaPersonalHomeRows.build(
            listOf(entry("tt0409591", listedAt = 1),
                   entry("tt0409591", listedAt = 8),
                   entry("tt30217403", listedAt = 5))
        )
        assertEquals(listOf("tt0409591", "tt30217403"), row?.items?.map { it.id })
        assertEquals("Ma liste", row?.catalogName)
    }

    @Test fun emptyShelfIsHidden() {
        assertNull(AnimeSamaPersonalHomeRows.build(emptyList()))
        assertNull(AnimeSamaPersonalHomeRows.build(listOf(entry("invalid"))))
    }

    @Test fun insertIsIdempotentAndRemovalKeepsCatalogs() {
        val personal = AnimeSamaPersonalHomeRows.build(listOf(entry("tt0409591")))!!
        val shelf = HomeRow.Catalog(personal)
        val rows = listOf<HomeRow>(HomeRow.Catalog(personal))
        val merged = AnimeSamaPersonalHomeRows.prependPersonalShelf(rows, personal)
        assertEquals(listOf(shelf), merged)
        assertTrue(AnimeSamaPersonalHomeRows.prependPersonalShelf(merged, null).isEmpty())
    }
}
