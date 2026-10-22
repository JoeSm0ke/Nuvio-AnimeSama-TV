package com.nuvio.tv.data.repository

import com.nuvio.tv.domain.model.LocalScraperResult
import java.util.Locale

/**
 * The plugin runtime exposes the provider's language label but does not carry
 * Stremio behaviorHints from its JavaScript result. Encode only the explicit
 * Anime-Sama VF/VOSTFR variant as a stable binge group for Nuvio's existing
 * "reuse same source group for next episode" logic.
 *
 * Unlabeled or ambiguous links intentionally have no language group: never
 * assume that generic "fr" means French audio rather than French subtitles.
 */
internal object AnimeSamaLanguageContinuity {
    private const val SCRAPER_ID = "anime-sama-qilive"
    private const val GROUP_PREFIX = "anime-sama-language:"
    private val vostfr = Regex("(?<![A-Z0-9])VOSTFR(?![A-Z0-9])")
    private val vf = Regex("(?<![A-Z0-9])VF(?![A-Z0-9])")

    fun bingeGroup(scraperId: String, result: LocalScraperResult): String? {
        if (scraperId != SCRAPER_ID) return null
        val explicitLabel = listOf(result.name, result.title, result.language)
            .filterNotNull()
            .joinToString(" ")
            .uppercase(Locale.ROOT)
        return when {
            vostfr.containsMatchIn(explicitLabel) -> GROUP_PREFIX + "vostfr"
            vf.containsMatchIn(explicitLabel) -> GROUP_PREFIX + "vf"
            else -> null
        }
    }
}
