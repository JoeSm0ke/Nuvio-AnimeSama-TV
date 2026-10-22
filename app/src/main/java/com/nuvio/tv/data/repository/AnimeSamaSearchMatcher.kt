package com.nuvio.tv.data.repository

import java.text.Normalizer
import java.util.Locale

/**
 * Safe and deterministic local matcher for the verified Anime-Sama catalog.
 * Never invents a metadata ID: callers display only entries with usable IDs.
 */
internal object AnimeSamaSearchMatcher {
    private val combiningMarks = Regex("\\p{M}+")
    private val separators = Regex("[^a-z0-9]+")

    fun normalize(value: String): String =
        Normalizer.normalize(value, Normalizer.Form.NFD)
            .replace(combiningMarks, "")
            .lowercase(Locale.ROOT)
            .replace(separators, " ")
            .trim()

    fun compact(value: String): String = normalize(value).replace(" ", "")

    /** Strict whitelist matching; punctuation differences are accepted, not fuzzy titles. */
    fun verifiedNameMatches(name: String, verified: Set<String>): Boolean {
        val key = normalize(name)
        if (key.isEmpty()) return false
        return key in verified || (key.length >= 5 && key.replace(" ", "") in verified)
    }

    /** Lower rank is more relevant. Null means no plausible title match. */
    fun rank(title: String, query: String): Int? {
        val name = normalize(title)
        val q = normalize(query)
        if (q.isEmpty() || name.isEmpty()) return null
        val compactName = name.replace(" ", "")
        val compactQuery = q.replace(" ", "")
        if (name == q) return 0
        if (compactQuery.length >= 4 && compactName == compactQuery) return 1
        if (name.startsWith(q)) return 2
        if (compactQuery.length >= 4 && compactName.startsWith(compactQuery)) return 3
        if (name.contains(q)) return 4
        if (compactQuery.length >= 4 && compactName.contains(compactQuery)) return 5

        val words = q.split(" ").filter { it.isNotBlank() }
        if (words.size >= 2) {
            val available = name.split(" ").filterTo(mutableListOf()) { it.isNotBlank() }
            if (words.all { word ->
                    val at = available.indexOfFirst { it.startsWith(word) }
                    if (at >= 0) available.removeAt(at)
                    at >= 0
                }) return 6
        }
        return null
    }
}
