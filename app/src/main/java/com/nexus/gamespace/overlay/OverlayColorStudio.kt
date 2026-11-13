package com.nexus.gamespace.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nexus.gamespace.domain.ColorGroup
import com.nexus.gamespace.domain.ColorMath
import com.nexus.gamespace.domain.ColorPresets
import com.nexus.gamespace.domain.ColorRole
import com.nexus.gamespace.domain.OverlayColors
import com.nexus.gamespace.ui.theme.NexusOk
import com.nexus.gamespace.ui.theme.NexusWarn

private val QUICK_COLORS: List<Long> = listOf(
    0xFFFFFFFFL, 0xFFE9E9EEL, 0xFF9FA0ABL, 0xFF000000L, 0xFF07070AL, 0xFF121217L, 0xFF2A2A31L,
    0xFFE5202EL, 0xFFFF4D5AL, 0xFFFF9100L, 0xFFFFEB3BL, 0xFF00E676L, 0xFF00E5FFL, 0xFF4DA3FFL, 0xFFB57BFFL, 0xFFFF4081L,
)

/**
 * COLOR STUDIO. Cada cambio se guarda al instante (DataStore) y se ve en todo el panel, que hace de vista
 * previa. Los avisos de contraste usan la relación WCAG; el contraste automático evita texto ilegible.
 */
@Composable
fun ColorStudioContent(colors: OverlayColors, onChange: (OverlayColors) -> Unit) {
    val spacing = LocalOverlaySpacing.current
    var editing by remember { mutableStateOf<ColorRole?>(null) }

    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(spacing)) {

        PreviewCard()

        OvCard(title = "Legibilidad") {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                OvText("Contraste automático", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                OvSwitch(colors.autoContrast) { onChange(colors.copy(autoContrast = it)) }
            }
            OvHint("Si un texto casi no se lee sobre su fondo, NEXUS lo cambia a blanco o negro. Los avisos se muestran igualmente.")
            val warnings = colors.warnings()
            if (warnings.isEmpty()) {
                OvText("Todos los textos tienen buen contraste.", color = NexusOk)
            } else {
                warnings.forEach {
                    OvText(
                        "⚠ ${it.text.label} sobre ${it.background.label}: contraste ${"%.1f".format(it.ratio)}:1 (mínimo recomendado 4,5:1)",
                        color = NexusWarn,
                    )
                }
            }
            OvHint("Las alertas térmicas y de seguridad mantienen siempre sus colores de alerta.")
        }

        OvCard(title = "Paletas") {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ColorPresets.all.forEach { preset ->
                    OvChip(
                        selected = preset.colors.values == colors.values,
                        onClick = { onChange(preset.colors.copy(autoContrast = colors.autoContrast)) },
                    ) { Text(preset.name, style = MaterialTheme.typography.labelLarge, maxLines = 1) }
                }
            }
        }

        editing?.let { role ->
            RoleEditor(role, colors, onChange) { editing = null }
        }

        ColorGroup.entries.forEach { group ->
            OvCard(title = group.label) {
                ColorRole.entries.filter { it.group == group }.forEach { role ->
                    RoleRow(role, colors, selected = editing == role) { editing = role }
                }
            }
        }

        OvButton(
            "RESTAURAR TODOS LOS COLORES",
            { onChange(OverlayColors(autoContrast = colors.autoContrast)); editing = null },
            Modifier.fillMaxWidth(), filled = false,
        )
    }
}

@Composable
private fun PreviewCard() {
    val p = LocalOvPalette.current
    OvCard(title = "Vista previa en directo") {
        OvText("Texto principal de ejemplo")
        OvText("Texto secundario de ejemplo", color = p.textSecondary)
        OvText("Texto de acento", color = p.accentText)
        OvHint("Descripción de ejemplo")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OvButton("ACTIVO", {}, Modifier.weight(1f))
            OvButton("CONTROL", {}, Modifier.weight(1f), filled = false)
            OvRing(fraction = 0.72f, label = "Anillo", center = { f -> "${(f * 100).toInt()}%" }, ringSize = 48.dp)
        }
    }
}

@Composable
private fun RoleRow(role: ColorRole, colors: OverlayColors, selected: Boolean, onSelect: () -> Unit) {
    val p = LocalOvPalette.current
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(Color(colors.raw(role)))
                .border(if (selected) 2.dp else 1.dp, if (selected) p.selection else p.border, CircleShape),
        )
        OvText(role.label, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        OvText(ColorMath.toHex(colors.raw(role)), style = MaterialTheme.typography.labelSmall, color = p.description)
        if (colors.isCustom(role)) OvText("●", style = MaterialTheme.typography.labelSmall, color = p.accentText)
    }
}

@Composable
private fun RoleEditor(role: ColorRole, colors: OverlayColors, onChange: (OverlayColors) -> Unit, onClose: () -> Unit) {
    val p = LocalOvPalette.current
    val raw = colors.raw(role)
    var hex by remember(role, raw) { mutableStateOf(ColorMath.toHex(raw)) }
    // Valores del selector mientras se arrastra; el color se guarda al soltar.
    val hsv = remember(role, raw) { ColorMath.rgbToHsv(raw) }
    var h by remember(role, raw) { mutableStateOf(hsv[0]) }
    var s by remember(role, raw) { mutableStateOf(hsv[1]) }
    var v by remember(role, raw) { mutableStateOf(hsv[2]) }
    val live = ColorMath.hsvToArgb(h, s, v)

    OvCard(title = "Editar: ${role.label}") {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(Color(live)).border(1.dp, p.border, CircleShape))
            OutlinedTextField(
                value = hex,
                onValueChange = { text ->
                    hex = text
                    ColorMath.parseHex(text)?.let { onChange(colors.with(role, it)) }
                },
                modifier = Modifier.weight(1f),
                singleLine = true,
                isError = ColorMath.parseHex(hex) == null,
                label = { Text("Código HEX (#RRGGBB)") },
            )
        }
        HsvSlider("Matiz", h, 0f..360f) { h = it }
        HsvSlider("Saturación", s, 0f..1f) { s = it }
        HsvSlider("Brillo", v, 0f..1f) { v = it }
        OvButton("APLICAR COLOR DEL SELECTOR", { onChange(colors.with(role, ColorMath.hsvToArgb(h, s, v))) }, Modifier.fillMaxWidth(), filled = false)

        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            QUICK_COLORS.forEach { c ->
                Box(
                    Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(Color(c))
                        .border(if (c == raw) 3.dp else 1.dp, if (c == raw) p.title else p.border, CircleShape)
                        .clickable { onChange(colors.with(role, c)) },
                )
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OvButton("RESTAURAR", { onChange(colors.reset(role)) }, Modifier.weight(1f), filled = false)
            OvButton("LISTO", onClose, Modifier.weight(1f))
        }
    }
}

@Composable
private fun HsvSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    val p = LocalOvPalette.current
    Column {
        OvText(label, style = MaterialTheme.typography.labelSmall, color = p.description)
        Slider(
            value = value.coerceIn(range),
            onValueChange = onChange,
            valueRange = range,
            colors = SliderDefaults.colors(thumbColor = p.activeButton, activeTrackColor = p.activeButton, inactiveTrackColor = p.bars),
        )
    }
}
