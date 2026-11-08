package com.nexus.gamespace.data

import com.nexus.gamespace.domain.DefaultProfiles
import com.nexus.gamespace.domain.GameEntry
import com.nexus.gamespace.domain.GameProfile
import com.nexus.gamespace.domain.ProfileSettings
import com.nexus.gamespace.domain.ProfileType
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/** Serialización simple a JSON para guardar en DataStore. Si los datos están dañados se devuelven valores por defecto. */
object JsonCodec {

    fun encodeGames(games: List<GameEntry>): String {
        val arr = JSONArray()
        games.forEach { g ->
            arr.put(
                JSONObject().apply {
                    put("pkg", g.packageName)
                    put("label", g.label)
                    put("fav", g.favorite)
                    put("profile", g.profileId ?: JSONObject.NULL)
                    put("added", g.addedAt)
                    put("last", g.lastLaunchedAt ?: JSONObject.NULL)
                }
            )
        }
        return arr.toString()
    }

    fun decodeGames(raw: String?): List<GameEntry> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                val pkg = o.optString("pkg")
                if (pkg.isBlank()) return@mapNotNull null
                GameEntry(
                    packageName = pkg,
                    label = o.optString("label", pkg),
                    favorite = o.optBoolean("fav", false),
                    profileId = if (o.isNull("profile")) null else o.optString("profile"),
                    addedAt = o.optLong("added", 0L),
                    lastLaunchedAt = if (o.isNull("last")) null else o.optLong("last"),
                )
            }
        } catch (e: JSONException) {
            emptyList()
        }
    }

    fun encodeProfiles(profiles: List<GameProfile>): String {
        val arr = JSONArray()
        profiles.forEach { p ->
            arr.put(
                JSONObject().apply {
                    put("id", p.id)
                    put("name", p.name)
                    put("desc", p.description)
                    put("color", p.colorArgb)
                    put("icon", p.iconKey)
                    put("updated", p.updatedAt)
                    put("thermal", p.settings.thermalAlerts)
                    put("record", p.settings.recordSessionStats)
                    put("open", p.settings.openGameOnStart)
                    put("interval", p.settings.monitorIntervalSec)
                    put("dnd", p.settings.doNotDisturb)
                }
            )
        }
        return arr.toString()
    }

    /** Siempre devuelve los 4 perfiles. Los predefinidos conservan nombre y descripción originales. */
    fun decodeProfiles(raw: String?, now: Long = System.currentTimeMillis()): List<GameProfile> {
        val defaults = DefaultProfiles.all(now)
        if (raw.isNullOrBlank()) return defaults
        val saved = try {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i -> arr.optJSONObject(i) }.associateBy { it.optString("id") }
        } catch (e: JSONException) {
            return defaults
        }
        return defaults.map { d ->
            val o = saved[d.id] ?: return@map d
            val base = d.settings
            d.copy(
                name = if (d.type == ProfileType.CUSTOM) o.optString("name", d.name).ifBlank { d.name } else d.name,
                description = if (d.type == ProfileType.CUSTOM) o.optString("desc", d.description) else d.description,
                colorArgb = o.optLong("color", d.colorArgb),
                updatedAt = o.optLong("updated", d.updatedAt),
                settings = ProfileSettings(
                    thermalAlerts = o.optBoolean("thermal", base.thermalAlerts),
                    recordSessionStats = o.optBoolean("record", base.recordSessionStats),
                    openGameOnStart = o.optBoolean("open", base.openGameOnStart),
                    monitorIntervalSec = o.optInt("interval", base.monitorIntervalSec),
                    doNotDisturb = o.optBoolean("dnd", base.doNotDisturb),
                ),
            )
        }
    }
}
