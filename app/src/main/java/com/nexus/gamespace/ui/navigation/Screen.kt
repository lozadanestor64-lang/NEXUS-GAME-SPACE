package com.nexus.gamespace.ui.navigation

/** Las 12 pantallas de la app. */
enum class Screen(val path: String, val title: String) {
    Welcome("welcome", "Bienvenida"),
    Dashboard("dashboard", "Panel"),
    Library("library", "Juegos"),
    Optimizer("optimizer", "Booster"),
    Monitor("monitor", "Monitor"),
    More("more", "Más"),
    Profiles("profiles", "Perfiles"),
    AddGames("add_games", "Añadir juegos"),
    Lab("lab", "Laboratorio avanzado"),
    Shizuku("shizuku", "Shizuku"),
    Network("network", "Diagnóstico de red"),
    Toolbox("toolbox", "Herramientas"),
    Settings("settings", "Configuración general"),
    About("about", "Acerca de y privacidad"),
}

/** Ruta con argumento para editar un perfil. */
const val PROFILE_EDIT_ROUTE = "profile_edit/{id}"

fun profileEditPath(id: String) = "profile_edit/$id"
