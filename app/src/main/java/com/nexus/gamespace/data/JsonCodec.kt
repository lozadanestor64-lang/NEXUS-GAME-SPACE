package com.nexus.gamespace.data

import com.nexus.gamespace.domain.ColorMath
import com.nexus.gamespace.domain.ColorRole
import com.nexus.gamespace.domain.CrosshairConfig
import com.nexus.gamespace.domain.CrosshairSettings
import com.nexus.gamespace.domain.CrosshairStyle
import com.nexus.gamespace.domain.DefaultProfiles
import com.nexus.gamespace.domain.GameEntry
import com.nexus.gamespace.domain.GameProfile
import com.nexus.gamespace.domain.OverlayColors
import com.nexus.gamespace.domain.RgbEffect
import com.nexus.gamespace.domain.RgbSettings
import com.nexus.gamespace.domain.RgbSpeed
import com.nexus.gamespace.domain.RgbTarget
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
                    put("forder", g.favoriteOrder)
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
                    favoriteOrder = o.optInt("forder", 0),
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

    // ---- Mira ----

    private fun configToJson(c: CrosshairConfig) = JSONObject().apply {
        put("style", c.style.name)
        put("color", c.colorArgb)
        put("size", c.sizeDp)
        put("thickness", c.thicknessDp)
        put("gap", c.gapDp)
        put("alpha", c.alphaPercent)
        put("ox", c.offsetX)
        put("oy", c.offsetY)
        put("cdot", c.customDot)
        put("ccross", c.customCross)
        put("ccircle", c.customCircle)
    }

    private fun configFromJson(o: JSONObject?): CrosshairConfig {
        if (o == null) return CrosshairConfig()
        val d = CrosshairConfig()
        return CrosshairConfig(
            style = CrosshairStyle.entries.firstOrNull { it.name == o.optString("style") } ?: d.style,
            colorArgb = o.optLong("color", d.colorArgb),
            sizeDp = o.optInt("size", d.sizeDp),
            thicknessDp = o.optInt("thickness", d.thicknessDp),
            gapDp = o.optInt("gap", d.gapDp),
            alphaPercent = o.optInt("alpha", d.alphaPercent),
            offsetX = o.optInt("ox", d.offsetX),
            offsetY = o.optInt("oy", d.offsetY),
            customDot = o.optBoolean("cdot", d.customDot),
            customCross = o.optBoolean("ccross", d.customCross),
            customCircle = o.optBoolean("ccircle", d.customCircle),
        ).clamped()
    }

    fun encodeCrosshair(s: CrosshairSettings): String = JSONObject().apply {
        put("enabled", s.enabled)
        put("active", s.activeGame ?: JSONObject.NULL)
        put("global", configToJson(s.global))
        put("perGame", JSONObject().also { o -> s.perGame.forEach { (pkg, cfg) -> o.put(pkg, configToJson(cfg)) } })
        put("excluded", JSONArray(s.excluded.toList()))
    }.toString()

    fun decodeCrosshair(raw: String?): CrosshairSettings {
        if (raw.isNullOrBlank()) return CrosshairSettings()
        return try {
            val o = JSONObject(raw)
            val perGame = mutableMapOf<String, CrosshairConfig>()
            o.optJSONObject("perGame")?.let { map ->
                val keys = map.keys()
                while (keys.hasNext()) {
                    val pkg = keys.next()
                    perGame[pkg] = configFromJson(map.optJSONObject(pkg))
                }
            }
            val excluded = mutableSetOf<String>()
            o.optJSONArray("excluded")?.let { arr ->
                for (i in 0 until arr.length()) arr.optString(i).takeIf { it.isNotBlank() }?.let { excluded.add(it) }
            }
            CrosshairSettings(
                enabled = o.optBoolean("enabled", false),
                global = configFromJson(o.optJSONObject("global")),
                perGame = perGame,
                excluded = excluded,
                activeGame = if (o.isNull("active")) null else o.optString("active").ifBlank { null },
            )
        } catch (e: JSONException) {
            CrosshairSettings()
        }
    }

    // ---- Colores del panel ----

    fun encodeColors(c: OverlayColors): String = JSONObject().apply {
        put("auto", c.autoContrast)
        put("colors", JSONObject().also { o -> c.values.forEach { (role, argb) -> o.put(role.name, ColorMath.toHex(argb)) } })
    }.toString()

    /** Ignora los roles desconocidos y los valores que no son colores válidos. */
    fun decodeColors(raw: String?): OverlayColors {
        if (raw.isNullOrBlank()) return OverlayColors()
        return try {
            val o = JSONObject(raw)
            val values = mutableMapOf<ColorRole, Long>()
            o.optJSONObject("colors")?.let { map ->
                ColorRole.entries.forEach { role ->
                    if (map.has(role.name)) {
                        ColorMath.parseHex(map.optString(role.name))?.let { values[role] = it }
                    }
                }
            }
            OverlayColors(values = values, autoContrast = o.optBoolean("auto", true))
        } catch (e: JSONException) {
            OverlayColors()
        }
    }

    // ---- RGB ----

    fun encodeRgb(r: RgbSettings): String = JSONObject().apply {
        put("enabled", r.enabled)
        put("global", r.global)
        put("targets", JSONArray(r.targets.map { it.name }))
        put("effect", r.effect.name)
        put("speed", r.speed.name)
        put("customMs", r.customPeriodMs)
        put("reverse", r.reverse)
        put("intensity", r.intensityPercent)
        put("lowBattery", r.reduceOnLowBattery)
    }.toString()

    fun decodeRgb(raw: String?): RgbSettings {
        if (raw.isNullOrBlank()) return RgbSettings()
        return try {
            val o = JSONObject(raw)
            val d = RgbSettings()
            val targets = mutableSetOf<RgbTarget>()
            o.optJSONArray("targets")?.let { arr ->
                for (i in 0 until arr.length()) {
                    RgbTarget.entries.firstOrNull { it.name == arr.optString(i) }?.let { targets.add(it) }
                }
            }
            RgbSettings(
                enabled = o.optBoolean("enabled", d.enabled),
                global = o.optBoolean("global", d.global),
                targets = targets,
                effect = RgbEffect.entries.firstOrNull { it.name == o.optString("effect") } ?: d.effect,
                speed = RgbSpeed.entries.firstOrNull { it.name == o.optString("speed") } ?: d.speed,
                customPeriodMs = o.optInt("customMs", d.customPeriodMs).coerceIn(RgbSettings.PERIOD_RANGE),
                reverse = o.optBoolean("reverse", d.reverse),
                intensityPercent = o.optInt("intensity", d.intensityPercent).coerceIn(RgbSettings.INTENSITY_RANGE),
                reduceOnLowBattery = o.optBoolean("lowBattery", d.reduceOnLowBattery),
            )
        } catch (e: JSONException) {
            RgbSettings()
        }
    }
}
