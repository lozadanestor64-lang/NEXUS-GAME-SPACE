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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import com.nexus.gamespace.booster.BoosterController
import com.nexus.gamespace.domain.CrosshairConfig
import com.nexus.gamespace.domain.CrosshairStyle
import com.nexus.gamespace.ui.theme.NexusBorder
import com.nexus.gamespace.ui.theme.NexusRed
import com.nexus.gamespace.ui.theme.NexusTextSecondary

/**
 * Pantalla MIRA del panel. La mira es solo un dibujo fijo, ajustable a mano: no hay detección de enemigos,
 * lectura de imagen, automatización de disparos ni inyección de toques.
 */
@Composable
fun CrosshairContent(controller: BoosterController) {
    val settings by controller.crosshair.collectAsState()
    val favorites by controller.favorites.collectAsState()
    val spacing = LocalOverlaySpacing.current
    // null = mira global. Con un paquete, se edita la mira propia de ese juego.
    var target by remember { mutableStateOf<String?>(null) }
    var step by remember { mutableIntStateOf(1) }

    val cfg = settings.configFor(target)
    fun save(c: CrosshairConfig) = controller.saveCrosshair(target, c)
    val targetLabel = target?.let { pkg -> favorites.firstOrNull { it.packageName == pkg }?.label ?: pkg } ?: "Global"
    val hasOverride = target?.let { it in settings.perGame } ?: false

    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(spacing)) {

        OvCard {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Mostrar mira sobre el juego", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                OvSwitch(checked = settings.enabled, onCheckedChange = { controller.setCrosshairEnabled(it) })
            }
            Text(
                "La mira es solo un dibujo fijo que tú colocas. No detecta enemigos, no lee la pantalla ni dispara. " +
                    "Algunos juegos competitivos prohíben las superposiciones externas: úsala bajo tu responsabilidad.",
                style = MaterialTheme.typography.labelSmall, color = LocalOvPalette.current.description,
            )
        }

        OvCard(title = "Ajustar la mira de") {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OvChip(target == null, { target = null }, { Text("Global", style = MaterialTheme.typography.labelLarge) })
                favorites.forEach { g ->
                    OvChip(
                        selected = target == g.packageName,
                        onClick = { target = g.packageName },
                        label = { Text(g.label, style = MaterialTheme.typography.labelLarge, maxLines = 1) },
                    )
                }
            }
            val game = target
            if (game == null) {
                Text(
                    "La mira global se usa en todos los juegos que no tengan una propia. Los juegos que ves aquí son tus favoritos.",
                    style = MaterialTheme.typography.labelSmall, color = LocalOvPalette.current.description,
                )
            } else {
                Text(
                    if (hasOverride) "$targetLabel tiene su propia mira." else "$targetLabel usa la mira global; al cambiar algo se guardará una propia.",
                    style = MaterialTheme.typography.labelSmall, color = LocalOvPalette.current.description,
                )
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("No mostrar la mira en este juego", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    OvSwitch(checked = game in settings.excluded, onCheckedChange = { controller.setCrosshairExcluded(game, it) })
                }
                if (hasOverride) {
                    OvButton("USAR LA MIRA GLOBAL", { controller.clearCrosshairOverride(game) }, Modifier.fillMaxWidth(), filled = false)
                }
                Text(
                    "Sin el acceso de uso no se puede saber qué juego está en primer plano: la mira de un juego se aplica cuando lo abres desde NEXUS GAME SPACE.",
                    style = MaterialTheme.typography.labelSmall, color = LocalOvPalette.current.description,
                )
            }
        }

        OvCard(title = "Vista previa · $targetLabel") {
            Box(
                Modifier
                    .fillMaxWidth()
                    .size(120.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(LocalOvPalette.current.menuBg)
                    .border(1.dp, LocalOvPalette.current.border, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                AndroidView(
                    factory = { ctx -> CrosshairView(ctx).also { it.config = cfg } },
                    update = { it.config = cfg },
                )
            }
        }

        OvCard(title = "Diseño") {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                CrosshairStyle.entries.forEach { st ->
                    OvChip(
                        selected = cfg.style == st,
                        onClick = { save(cfg.copy(style = st)) },
                        label = { Text(st.label, style = MaterialTheme.typography.labelLarge, maxLines = 1) },
                    )
                }
            }
            if (cfg.style == CrosshairStyle.CUSTOM) {
                PartOvSwitch("Punto central", cfg.customDot) { save(cfg.copy(customDot = it)) }
                PartOvSwitch("Cruz", cfg.customCross) { save(cfg.copy(customCross = it)) }
                PartOvSwitch("Círculo", cfg.customCircle) { save(cfg.copy(customCircle = it)) }
            }
        }

        OvCard(title = "Personalización") {
            Text("Color", style = MaterialTheme.typography.bodyMedium)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CrosshairConfig.PALETTE.forEach { c ->
                    Box(
                        Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(c))
                            .then(if (cfg.colorArgb == c) Modifier.border(3.dp, LocalOvPalette.current.title, CircleShape) else Modifier)
                            .clickable { save(cfg.copy(colorArgb = c)) },
                    )
                }
            }
            OvSlider("Tamaño", cfg.sizeDp, CrosshairConfig.SIZE_RANGE, "dp") { save(cfg.copy(sizeDp = it)) }
            OvSlider("Grosor", cfg.thicknessDp, CrosshairConfig.THICKNESS_RANGE, "dp") { save(cfg.copy(thicknessDp = it)) }
            OvSlider("Separación central", cfg.gapDp, CrosshairConfig.GAP_RANGE, "dp") { save(cfg.copy(gapDp = it)) }
            OvSlider("Transparencia (opacidad)", cfg.alphaPercent, CrosshairConfig.ALPHA_RANGE, "%") { save(cfg.copy(alphaPercent = it)) }
            Text(
                "En Android 12 o superior la ventana de la mira se limita al 80 % de opacidad para no bloquear tus toques en el juego.",
                style = MaterialTheme.typography.labelSmall, color = LocalOvPalette.current.description,
            )
        }

        OvCard(title = "Posición y calibración") {
            Text(
                "Desplazamiento desde el centro: X ${cfg.offsetX} dp · Y ${cfg.offsetY} dp. La mira real se mueve a la vez si está activada.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OvChip(step == 1, { step = 1 }, { Text("Paso 1", style = MaterialTheme.typography.labelLarge) })
                OvChip(step == 5, { step = 5 }, { Text("Paso 5", style = MaterialTheme.typography.labelLarge) })
                OvChip(step == 20, { step = 20 }, { Text("Paso 20", style = MaterialTheme.typography.labelLarge) })
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OvButton("◀", { save(cfg.copy(offsetX = cfg.offsetX - step)) }, Modifier.weight(1f), filled = false)
                OvButton("▲", { save(cfg.copy(offsetY = cfg.offsetY - step)) }, Modifier.weight(1f), filled = false)
                OvButton("▼", { save(cfg.copy(offsetY = cfg.offsetY + step)) }, Modifier.weight(1f), filled = false)
                OvButton("▶", { save(cfg.copy(offsetX = cfg.offsetX + step)) }, Modifier.weight(1f), filled = false)
            }
            OvButton("CENTRAR", { save(cfg.copy(offsetX = 0, offsetY = 0)) }, Modifier.fillMaxWidth(), filled = false)
        }

        Text(
            "Cada ajuste se guarda al instante en tu teléfono.",
            style = MaterialTheme.typography.labelSmall, color = LocalOvPalette.current.accentText,
        )
    }
}

@Composable
private fun PartOvSwitch(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        OvSwitch(checked = checked, onCheckedChange = onChange)
    }
}
