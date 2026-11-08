package com.nexus.gamespace.features.library

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nexus.gamespace.core.InstalledAppsProvider
import com.nexus.gamespace.data.NexusRepository
import com.nexus.gamespace.domain.GameEntry
import com.nexus.gamespace.domain.GameProfile
import com.nexus.gamespace.domain.InstalledApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class LibraryFilter(val label: String) {
    ALL("Todos"),
    FAVORITES("Favoritos"),
    RECENT("Recientes"),
}

data class LibraryItem(val entry: GameEntry, val installed: Boolean)

class LibraryViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = NexusRepository(app)
    private val apps = InstalledAppsProvider(app)

    val query = MutableStateFlow("")
    val filter = MutableStateFlow(LibraryFilter.ALL)
    val pickerQuery = MutableStateFlow("")
    private val installedApps = MutableStateFlow<List<InstalledApp>>(emptyList())

    val profiles: StateFlow<List<GameProfile>> =
        repo.profiles.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val savedPackages: StateFlow<Set<String>> =
        repo.games.map { list -> list.map { it.packageName }.toSet() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val items: StateFlow<List<LibraryItem>> =
        combine(repo.games, query, filter) { games, q, f ->
            val filtered = games
                .filter { q.isBlank() || it.label.contains(q.trim(), ignoreCase = true) }
                .filter {
                    when (f) {
                        LibraryFilter.ALL -> true
                        LibraryFilter.FAVORITES -> it.favorite
                        LibraryFilter.RECENT -> it.lastLaunchedAt != null
                    }
                }
            val sorted = if (f == LibraryFilter.RECENT) {
                filtered.sortedByDescending { it.lastLaunchedAt }
            } else {
                filtered.sortedWith(compareByDescending<GameEntry> { it.favorite }.thenBy { it.label.lowercase() })
            }
            sorted.map { entry ->
                val liveLabel = apps.labelOf(entry.packageName)
                LibraryItem(entry.copy(label = liveLabel ?: entry.label), installed = liveLabel != null)
            }
        }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Apps lanzables para elegir manualmente. Las que se declaran como juego salen primero. */
    val pickerList: StateFlow<List<InstalledApp>> =
        combine(installedApps, pickerQuery) { list, q ->
            list.filter { q.isBlank() || it.label.contains(q.trim(), ignoreCase = true) }
                .sortedWith(compareByDescending<InstalledApp> { it.suggestedGame }.thenBy { it.label.lowercase() })
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun loadInstalled() {
        viewModelScope.launch(Dispatchers.Default) { installedApps.value = apps.launchableApps() }
    }

    fun toggleFavorite(pkg: String) = viewModelScope.launch {
        repo.updateGames { list -> list.map { if (it.packageName == pkg) it.copy(favorite = !it.favorite) else it } }
    }

    fun addGame(app: InstalledApp) = viewModelScope.launch {
        repo.updateGames { list ->
            if (list.any { it.packageName == app.packageName }) list
            else list + GameEntry(app.packageName, app.label, addedAt = System.currentTimeMillis())
        }
    }

    fun removeGame(pkg: String) = viewModelScope.launch {
        repo.updateGames { list -> list.filterNot { it.packageName == pkg } }
    }

    fun setProfile(pkg: String, profileId: String?) = viewModelScope.launch {
        repo.updateGames { list -> list.map { if (it.packageName == pkg) it.copy(profileId = profileId) else it } }
    }

    fun markLaunched(pkg: String) = viewModelScope.launch {
        repo.updateGames { list ->
            list.map { if (it.packageName == pkg) it.copy(lastLaunchedAt = System.currentTimeMillis()) else it }
        }
    }

    fun launchIntent(pkg: String): Intent? = apps.launchIntent(pkg)
}
