package com.nexus.gamespace.overlay

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.gamespace.domain.AnimationMode
import com.nexus.gamespace.domain.PanelStyle
import kotlin.math.roundToInt

/** Valores de diseño que el panel reparte a sus componentes. */
val LocalOverlaySpacing = compositionLocalOf { 8.dp }
val LocalIconScale = compositionLocalOf { 1f }
val LocalAnimation = compositionLocalOf { AnimationMode.NORMAL }
val LocalPanelStyle = compositionLocalOf { PanelStyle.REDMAGIC }

/** Tipografía compacta del panel flotante: textos legibles y sin tarjetas enormes. */
val OverlayTypography = Typography(
    headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Black, fontSize = 20.sp, lineHeight = 24.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 20.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 17.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 13.sp, lineHeight = 18.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp),
    labelSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 10.sp, lineHeight = 13.sp),
)

// ----------------------------------------------------------------------------- textos

/** Texto principal del panel. Siempre lleva un color explícito de la paleta: nunca el negro por defecto. */
@Composable
fun OvText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    color: Color = LocalOvPalette.current.text,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    textAlign: TextAlign? = null,
) {
    Text(text, modifier = modifier, style = style, color = color, maxLines = maxLines, overflow = overflow, textAlign = textAlign)
}

/** Texto de ayuda o descripción (color «Descripciones»). */
@Composable
fun OvHint(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier = modifier, style = MaterialTheme.typography.labelSmall, color = LocalOvPalette.current.description)
}

// ----------------------------------------------------------------------------- contenedores

/**
 * Recorta con la forma dada y dibuja fondo y borde en la fase de dibujo. Así, cuando el RGB cambia un color
 * solo se redibuja el marco y no se recompone el contenido.
 */
fun Modifier.ovFrame(
    shape: Shape,
    background: () -> Color,
    border: () -> Color,
    borderWidth: Dp = 1.dp,
): Modifier = this
    .clip(shape)
    .drawBehind {
        drawRect(background())
        val outline = shape.createOutline(size, layoutDirection, this)
        drawOutline(outline, border(), style = Stroke(width = borderWidth.toPx() * 2f))
    }

@Composable
fun OvCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val spacing = LocalOverlaySpacing.current
    val p = LocalOvPalette.current
    val style = LocalPanelStyle.current
    val redmagic = style == PanelStyle.REDMAGIC
    // REDMAGIC: esquinas cortadas y una marca luminosa; XIAOMI: esquinas redondeadas y más compacta.
    val shape: Shape = if (redmagic) CutCornerShape(topStart = 10.dp, bottomEnd = 10.dp) else RoundedCornerShape(16.dp)
    val inner = if (redmagic) spacing else spacing * 0.85f
    Column(
        modifier = modifier
            .fillMaxWidth()
            .ovFrame(shape, background = { p.cardBg }, border = { p.border })
            .then(
                if (redmagic) {
                    Modifier.drawBehind {
                        drawRect(p.glow.copy(alpha = 0.9f), size = androidx.compose.ui.geometry.Size(3.dp.toPx(), size.height * 0.5f))
                    }
                } else Modifier
            )
            .padding(inner),
        verticalArrangement = Arrangement.spacedBy(inner * 0.75f),
    ) {
        if (title != null) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = p.title)
        }
        content()
    }
}

@Composable
fun OvButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    filled: Boolean = true,
    enabled: Boolean = true,
) {
    val p = LocalOvPalette.current
    val shape = RoundedCornerShape(10.dp)
    val bg = when {
        !enabled -> p.menuBg
        filled -> p.activeButton
        else -> p.buttonBg
    }
    val fg = when {
        !enabled -> p.description
        filled -> p.buttonText
        else -> p.controlText
    }
    Box(
        modifier = modifier
            .heightIn(min = 34.dp)
            .clip(shape)
            .background(bg)
            .then(if (!filled) Modifier.border(1.dp, if (enabled) p.activeButton else p.border, shape) else Modifier)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text, style = MaterialTheme.typography.labelLarge, color = fg,
            maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
        )
    }
}

/** Botón circular perfecto con contenido propio (icono o texto corto). El contenido hereda el color de iconos. */
@Composable
fun OvRoundButton(
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    filled: Boolean = false,
    size: Dp = 32.dp,
    content: @Composable () -> Unit,
) {
    val p = LocalOvPalette.current
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(if (filled) p.activeButton else p.buttonBg)
            .then(if (!filled) Modifier.border(1.dp, p.activeButton.copy(alpha = 0.8f), CircleShape) else Modifier)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides (if (filled) p.buttonText else p.icons)) { content() }
    }
}

