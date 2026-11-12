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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
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
import com.nexus.gamespace.ui.theme.NexusBorder
import com.nexus.gamespace.ui.theme.NexusRed
import com.nexus.gamespace.ui.theme.NexusSurfaceHigh
import com.nexus.gamespace.ui.theme.NexusTextSecondary
import kotlin.math.roundToInt

/** Valores de diseño que el panel reparte a sus componentes. */
val LocalOverlaySpacing = compositionLocalOf { 8.dp }
val LocalIconScale = compositionLocalOf { 1f }
val LocalAnimation = compositionLocalOf { AnimationMode.NORMAL }

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

@Composable
fun OvCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val spacing = LocalOverlaySpacing.current
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color(0xFF121217).copy(alpha = 0.9f))
            .border(1.dp, NexusBorder, shape)
            .padding(spacing),
        verticalArrangement = Arrangement.spacedBy(spacing * 0.75f),
    ) {
        if (title != null) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
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
    val shape = RoundedCornerShape(10.dp)
    val bg = when {
        !enabled -> NexusSurfaceHigh
        filled -> NexusRed
        else -> Color.Transparent
    }
    val fg = if (enabled) Color.White else NexusTextSecondary
    Box(
        modifier = modifier
            .heightIn(min = 34.dp)
            .clip(shape)
            .background(bg)
            .then(if (!filled) Modifier.border(1.dp, if (enabled) NexusRed else NexusBorder, shape) else Modifier)
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

/** Botón circular perfecto con contenido propio (icono o texto corto). */
@Composable
fun OvRoundButton(
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    filled: Boolean = false,
    size: Dp = 32.dp,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(if (filled) NexusRed else Color.Transparent)
            .then(if (!filled) Modifier.border(1.dp, NexusRed.copy(alpha = 0.8f), CircleShape) else Modifier)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
fun OvStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = NexusTextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
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
    var current by remember(value) { mutableFloatStateOf(value.toFloat()) }
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text("${current.roundToInt()} $unit", style = MaterialTheme.typography.bodyMedium, color = NexusTextSecondary)
        }
        Slider(
            value = current,
            onValueChange = { current = it },
            onValueChangeFinished = { onFinished(current.roundToInt()) },
            valueRange = range.first.toFloat()..range.last.toFloat(),
        )
    }
}

/**
 * Anillo indicador. Al abrirse crece desde cero hasta el valor real y, cuando el dato cambia, se anima
 * desde el valor anterior al nuevo. Con las animaciones desactivadas salta directo al valor.
 * fraction == null significa «no disponible»: se dibuja solo la pista.
 */
@Composable
fun OvRing(
    fraction: Float?,
    label: String,
    center: (Float) -> String,
    color: Color,
    modifier: Modifier = Modifier,
    ringSize: Dp = 62.dp,
) {
    val mode = LocalAnimation.current
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

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(ringSize), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(ringSize)) {
                val stroke = this.size.minDimension * 0.11f
                val inset = stroke
                val arcSize = Size(this.size.width - inset * 2, this.size.height - inset * 2)
                val topLeft = Offset(inset, inset)
                drawArc(
                    color = NexusBorder, startAngle = 135f, sweepAngle = 270f, useCenter = false,
                    topLeft = topLeft, size = arcSize, style = Stroke(stroke, cap = StrokeCap.Round),
                )
                if (fraction != null) {
                    if (glow) {
                        drawArc(
                            color = color.copy(alpha = 0.22f), startAngle = 135f, sweepAngle = 270f * progress.value,
                            useCenter = false, topLeft = topLeft, size = arcSize,
                            style = Stroke(stroke * 1.7f, cap = StrokeCap.Round),
                        )
                    }
                    drawArc(
                        color = color, startAngle = 135f, sweepAngle = 270f * progress.value,
                        useCenter = false, topLeft = topLeft, size = arcSize,
                        style = Stroke(stroke, cap = StrokeCap.Round),
                    )
                }
            }
            Text(
                if (fraction == null) "—" else center(progress.value),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
        }
        Text(label, style = MaterialTheme.typography.labelSmall, color = NexusTextSecondary, maxLines = 1)
    }
}
