package com.nexus.gamespace.overlay

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nexus.gamespace.domain.AnimationMode
import com.nexus.gamespace.domain.OverlayPrefs
import com.nexus.gamespace.domain.OverlaySide
import com.nexus.gamespace.domain.SizePreset
import com.nexus.gamespace.ui.theme.NexusTextSecondary

/**
 * CONFIGURACIÓN DEL PANEL (grupo 1: tamaño, posición y animaciones). Los cambios se guardan con DataStore
 * y se ven al instante en este mismo panel, que hace de vista previa en tiempo real.
 */
@Composable
fun PanelSettingsContent(
    prefs: OverlayPrefs,
    onSave: (OverlayPrefs) -> Unit,
    onSide: (OverlaySide) -> Unit,
    onOpenColors: () -> Unit,
) {
    val spacing = LocalOverlaySpacing.current
    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(spacing)) {

        OvCard(title = "Tamaño del panel") {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SizePreset.entries.forEach { p ->
                    OvChip(
                        selected = prefs.preset() == p,
                        onClick = { if (p != SizePreset.CUSTOM) onSave(prefs.withPreset(p)) },
                        label = { Text(p.label, style = MaterialTheme.typography.labelLarge, maxLines = 1) },
                    )
                }
            }
            OvSlider("Tamaño global", prefs.scalePercent, OverlayPrefs.SCALE_RANGE, "%") { onSave(prefs.copy(scalePercent = it)) }
            OvSlider("Ancho", prefs.widthPercent, OverlayPrefs.WIDTH_RANGE, "%") { onSave(prefs.copy(widthPercent = it)) }
            OvSlider("Alto máximo", prefs.heightPercent, OverlayPrefs.HEIGHT_RANGE, "%") { onSave(prefs.copy(heightPercent = it)) }
            OvSlider("Texto", prefs.textScalePercent, OverlayPrefs.TEXT_RANGE, "%") { onSave(prefs.copy(textScalePercent = it)) }
            OvSlider("Iconos", prefs.iconScalePercent, OverlayPrefs.ICON_RANGE, "%") { onSave(prefs.copy(iconScalePercent = it)) }
            OvSlider("Espaciado", prefs.spacingDp, OverlayPrefs.SPACING_RANGE, "dp") { onSave(prefs.copy(spacingDp = it)) }
            OvSlider("Márgenes", prefs.marginDp, OverlayPrefs.MARGIN_RANGE, "dp") { onSave(prefs.copy(marginDp = it)) }
            OvSlider("Opacidad", prefs.alphaPercent, OverlayPrefs.ALPHA_MIN..OverlayPrefs.ALPHA_MAX, "%") { onSave(prefs.copy(alphaPercent = it)) }
            Text(
                "El ajuste se aplica al soltar el control y este mismo panel te sirve de vista previa.",
                style = MaterialTheme.typography.labelSmall, color = LocalOvPalette.current.description,
            )
        }

        OvCard(title = "Colores") {
            OvButton("COLOR STUDIO", onOpenColors, Modifier.fillMaxWidth())
            OvHint("Colores de texto, fondos, bordes, iconos, anillos y más, con códigos HEX y avisos de contraste.")
        }

        OvCard(title = "Posición") {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OvChip(prefs.side == OverlaySide.LEFT, { onSide(OverlaySide.LEFT) }, { Text("Izquierda", style = MaterialTheme.typography.labelLarge) })
                OvChip(prefs.side == OverlaySide.RIGHT, { onSide(OverlaySide.RIGHT) }, { Text("Derecha", style = MaterialTheme.typography.labelLarge) })
            }
            Text(
                "También puedes arrastrar la pestaña lateral: se queda donde la sueltes y cambia de lado si la llevas al otro extremo.",
                style = MaterialTheme.typography.labelSmall, color = LocalOvPalette.current.description,
            )
        }

        OvCard(title = "Animaciones") {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                AnimationMode.entries.forEach { m ->
                    OvChip(
                        selected = prefs.animation == m,
                        onClick = { onSave(prefs.copy(animation = m)) },
                        label = { Text(m.label, style = MaterialTheme.typography.labelLarge, maxLines = 1) },
                    )
                }
            }
            Text(
                "Los anillos crecen desde cero hasta el valor real al abrir el panel y se animan cuando el dato cambia. " +
                    "Desactivadas o Reducidas ahorran batería. No hay animaciones infinitas con el panel cerrado.",
                style = MaterialTheme.typography.labelSmall, color = LocalOvPalette.current.description,
            )
        }
    }
}
