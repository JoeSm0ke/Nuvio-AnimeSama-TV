package com.nuvio.tv.core.player

import com.nuvio.tv.domain.model.Stream

/**
 * TV fork playback defaults: choose VF 1080p automatically where available.
 * Episode selection otherwise keeps the source picker visible. Home resume
 * always picks the best available direct source, preferring French audio.
 */
object AnimeSamaPreferredStream {
    private val vfWord = Regex("(?<![A-Z0-9])VF(?![A-Z0-9])")
    private val vostfrWord = Regex("(?<![A-Z0-9])VOSTFR(?![A-Z0-9])")
    private val resolution = Regex("(?<![0-9])(2160|1440|1080|720|576|480|360)(?:[pP])?(?![0-9])")

    private fun language(stream: Stream): String {
        val group = stream.behaviorHints?.bingeGroup.orEmpty().lowercase()
        if (group == "anime-sama-language:vf") return "vf"
        if (group == "anime-sama-language:vostfr") return "vostfr"
        val name = listOfNotNull(stream.name, stream.title).joinToString(" ").uppercase()
        return when {
            vostfrWord.containsMatchIn(name) -> "vostfr"
            vfWord.containsMatchIn(name) -> "vf"
            else -> "unknown"
        }
    }

    private fun quality(stream: Stream): Int {
        if (stream.qualityValue >= 360) return stream.qualityValue
        val label = listOfNotNull(stream.quality, stream.name, stream.title).joinToString(" ")
        return resolution.find(label)?.groupValues?.get(1)?.toIntOrNull() ?: -1
    }

    fun choose(streams: List<Stream>, resumeFromHome: Boolean): Stream? {
        val playable = streams.filter {
            !it.isExternal() && it.getStreamUrl()?.startsWith("https://", ignoreCase = true) == true
        }
        val preferred = playable.filter { language(it) == "vf" && quality(it) == 1080 }
        if (preferred.isNotEmpty()) return preferred.first()
        if (!resumeFromHome) return null
        return playable.sortedWith(
            compareByDescending<Stream> { language(it) == "vf" }
                .thenByDescending { quality(it) }
        ).firstOrNull()
    }
}
