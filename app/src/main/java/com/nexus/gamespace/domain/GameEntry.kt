package com.nexus.gamespace.domain

/** Un juego que el usuario eligió para su biblioteca. Quitarlo no desinstala nada. */
data class GameEntry(
    val packageName: String,
    val label: String,
    val favorite: Boolean = false,
    val profileId: String? = null,
    val addedAt: Long = 0L,
    val lastLaunchedAt: Long? = null,
    /** Posición en la fila de favoritos del panel (1 = primero). */
    val favoriteOrder: Int = 0,
)

/** App con actividad lanzable detectada en el teléfono. */
data class InstalledApp(
    val packageName: String,
    val label: String,
    /** La app se declara a sí misma como juego en su manifiesto. Es solo una pista. */
    val suggestedGame: Boolean,
)

/** Operaciones sobre la fila de favoritos. Son funciones puras para poder probarlas. */
object FavoritesLogic {

    fun ordered(list: List<GameEntry>): List<GameEntry> =
        list.filter { it.favorite }
            .sortedWith(compareBy<GameEntry> { if (it.favoriteOrder == 0) Int.MAX_VALUE else it.favoriteOrder }
                .thenBy { it.label.lowercase() })

    /** Añade apps como favoritas al final de la fila. Si ya estaban en la biblioteca, solo las marca. */
    fun addFavorites(list: List<GameEntry>, apps: List<InstalledApp>, now: Long): List<GameEntry> {
        var next = list.filter { it.favorite }.maxOfOrNull { it.favoriteOrder } ?: 0
        var result = list
        apps.forEach { app ->
            val existing = result.firstOrNull { it.packageName == app.packageName }
            if (existing != null && existing.favorite) return@forEach
            next += 1
            result = if (existing != null) {
                result.map { if (it.packageName == app.packageName) it.copy(favorite = true, favoriteOrder = next) else it }
            } else {
                result + GameEntry(app.packageName, app.label, favorite = true, addedAt = now, favoriteOrder = next)
            }
        }
        return normalize(result)
    }

    /** Quita la app de favoritos sin quitarla de la biblioteca. */
    fun removeFavorite(list: List<GameEntry>, packageName: String): List<GameEntry> =
        normalize(list.map { if (it.packageName == packageName) it.copy(favorite = false) else it })

    /** Mueve un favorito delta posiciones (-1 = hacia el inicio, +1 = hacia el final). */
    fun move(list: List<GameEntry>, packageName: String, delta: Int): List<GameEntry> {
        val favs = ordered(list)
        val from = favs.indexOfFirst { it.packageName == packageName }
        val to = from + delta
        if (from < 0 || to !in favs.indices) return list
        val reordered = favs.toMutableList().apply { add(to, removeAt(from)) }
        val positions = reordered.mapIndexed { i, g -> g.packageName to i + 1 }.toMap()
        return list.map { if (it.favorite) it.copy(favoriteOrder = positions[it.packageName] ?: it.favoriteOrder) else it }
    }

    private fun normalize(list: List<GameEntry>): List<GameEntry> {
        val positions = ordered(list).mapIndexed { i, g -> g.packageName to i + 1 }.toMap()
        return list.map { if (it.favorite) it.copy(favoriteOrder = positions[it.packageName] ?: 0) else it }
    }
}
