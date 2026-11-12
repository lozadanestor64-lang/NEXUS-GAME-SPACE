package com.nexus.gamespace.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.nexus.gamespace.ui.theme.NexusSurfaceHigh
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Icono original de una app instalada. Si no se puede cargar, muestra un recuadro neutro. */
@Composable
fun AppIcon(
    packageName: String,
    size: Dp = 56.dp,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(14.dp),
) {
    val context = LocalContext.current
    val bitmap by produceState<ImageBitmap?>(initialValue = null, packageName) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                context.packageManager.getApplicationIcon(packageName).toBitmap(128, 128).asImageBitmap()
            }.getOrNull()
        }
    }
    val bmp = bitmap
    if (bmp != null) {
        Image(bitmap = bmp, contentDescription = null, modifier = modifier.size(size).clip(shape))
    } else {
        Box(modifier = modifier.size(size).clip(shape).background(NexusSurfaceHigh))
    }
}
