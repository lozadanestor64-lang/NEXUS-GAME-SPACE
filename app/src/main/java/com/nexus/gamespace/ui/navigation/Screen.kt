package com.nexus.gamespace.ui.navigation

/** Las 12 pantallas de la app. */
enum class Screen(val path: String, val title: String) {
    Welcome("welcome", "Bienvenida"),
    Dashboard("dashboard", "Panel"),
    Library("library", "Juegos"),
    Optimizer("optimizer", "Optimizar"),
    Monitor("monitor", "Monitor"),
    More("more", "Más"),
    GameConfig("game_config", "Configuración por juego"),
    Lab("lab", "Laboratorio avanzado"),
    Shizuku("shizuku", "Shizuku"),
    Network("network", "Diagnóstico de red"),
    Toolbox("toolbox", "Herramientas"),
    Settings("settings", "Configuración general"),
    About("about", "Acerca de y privacidad"),
}
