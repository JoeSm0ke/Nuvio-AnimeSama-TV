package com.nuvio.tv.core.player

import com.nuvio.tv.domain.model.Stream
import com.nuvio.tv.domain.model.StreamBehaviorHints
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AnimeSamaPreferredStreamTest {
    private fun stream(language: String, resolution: Int): Stream = Stream(
        name = "Anime-Sama (${language.uppercase()})",
        title = "Episode 1 - ${language.uppercase()} [${resolution}p]",
        description = null,
        url = "https://example.org/${language}-${resolution}.m3u8",
        ytId = null,
        infoHash = null,
        fileIdx = null,
        externalUrl = null,
        behaviorHints = StreamBehaviorHints(
            notWebReady = null,
            bingeGroup = "anime-sama-language:$language",
            countryWhitelist = null,
            proxyHeaders = null
        ),
        addonName = "Anime-Sama TV - Qilive",
        addonLogo = null,
        quality = "${resolution}p",
        qualityValue = resolution
    )

    @Test fun episodeChoosesVf1080NotFirstItem() {
        val vostfr1080 = stream("vostfr", 1080)
        val vf720 = stream("vf", 720)
        val vf1080 = stream("vf", 1080)
        assertEquals(vf1080, AnimeSamaPreferredStream.choose(
            listOf(vostfr1080, vf720, vf1080), false
        ))
    }

    @Test fun episodeWithoutVf1080ShowsChoices() {
        assertNull(AnimeSamaPreferredStream.choose(listOf(
            stream("vf", 720), stream("vostfr", 1080)
        ), false))
        assertNull(AnimeSamaPreferredStream.choose(listOf(
            stream("vostfr", 1080)
        ), false))
    }

    @Test fun resumeAutoselectsBestVfEvenWithout1080() {
        val vostfr1080 = stream("vostfr", 1080)
        val vf720 = stream("vf", 720)
        assertEquals(vf720, AnimeSamaPreferredStream.choose(
            listOf(vostfr1080, stream("vf", 360), vf720), true
        ))
    }

    @Test fun resumeFallsBackToBestAvailableWhenNoVf() {
        val vostfr1080 = stream("vostfr", 1080)
        assertEquals(vostfr1080, AnimeSamaPreferredStream.choose(
            listOf(stream("vostfr", 360), vostfr1080), true
        ))
    }

    @Test fun episodeNeverSelectsUnavailableSource() {
        assertNull(AnimeSamaPreferredStream.choose(emptyList(), true))
        val external = stream("vf", 1080).copy(url = null, externalUrl = "https://example.org/page")
        assertNull(AnimeSamaPreferredStream.choose(listOf(external), false))
    }
}
