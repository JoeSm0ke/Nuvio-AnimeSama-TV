package com.nuvio.tv.data.repository

import com.nuvio.tv.domain.model.LocalScraperResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AnimeSamaLanguageContinuityTest {
    private fun result(name: String, language: String? = "fr") = LocalScraperResult(
        name = name,
        title = "Stream for $name",
        url = "https://example.org/playlist.m3u8",
        language = language
    )

    @Test fun keepsVostfrSeparateFromFrenchDub() {
        assertEquals("anime-sama-language:vostfr",
            AnimeSamaLanguageContinuity.bingeGroup("anime-sama-qilive", result("Anime-Sama (VOSTFR)")))
        assertEquals("anime-sama-language:vf",
            AnimeSamaLanguageContinuity.bingeGroup("anime-sama-qilive", result("Anime-Sama (VF)")))
    }

    @Test fun doesNotGuessFrenchDubFromGenericFrenchLanguage() {
        assertNull(AnimeSamaLanguageContinuity.bingeGroup(
            "anime-sama-qilive", result("Anime-Sama", "fr")
        ))
    }

    @Test fun otherPluginsRetainTheirExistingBingeBehavior() {
        assertNull(AnimeSamaLanguageContinuity.bingeGroup(
            "other-plugin", result("Some Anime (VOSTFR)")
        ))
    }

    @Test fun noSubstringCollision() {
        assertNull(AnimeSamaLanguageContinuity.bingeGroup(
            "anime-sama-qilive", result("NEVFOR stream")
        ))
        assertEquals("anime-sama-language:vostfr",
            AnimeSamaLanguageContinuity.bingeGroup(
                "anime-sama-qilive", result("Host - Episode 1 - VOSTFR / VF")
            ))
    }
}
