package com.nexus.gamespace.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.nexus.gamespace.domain.GameEntry
import com.nexus.gamespace.domain.GameProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.nexusStore by preferencesDataStore(name = "nexus_prefs")

/** Todo se guarda localmente en DataStore. Sin cuentas ni red. */
class NexusRepository(context: Context) {

    private val store = context.applicationContext.nexusStore

    val games: Flow<List<GameEntry>> = store.data.map { JsonCodec.decodeGames(it[GAMES]) }
    val profiles: Flow<List<GameProfile>> = store.data.map { JsonCodec.decodeProfiles(it[PROFILES]) }
    val activeProfileId: Flow<String> = store.data.map { it[ACTIVE_PROFILE] ?: DEFAULT_ACTIVE }

    suspend fun updateGames(transform: (List<GameEntry>) -> List<GameEntry>) {
        store.edit { prefs ->
            val current = JsonCodec.decodeGames(prefs[GAMES])
            prefs[GAMES] = JsonCodec.encodeGames(transform(current))
        }
    }

    suspend fun saveProfile(profile: GameProfile) {
        store.edit { prefs ->
            val current = JsonCodec.decodeProfiles(prefs[PROFILES])
            prefs[PROFILES] = JsonCodec.encodeProfiles(current.map { if (it.id == profile.id) profile else it })
        }
    }

    suspend fun setActiveProfile(id: String) {
        store.edit { prefs -> prefs[ACTIVE_PROFILE] = id }
    }

    private companion object {
        val GAMES = stringPreferencesKey("games_json")
        val PROFILES = stringPreferencesKey("profiles_json")
        val ACTIVE_PROFILE = stringPreferencesKey("active_profile")
        const val DEFAULT_ACTIVE = "balanced"
    }
}
