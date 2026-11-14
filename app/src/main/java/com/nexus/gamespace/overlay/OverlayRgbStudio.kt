package com.nexus.gamespace.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.nexus.gamespace.domain.RgbEffect
import com.nexus.gamespace.domain.RgbSettings
import com.nexus.gamespace.domain.RgbSpeed
import com.nexus.gamespace.domain.RgbTarget
import com.nexus.gamespace.ui.theme.NexusWarn

private fun effectDescription(effect: RgbEffect): String = when (effect) {
    RgbEffect.RAINBOW -> "Todos los elementos cambian de color a la vez recorriendo el arcoíris."
    RgbEffect.CHROMA -> "Cada elemento va desfasado: se forma una onda de color por el panel."
    RgbEffect.SPECTRUM -> "Salta entre los colores del espectro con transiciones suaves."
    RgbEffect.GRADIENT -> "Oscila suavemente alrededor del color que elegiste."
    RgbEffect.BREATHING -> "El color que elegiste respira: brilla y se atenúa."
    RgbEffect.COLOR_WAVE -> "Una onda de tonos alrededor del color que elegiste recorre el panel."
}

/**
 * NEXUS RGB MODE. El RGB nunca se aplica al texto ni a las alertas térmicas, y los fondos solo reciben un
 * tinte oscuro para no perjudicar la lectura. Desactivarlo elimina por completo su coste de batería.
 */
@Composable
fun RgbStudioContent(
    rgb: RgbSettings,
    lowBattery: Boolean,
    onChange: (RgbSettings) -> Unit,
    onReduceAnimations: () -> Unit,
) {
    val spacing = LocalOverlaySpacing.current
    val p = LocalOvPalette.current

    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(spacing)) {

        OvCard {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                OvText("NEXUS RGB MODE", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                OvSwitch(rgb.enabled) { onChange(rgb.copy(enabled = it)) }
            }
            OvHint(
                "El texto y las alertas térmicas nunca llevan RGB. Los fondos solo reciben un tinte oscuro. " +
                    "Con las animaciones desactivadas o reducidas el color se queda fijo.",
            )
        }

        if (rgb.enabled && lowBattery) {
            OvCard(title = "Batería baja") {
                OvText("Los efectos RGB consumen batería. Puedes reducirlos o apagarlos.", color = NexusWarn)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OvButton("REDUCIR EFECTOS", onReduceAnimations, Modifier.weight(1f), filled = false)
                    OvButton("DESACTIVAR RGB", { onChange(rgb.copy(enabled = false)) }, Modifier.weight(1f))
                }
            }
        }

        OvCard(title = "Vista previa en directo") {
            if (!rgb.enabled) {
                OvHint("El RGB está desactivado. Actívalo para ver el efecto.")
            } else {
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    for (i in 0 until 12) {
                        Box(Modifier.weight(1f).height(16.dp).background(p.sample(i / 11f)))
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OvButton("BOTÓN", {}, Modifier.weight(1f))
                    OvButton("CONTROL", {}, Modifier.weight(1f), filled = false)
                    OvRing(fraction = 0.72f, label = "Anillo", center = { f -> "${(f * 100).toInt()}%" }, ringSize = 44.dp)
                }
            }
        }

        OvCard(title = "Aplicar a") {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OvChip(selected = rgb.global, onClick = { onChange(rgb.copy(global = true)) }) {
                    Text("RGB Global", style = MaterialTheme.typography.labelLarge, maxLines = 1)
                }
                OvChip(selected = !rgb.global, onClick = { onChange(rgb.copy(global = false)) }) {
                    Text("RGB Selectivo", style = MaterialTheme.typography.labelLarge, maxLines = 1)
                }
            }
            if (rgb.global) {
                OvHint("Bordes, iconos, botones, anillos, fondo e iluminación: todo el panel.")
            } else {
                OvHint("Elige qué componentes tendrán RGB.")
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    RgbTarget.entries.forEach { target ->
                        OvChip(
                            selected = target in rgb.targets,
                            onClick = {
                                val next = if (target in rgb.targets) rgb.targets - target else rgb.targets + target
                                onChange(rgb.copy(targets = next))
                            },
                        ) { Text(target.label, style = MaterialTheme.typography.labelLarge, maxLines = 1) }
                    }
                }
                if (rgb.targets.isEmpty()) OvText("Ningún componente seleccionado: no se aplica RGB.", color = NexusWarn)
            }
        }

        OvCard(title = "Efecto") {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                RgbEffect.entries.forEach { effect ->
                    OvChip(selected = rgb.effect == effect, onClick = { onChange(rgb.copy(effect = effect)) }) {
                        Text(effect.label, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                    }
                }
            }
            OvHint(effectDescription(rgb.effect))
        }

        OvCard(title = "Velocidad Chroma") {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                RgbSpeed.entries.forEach { speed ->
                    OvChip(selected = rgb.speed == speed, onClick = { onChange(rgb.copy(speed = speed)) }) {
                        Text(speed.label, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                    }
                }
            }
            if (rgb.speed == RgbSpeed.CUSTOM) {
                OvSlider("Duración del ciclo", rgb.customPeriodMs / 1000, 1..20, "s") { onChange(rgb.copy(customPeriodMs = it * 1000)) }
            }
            Text("Dirección", style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OvChip(selected = !rgb.reverse, onClick = { onChange(rgb.copy(reverse = false)) }) {
                    Text("Adelante", style = MaterialTheme.typography.labelLarge, maxLines = 1)
                }
                OvChip(selected = rgb.reverse, onClick = { onChange(rgb.copy(reverse = true)) }) {
                    Text("Inversa", style = MaterialTheme.typography.labelLarge, maxLines = 1)
                }
            }
            OvSlider("Intensidad", rgb.intensityPercent, RgbSettings.INTENSITY_RANGE, "%") { onChange(rgb.copy(intensityPercent = it)) }
        }

        OvCard(title = "Ahorro de batería") {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                OvText("Dejar el RGB quieto con batería baja", Modifier.weight(1f))
                OvSwitch(rgb.reduceOnLowBattery) { onChange(rgb.copy(reduceOnLowBattery = it)) }
            }
            OvHint("Con la batería al ${RgbSettings.LOW_BATTERY_PERCENT} % o menos y sin cargar, el color se queda fijo.")
        }

        OvButton(
            "DESACTIVAR RGB POR COMPLETO",
            { onChange(rgb.copy(enabled = false)) },
            Modifier.fillMaxWidth(), filled = false, enabled = rgb.enabled,
        )
    }
}
