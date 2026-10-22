package com.nuvio.tv.core.player

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnimeSamaTransientLinkPolicyTest {
    @Test fun animeSamaSignedHlsIsNotReusable() {
        assertTrue(AnimeSamaTransientLinkPolicy.isVolatileHttpLink(
            "Anime-Sama (VF) - HD", null, "https://example.org/master.m3u8"
        ))
        assertTrue(AnimeSamaTransientLinkPolicy.isVolatileHttpLink(
            "Generic", "anime-sama-language:vostfr", "https://example.org/master.m3u8"
        ))
    }

    @Test fun oldCachedAnimeSamaLinksAreAlsoRecognized() {
        assertTrue(AnimeSamaTransientLinkPolicy.isVolatileHttpLink(
            "Anime-Sama (VOSTFR)", null, "https://example.org/old.mp4"
        ))
    }

    @Test fun otherProvidersAndPermanentLinksKeepExistingCache() {
        assertFalse(AnimeSamaTransientLinkPolicy.isVolatileHttpLink(
            "Other (VF)", null, "https://example.org/video.m3u8"
        ))
        assertFalse(AnimeSamaTransientLinkPolicy.isVolatileHttpLink(
            "The Anime-Sama Documentary", null, "https://example.org/video.mp4"
        ))
    }

    @Test fun torrentsAndMissingUrlsDoNotChangeCachePolicy() {
        assertFalse(AnimeSamaTransientLinkPolicy.isVolatileHttpLink(
            "Anime-Sama (VF)", "anime-sama-language:vf", ""
        ))
        assertFalse(AnimeSamaTransientLinkPolicy.isVolatileHttpLink(
            "Anime-Sama (VF)", "anime-sama-language:vf", "magnet:?xt=urn:btih:123"
        ))
    }
}
