package com.nuvio.tv.core.player

/**
 * Anime-Sama video hosts can return short-lived signed media URLs. Their
 * stream links must be resolved anew instead of replaying a persisted URL
 * several hours later. This does not touch watch progress, binge-group
 * preference, the stream discovery session, or other providers' links.
 */
object AnimeSamaTransientLinkPolicy {
    private val sourceName = Regex("^anime[- ]sama(?:\\s|\\()", RegexOption.IGNORE_CASE)
    private const val BINGE_PREFIX = "anime-sama-language:"

    fun isVolatileHttpLink(
        streamName: String?,
        bingeGroup: String?,
        url: String?
    ): Boolean {
        if (url.isNullOrBlank() || !url.startsWith("http", ignoreCase = true)) return false
        return (bingeGroup?.startsWith(BINGE_PREFIX) == true) ||
            (streamName?.trim()?.let { sourceName.containsMatchIn(it) } == true)
    }
}
