package com.nexus.gamespace.domain

/** Un juego que el usuario eligió para su biblioteca. Quitarlo no desinstala nada. */
data class GameEntry(
    val packageName: String,
    val label: String,
    val favorite: Boolean = false,
    val profileId: String? = null,
    val addedAt: Long = 0L,
    val lastLaunchedAt: Long? = null,
)

/** App con actividad lanzable detectada en el teléfono. */
data class InstalledApp(
    val packageName: String,
    val label: String,
    /** La app se declara a sí misma como juego en su manifiesto. Es solo una pista. */
    val suggestedGame: Boolean,
)
