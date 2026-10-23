package com.nuvio.tv.ui.components

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.compositionLocalOf
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.tv.core.build.AppFeaturePolicy
import com.nuvio.tv.core.plugin.PluginManager
import com.nuvio.tv.core.streams.PlaybackAvailability
import com.nuvio.tv.data.local.PluginDataStore
import com.nuvio.tv.domain.repository.AddonRepository
import com.nuvio.tv.domain.repository.MetaRepository
import com.nuvio.tv.ui.screens.home.ContinueWatchingItem
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

internal val LocalPlaybackAvailability = compositionLocalOf { PlaybackAvailability() }

@HiltViewModel
internal class PlaybackAvailabilityViewModel @Inject constructor(
    addonRepository: AddonRepository,
    pluginDataStore: PluginDataStore,
    metaRepository: MetaRepository,
    pluginManager: PluginManager,
    @ApplicationContext context: Context
) : ViewModel() {
    init {
        // Provision before the UI asks whether Play is available. Previously
        // provisioning only happened during a stream request, but the playback
        // availability gate prevented that request on a clean installation.
        if (context.packageName == "com.nuviodebug.com" && AppFeaturePolicy.pluginsEnabled) {
            viewModelScope.launch {
                try {
                    pluginManager.ensureAnimeSamaRepositoryForFork()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w("AnimeSamaBootstrap", "Provider initialization will retry: ${e.message}")
                }
            }
        }
    }

    private val enabledScrapers = if (AppFeaturePolicy.pluginsEnabled) {
        combine(pluginDataStore.scrapers, pluginDataStore.pluginsEnabled) { scrapers, enabled ->
            if (enabled) scrapers.filter { it.enabled } else emptyList()
        }
    } else {
        flowOf(emptyList())
    }

    val availability = combine(addonRepository.getInstalledAddons(), enabledScrapers) { addons, scrapers ->
        PlaybackAvailability(addons, scrapers, isLoaded = true, cachedMeta = metaRepository::getCachedMeta)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlaybackAvailability())
}

@Composable
internal fun PlaybackAvailabilityProvider(content: @Composable () -> Unit) {
    val viewModel: PlaybackAvailabilityViewModel = hiltViewModel()
    val availability by viewModel.availability.collectAsStateWithLifecycle()
    CompositionLocalProvider(LocalPlaybackAvailability provides availability, content = content)
}

internal fun PlaybackAvailability.canStream(item: ContinueWatchingItem): Boolean = when (item) {
    is ContinueWatchingItem.InProgress -> canStream(
        item.progress.contentType, item.progress.videoId, item.progress.contentId
    )
    is ContinueWatchingItem.NextUp -> canStream(
        item.info.contentType, item.info.videoId, item.info.contentId
    )
}
