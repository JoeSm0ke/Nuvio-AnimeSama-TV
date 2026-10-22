package com.nuvio.tv.data.repository

import org.junit.Assert.*
import org.junit.Test

class AnimeSamaSearchMatcherTest {
    @Test fun punctuationAndSpacingAliases() {
        assertEquals(1, AnimeSamaSearchMatcher.rank("Dandadan", "Dan Da Dan"))
        assertEquals(0, AnimeSamaSearchMatcher.rank("Dandadan", "dandadan"))
        assertEquals(1, AnimeSamaSearchMatcher.rank("Spy X Family", "SpyXFamily"))
    }

    @Test fun frenchAccentAndRanking() {
        assertEquals(0, AnimeSamaSearchMatcher.rank("L'Été de Cloe", "l ete de cloe"))
        assertEquals(2, AnimeSamaSearchMatcher.rank("Naruto Shippuden", "naruto"))
        assertNull(AnimeSamaSearchMatcher.rank("Naruto", "Interstellar"))
    }

    @Test fun strictWhitelistDoesNotInferMissingTitles() {
        val known = setOf("dandadan", "naruto", "kaoru hana wa rin to saku", "kaoruhanawarintosaku")
        assertTrue(AnimeSamaSearchMatcher.verifiedNameMatches("Dan Da Dan", known))
        assertTrue(AnimeSamaSearchMatcher.verifiedNameMatches("Kaoru Hana Wa Rin to Saku", known))
        assertFalse(AnimeSamaSearchMatcher.verifiedNameMatches("Interstellar", known))
        assertFalse(AnimeSamaSearchMatcher.verifiedNameMatches("Naruto Shippuden", known))
    }

    @Test fun nonMatchingAndMultiword() {
        assertEquals(6, AnimeSamaSearchMatcher.rank("Reincarnated as a Sword", "sword reincarnated"))
        assertNull(AnimeSamaSearchMatcher.rank("Dragon Ball Z", "Dragon Ball Super"))
        assertNull(AnimeSamaSearchMatcher.rank("Dandadan", ""))
    }
}
