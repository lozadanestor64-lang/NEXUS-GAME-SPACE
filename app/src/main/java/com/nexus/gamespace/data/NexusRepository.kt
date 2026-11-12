package com.nexus.gamespace.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.nexus.gamespace.domain.BoosterRestoreState
import com.nexus.gamespace.domain.CrosshairSettings
import com.nexus.gamespace.domain.AnimationMode
import com.nexus.gamespace.domain.GameEntry
import com.nexus.gamespace.domain.GameProfile
import com.nexus.gamespace.domain.OverlayPrefs
import com.nexus.gamespace.domain.OverlaySide
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

    val boosterRestore: Flow<BoosterRestoreState> = store.data.map {
        BoosterRestoreState(it[DND_PREV], it[BRIGHT_PREV], it[BRIGHT_MODE_PREV], it[ROTATION_PREV])
    }
    val overlayPrefs: Flow<OverlayPrefs> = store.data.map {
        OverlayPrefs(
            side = if (it[OV_SIDE] == "left") OverlaySide.LEFT else OverlaySide.RIGHT,
            alphaPercent = (it[OV_ALPHA] ?: 92).coerceIn(OverlayPrefs.ALPHA_MIN, OverlayPrefs.ALPHA_MAX),
            scalePercent = (it[OV_SCALE] ?: 100).coerceIn(OverlayPrefs.SCALE_RANGE),
            tabY = it[OV_TAB_Y] ?: -1,
            widthPercent = (it[OV_WIDTH] ?: 100).coerceIn(OverlayPrefs.WIDTH_RANGE),
            heightPercent = (it[OV_HEIGHT] ?: 100).coerceIn(OverlayPrefs.HEIGHT_RANGE),
            textScalePercent = (it[OV_TEXT] ?: 100).coerceIn(OverlayPrefs.TEXT_RANGE),
            iconScalePercent = (it[OV_ICON] ?: 100).coerceIn(OverlayPrefs.ICON_RANGE),
            spacingDp = (it[OV_SPACING] ?: 8).coerceIn(OverlayPrefs.SPACING_RANGE),
            marginDp = (it[OV_MARGIN] ?: 8).coerceIn(OverlayPrefs.MARGIN_RANGE),
            animation = AnimationMode.entries.firstOrNull { m -> m.name == it[OV_ANIM] } ?: AnimationMode.NORMAL,
            hiddenTools = (it[OV_HIDDEN] ?: "").split(",").filter { id -> id.isNotBlank() }.toSet(),
        )
    }
    val crosshair: Flow<CrosshairSettings> = store.data.map { JsonCodec.decodeCrosshair(it[CROSSHAIR]) }
    val notes: Flow<String> = store.data.map { it[NOTES] ?: "" }

    /** Guarda el valor original solo si aún no hay uno, para no sobrescribirlo con un valor ya modificado. */
    suspend fun saveDndPreviousIfAbsent(filter: Int) {
        store.edit { prefs -> if (prefs[DND_PREV] == null) prefs[DND_PREV] = filter }
    }

    suspend fun clearDndPrevious() {
        store.edit { prefs -> prefs.remove(DND_PREV) }
    }

    suspend fun saveBrightnessPreviousIfAbsent(value: Int, mode: Int) {
        store.edit { prefs ->
            if (prefs[BRIGHT_PREV] == null) {
                prefs[BRIGHT_PREV] = value
                prefs[BRIGHT_MODE_PREV] = mode
            }
        }
    }

    suspend fun clearBrightnessPrevious() {
        store.edit { prefs ->
            prefs.remove(BRIGHT_PREV)
            prefs.remove(BRIGHT_MODE_PREV)
        }
    }

    suspend fun saveRotationPreviousIfAbsent(value: Int) {
        store.edit { prefs -> if (prefs[ROTATION_PREV] == null) prefs[ROTATION_PREV] = value }
    }

    suspend fun clearRotationPrevious() {
        store.edit { prefs -> prefs.remove(ROTATION_PREV) }
    }

    suspend fun setOverlayPosition(side: OverlaySide, tabY: Int) {
        store.edit { prefs ->
            prefs[OV_SIDE] = if (side == OverlaySide.LEFT) "left" else "right"
            prefs[OV_TAB_Y] = tabY
        }
    }

    suspend fun setOverlaySide(side: OverlaySide) {
        store.edit { prefs -> prefs[OV_SIDE] = if (side == OverlaySide.LEFT) "left" else "right" }
    }

    suspend fun setOverlayAlpha(percent: Int) {
        store.edit { prefs -> prefs[OV_ALPHA] = percent.coerceIn(OverlayPrefs.ALPHA_MIN, OverlayPrefs.ALPHA_MAX) }
    }

    /** Guarda de una vez todos los ajustes de tamaño y animación (preajustes y control personalizado). */
    suspend fun saveOverlayAppearance(p: OverlayPrefs) {
        store.edit { prefs ->
            prefs[OV_ALPHA] = p.alphaPercent.coerceIn(OverlayPrefs.ALPHA_MIN, OverlayPrefs.ALPHA_MAX)
            prefs[OV_SCALE] = p.scalePercent.coerceIn(OverlayPrefs.SCALE_RANGE)
            prefs[OV_WIDTH] = p.widthPercent.coerceIn(OverlayPrefs.WIDTH_RANGE)
            prefs[OV_HEIGHT] = p.heightPercent.coerceIn(OverlayPrefs.HEIGHT_RANGE)
            prefs[OV_TEXT] = p.textScalePercent.coerceIn(OverlayPrefs.TEXT_RANGE)
            prefs[OV_ICON] = p.iconScalePercent.coerceIn(OverlayPrefs.ICON_RANGE)
            prefs[OV_SPACING] = p.spacingDp.coerceIn(OverlayPrefs.SPACING_RANGE)
            prefs[OV_MARGIN] = p.marginDp.coerceIn(OverlayPrefs.MARGIN_RANGE)
            prefs[OV_ANIM] = p.animation.name
        }
    }

    suspend fun setOverlayScale(percent: Int) {
        store.edit { prefs -> prefs[OV_SCALE] = percent }
    }

    suspend fun setOverlayToolHidden(toolId: String, hidden: Boolean) {
        store.edit { prefs ->
            val current = (prefs[OV_HIDDEN] ?: "").split(",").filter { it.isNotBlank() }.toMutableSet()
            if (hidden) current.add(toolId) else current.remove(toolId)
            prefs[OV_HIDDEN] = current.joinToString(",")
        }
    }

    suspend fun updateCrosshair(transform: (CrosshairSettings) -> CrosshairSettings) {
        store.edit { prefs ->
            val current = JsonCodec.decodeCrosshair(prefs[CROSSHAIR])
            prefs[CROSSHAIR] = JsonCodec.encodeCrosshair(transform(current))
        }
    }

    suspend fun saveNotes(text: String) {
        store.edit { prefs -> prefs[NOTES] = text }
    }

    private companion object {
        val DND_PREV = intPreferencesKey("booster_dnd_prev")
        val BRIGHT_PREV = intPreferencesKey("booster_brightness_prev")
        val BRIGHT_MODE_PREV = intPreferencesKey("booster_brightness_mode_prev")
        val NOTES = stringPreferencesKey("booster_notes")
        val CROSSHAIR = stringPreferencesKey("crosshair_json")
        val ROTATION_PREV = intPreferencesKey("booster_rotation_prev")
        val OV_SIDE = stringPreferencesKey("overlay_side")
        val OV_ALPHA = intPreferencesKey("overlay_alpha")
        val OV_SCALE = intPreferencesKey("overlay_scale")
        val OV_TAB_Y = intPreferencesKey("overlay_tab_y")
        val OV_HIDDEN = stringPreferencesKey("overlay_hidden_tools")
        val OV_WIDTH = intPreferencesKey("overlay_width")
        val OV_HEIGHT = intPreferencesKey("overlay_height")
        val OV_TEXT = intPreferencesKey("overlay_text")
        val OV_ICON = intPreferencesKey("overlay_icon")
        val OV_SPACING = intPreferencesKey("overlay_spacing")
        val OV_MARGIN = intPreferencesKey("overlay_margin")
        val OV_ANIM = stringPreferencesKey("overlay_animation")
        val GAMES = stringPreferencesKey("games_json")
        val PROFILES = stringPreferencesKey("profiles_json")
        val ACTIVE_PROFILE = stringPreferencesKey("active_profile")
        const val DEFAULT_ACTIVE = "balanced"
    }
}