@Composable
fun OvSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val p = LocalOvPalette.current
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        colors = SwitchDefaults.colors(
            checkedThumbColor = p.buttonText,
            checkedTrackColor = p.indicators,
            checkedBorderColor = p.indicators,
            uncheckedThumbColor = p.textSecondary,
            uncheckedTrackColor = p.bars,
            uncheckedBorderColor = p.border,
        ),
    )
}

@Composable
fun OvCheckbox(checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?, enabled: Boolean = true) {
    val p = LocalOvPalette.current
    Checkbox(
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
        colors = CheckboxDefaults.colors(
            checkedColor = p.indicators,
            uncheckedColor = p.border,
            checkmarkColor = p.buttonText,
        ),
    )
}

@Composable
fun OvChip(selected: Boolean, onClick: () -> Unit, label: @Composable () -> Unit) {
    val p = LocalOvPalette.current
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = label,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = p.menuBg,
            labelColor = p.controlText,
            selectedContainerColor = p.selection.copy(alpha = 0.30f),
            selectedLabelColor = p.controlText,
        ),
    )
}

// ----------------------------------------------------------------------------- datos

@Composable
fun OvStat(label: String, value: String, modifier: Modifier = Modifier) {
    val p = LocalOvPalette.current
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = p.description, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = p.monitorValues, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun OvStatPair(aLabel: String, aValue: String, bLabel: String, bValue: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OvStat(aLabel, aValue, Modifier.weight(1f))
        OvStat(bLabel, bValue, Modifier.weight(1f))
    }
}

@Composable
fun OvSlider(label: String, value: Int, range: IntRange, unit: String, onFinished: (Int) -> Unit) {
    val p = LocalOvPalette.current
    var current by remember(value) { mutableFloatStateOf(value.toFloat()) }
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = p.text)
            Text("${current.roundToInt()} $unit", style = MaterialTheme.typography.bodyMedium, color = p.numbers)
        }
        Slider(
            value = current,
            onValueChange = { current = it },
            onValueChangeFinished = { onFinished(current.roundToInt()) },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            colors = SliderDefaults.colors(
                thumbColor = p.activeButton,
                activeTrackColor = p.activeButton,
                inactiveTrackColor = p.bars,
            ),
        )
    }
}

/**
 * Anillo indicador. Al abrirse crece desde cero hasta el valor real y, cuando el dato cambia, se anima
 * desde el valor anterior al nuevo. Con las animaciones desactivadas salta directo al valor.
 * fraction == null significa «no disponible»: se dibuja solo la pista.
 * color == null usa el color «Anillos»; el anillo térmico pasa su propio color de alerta.
 */
@Composable
fun OvRing(
    fraction: Float?,
    label: String,
    center: (Float) -> String,
    modifier: Modifier = Modifier,
    color: Color? = null,
    ringSize: Dp = 62.dp,
) {
    val p = LocalOvPalette.current
    val mode = LocalAnimation.current
    val ringColor = color ?: p.rings
    val progress = remember { Animatable(0f) }
    val target = (fraction ?: 0f).coerceIn(0f, 1f)
    LaunchedEffect(target, mode) {
        if (mode == AnimationMode.OFF) {
            progress.snapTo(target)
        } else {
            progress.animateTo(target, tween(mode.durationMs, easing = FastOutSlowInEasing))
        }
    }
    val glow = mode != AnimationMode.OFF && mode != AnimationMode.REDUCED
    val track = p.bars
    val glowColor = if (color == null) p.glow else ringColor

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(ringSize), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(ringSize)) {
                val stroke = size.minDimension * 0.11f
                val inset = stroke
                val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
                val topLeft = Offset(inset, inset)
                drawArc(
                    color = track, startAngle = 135f, sweepAngle = 270f, useCenter = false,
                    topLeft = topLeft, size = arcSize, style = Stroke(stroke, cap = StrokeCap.Round),
                )
                if (fraction != null) {
                    if (glow) {
                        drawArc(
                            color = glowColor.copy(alpha = 0.22f), startAngle = 135f, sweepAngle = 270f * progress.value,
                            useCenter = false, topLeft = topLeft, size = arcSize,
                            style = Stroke(stroke * 1.7f, cap = StrokeCap.Round),
                        )
                    }
                    drawArc(
                        color = ringColor, startAngle = 135f, sweepAngle = 270f * progress.value,
                        useCenter = false, topLeft = topLeft, size = arcSize,
                        style = Stroke(stroke, cap = StrokeCap.Round),
                    )
                }
            }
            Text(
                if (fraction == null) "—" else center(progress.value),
                style = MaterialTheme.typography.labelLarge,
                color = p.numbers,
                maxLines = 1,
            )
        }
        Text(label, style = MaterialTheme.typography.labelSmall, color = p.indicatorText, maxLines = 1)
    }
}
