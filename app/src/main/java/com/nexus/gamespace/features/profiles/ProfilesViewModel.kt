package com.nexus.gamespace.features.profiles

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nexus.gamespace.data.NexusRepository
import com.nexus.gamespace.domain.DefaultProfiles
import com.nexus.gamespace.domain.GameProfile
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProfilesViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = NexusRepository(app)

    val profiles: StateFlow<List<GameProfile>> =
        repo.profiles.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val activeId: StateFlow<String> =
        repo.activeProfileId.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "balanced")

    val activeProfile: StateFlow<GameProfile?> =
        combine(repo.profiles, repo.activeProfileId) { list, id -> list.firstOrNull { it.id == id } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val gamesCount: StateFlow<Int> =
        repo.games.map { it.size }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    /** Cuántos juegos tienen asignado cada perfil de forma explícita. */
    val gamesPerProfile: StateFlow<Map<String, Int>> =
        repo.games.map { list -> list.mapNotNull { it.profileId }.groupingBy { it }.eachCount() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    fun setActive(id: String) = viewModelScope.launch { repo.setActiveProfile(id) }

    fun save(profile: GameProfile) = viewModelScope.launch { repo.saveProfile(profile) }

    fun defaultFor(id: String): GameProfile? = DefaultProfiles.default(id, System.currentTimeMillis())
}
