package com.nuvio.tv.data.repository

import android.content.Context
import android.util.Log
import com.nuvio.tv.core.network.NetworkResult
import com.nuvio.tv.core.network.safeApiCall
import com.nuvio.tv.data.mapper.toDomainOrNull
import com.nuvio.tv.data.remote.dto.CatalogResponseDto
import com.nuvio.tv.data.remote.api.AddonApi
import com.nuvio.tv.domain.model.CatalogRow
import com.nuvio.tv.domain.model.ContentType
import com.nuvio.tv.domain.repository.CatalogRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.net.URLEncoder
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CatalogRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: AddonApi
) : CatalogRepository {
    companion object {
        private const val TAG = "CatalogRepository"
        private const val ANIME_TITLES_INDEX =
            "https://joesm0ke.github.io/Nuvio-AnimeSama-Qilive/catalog/series/as_names.json"
        private const val ANIME_SEARCH_CACHE_MS = 20L * 60 * 1000
    }

    private val animeIndexMutex = Mutex()
    @Volatile private var knownAnimeNames: Set<String>? = null
    private val animeSearchMutex = Mutex()
    @Volatile private var cachedAnimeSearch: CatalogResponseDto? = null
    @Volatile private var cachedAnimeSearchAtMs = 0L

    /** Cache the static JSON index once per session window, not once per keystroke. */
    private suspend fun getAnimeSearchSnapshot(url: String): NetworkResult<CatalogResponseDto> =
        animeSearchMutex.withLock {
            val cached = cachedAnimeSearch
            val now = System.currentTimeMillis()
            if (cached != null && now - cachedAnimeSearchAtMs < ANIME_SEARCH_CACHE_MS) {
                return@withLock NetworkResult.Success(cached)
            }
            val fetched = safeApiCall(context) { api.getCatalog(url) }
            if (fetched is NetworkResult.Success) {
                cachedAnimeSearch = fetched.data
                cachedAnimeSearchAtMs = now
                return@withLock fetched
            }
            // Keep the last known index while offline; a failed refresh must not
            // erase search suggestions or results during an interrupted connection.
            if (cached != null) NetworkResult.Success(cached) else fetched
        }

    private suspend fun getVerifiedAnimeNames(): Set<String> = animeIndexMutex.withLock {
        knownAnimeNames?.let { return@withLock it }
        val result = safeApiCall(context) { api.getCatalog(ANIME_TITLES_INDEX) }
        val names = if (result is NetworkResult.Success) {
            result.data.metas.mapNotNull { it?.name?.takeIf(String::isNotBlank) }
                .flatMap { title ->
                    val normalized = AnimeSamaSearchMatcher.normalize(title)
                    if (normalized.length >= 5) {
                        listOf(normalized, normalized.replace(" ", ""))
                    } else listOf(normalized)
                }.toSet()
        } else emptySet()
        // Cache only successful responses; a temporary network failure may be retried.
        if (names.isNotEmpty()) knownAnimeNames = names
        Log.d(TAG, "Anime-Sama name index loaded: ${names.size} entries")
        names
    }

    override fun getCatalog(
        addonBaseUrl: String,
        addonId: String,
        addonName: String,
        catalogId: String,
        catalogName: String,
        type: String,
        skip: Int,
        skipStep: Int,
        extraArgs: Map<String, String>,
        supportsSkip: Boolean
    ): Flow<NetworkResult<CatalogRow>> = flow {
        emit(NetworkResult.Loading)

        // The Anime-Sama addon is a static GitHub Pages catalog. Search locally in its
        // single JSON index instead of requiring a dynamic server on every keystroke.
        val isAnimeSamaSearch = addonId == "fr.joesm0ke.animesama.catalog" &&
            catalogId == "as_search" && extraArgs.containsKey("search")
        val url = if (isAnimeSamaSearch) {
            buildCatalogUrl(addonBaseUrl, type, catalogId, 0, emptyMap())
        } else {
            buildCatalogUrl(addonBaseUrl, type, catalogId, skip, extraArgs)
        }
        Log.d(
            TAG,
            "Fetching catalog addonId=$addonId addonName=$addonName type=$type catalogId=$catalogId skip=$skip skipStep=$skipStep supportsSkip=$supportsSkip url=$url"
        )

        val catalogResult = if (isAnimeSamaSearch) {
            getAnimeSearchSnapshot(url)
        } else {
            safeApiCall(context) { api.getCatalog(url) }
        }
        when (val result = catalogResult) {
            is NetworkResult.Success -> {
                val rawItemCount = result.data.metas.size
                val mappedItems = result.data.metas
                    .mapNotNull { it?.toDomainOrNull(type, addonBaseUrl) }
                    .distinctBy { it.id }
                val isCinemetaSearch = addonId == "com.linvo.cinemeta" &&
                    extraArgs.containsKey("search")
                val items = when {
                    isAnimeSamaSearch -> {
                        val query = extraArgs["search"].orEmpty()
                        mappedItems.mapNotNull { item ->
                            AnimeSamaSearchMatcher.rank(item.name, query)?.let { rank -> item to rank }
                        }.sortedWith(compareBy({ it.second }, { it.first.name.lowercase(java.util.Locale.ROOT) }))
                            .take(60).map { it.first }
                    }
                    isCinemetaSearch -> {
                        // Cinemeta is only a metadata resolver here; hide everything not
                        // explicitly listed in the Anime-Sama site catalogue.
                        val verifiedNames = getVerifiedAnimeNames()
                        mappedItems.filter { AnimeSamaSearchMatcher.verifiedNameMatches(it.name, verifiedNames) }
                    }
                    else -> mappedItems
                }
                Log.d(
                    TAG,
                    "Catalog fetch success addonId=$addonId type=$type catalogId=$catalogId items=${items.size}"
                )

                val catalogRow = CatalogRow(
                    addonId = addonId,
                    addonName = addonName,
                    addonBaseUrl = addonBaseUrl,
                    catalogId = catalogId,
                    catalogName = if (isCinemetaSearch) {
                        if (type == "movie") "Anime-Sama · Films" else "Anime-Sama · Séries"
                    } else catalogName,
                    type = ContentType.fromString(type),
                    rawType = type,
                    items = items,
                    isLoading = false,
                    hasMore = supportsSkip && rawItemCount > 0,
                    currentPage = if (skipStep > 0) skip / skipStep else 0,
                    supportsSkip = supportsSkip,
                    skipStep = skipStep,
                    nextSkip = if (supportsSkip && rawItemCount > 0) skip + rawItemCount else skip,
                    extraArgs = extraArgs
                )
                emit(NetworkResult.Success(catalogRow))
            }
            is NetworkResult.Error -> {
                Log.w(
                    TAG,
                    "Catalog fetch failed addonId=$addonId type=$type catalogId=$catalogId code=${result.code} message=${result.message} url=$url"
                )
                emit(result)
            }
            NetworkResult.Loading -> { /* Already emitted */ }
        }
    }

    private fun buildCatalogUrl(
        baseUrl: String,
        type: String,
        catalogId: String,
        skip: Int,
        extraArgs: Map<String, String>
    ): String {
        val trimmedBase = baseUrl.trimEnd('/')
        val queryStart = trimmedBase.indexOf('?')
        val basePath = if (queryStart >= 0) trimmedBase.substring(0, queryStart).trimEnd('/') else trimmedBase
        val baseQuery = if (queryStart >= 0) trimmedBase.substring(queryStart) else ""

        val catalogPath = if (extraArgs.isEmpty()) {
            if (skip > 0) {
                "$basePath/catalog/$type/$catalogId/skip=$skip.json"
            } else {
                "$basePath/catalog/$type/$catalogId.json"
            }
        } else {
            val allArgs = LinkedHashMap<String, String>()
            allArgs.putAll(extraArgs)

            if (!allArgs.containsKey("skip") && skip > 0) {
                allArgs["skip"] = skip.toString()
            }

            val encodedArgs = allArgs.entries.joinToString("&") { (key, value) ->
                "${encodeArg(key)}=${encodeArg(value)}"
            }

            "$basePath/catalog/$type/$catalogId/$encodedArgs.json"
        }

        return catalogPath + baseQuery
    }

    private fun encodeArg(value: String): String {
        return URLEncoder.encode(value, "UTF-8").replace("+", "%20")
    }
}
