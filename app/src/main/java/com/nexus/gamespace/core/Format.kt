package com.nexus.gamespace.core

import java.util.Locale

/** Convierte bytes a GB (base 1024) con un decimal, p. ej. "3,8 GB". */
fun formatGb(bytes: Long): String =
    String.format(Locale.getDefault(), "%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0))

const val NOT_AVAILABLE = "No disponible"
