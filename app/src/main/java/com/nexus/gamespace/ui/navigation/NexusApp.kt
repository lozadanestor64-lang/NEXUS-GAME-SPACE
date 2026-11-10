package com.nexus.gamespace.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.nexus.gamespace.features.about.AboutScreen
import com.nexus.gamespace.features.booster.BoosterScreen
import com.nexus.gamespace.features.dashboard.DashboardScreen
import com.nexus.gamespace.features.library.AddGamesScreen
import com.nexus.gamespace.features.library.LibraryScreen
import com.nexus.gamespace.features.profiles.ProfileEditScreen
import com.nexus.gamespace.features.profiles.ProfilesScreen
import com.nexus.gamespace.features.shizuku.ShizukuScreen
import com.nexus.gamespace.features.monitor.MonitorScreen
import com.nexus.gamespace.features.more.MoreScreen
import com.nexus.gamespace.features.placeholder.PlannedScreen
import com.nexus.gamespace.features.welcome.WelcomeScreen
import com.nexus.gamespace.ui.theme.NexusBlack
import com.nexus.gamespace.ui.theme.NexusRed

private data class BarItem(val screen: Screen, val icon: ImageVector)

private val barItems = listOf(
    BarItem(Screen.Dashboard, Icons.Filled.Home),
    BarItem(Screen.Library, Icons.Filled.PlayArrow),
    BarItem(Screen.Optimizer, Icons.Filled.Build),
    BarItem(Screen.Monitor, Icons.Filled.Info),
    BarItem(Screen.More, Icons.Filled.Menu),
)

@Composable
fun NexusApp() {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val current = entry?.destination?.route

    Scaffold(
        containerColor = NexusBlack,
        bottomBar = {
            if (current != Screen.Welcome.path) NexusBottomBar(current, nav)
        },
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = Screen.Welcome.path,
            modifier = Modifier.padding(padding),
        ) {
            composable(Screen.Welcome.path) {
                WelcomeScreen(onEnter = {
                    nav.navigate(Screen.Dashboard.path) {
                        popUpTo(Screen.Welcome.path) { inclusive = true }
                    }
                })
            }
            composable(Screen.Dashboard.path) { DashboardScreen() }
            composable(Screen.Library.path) {
                LibraryScreen(onAddGames = { nav.navigate(Screen.AddGames.path) })
            }
            composable(Screen.AddGames.path) { AddGamesScreen(onBack = { nav.popBackStack() }) }
            composable(Screen.Profiles.path) {
                ProfilesScreen(onEdit = { nav.navigate(profileEditPath(it)) })
            }
            composable(
                PROFILE_EDIT_ROUTE,
                arguments = listOf(navArgument("id") { type = NavType.StringType }),
            ) { entry ->
                ProfileEditScreen(
                    profileId = entry.arguments?.getString("id").orEmpty(),
                    onDone = { nav.popBackStack() },
                )
            }
            composable(Screen.Optimizer.path) {
                BoosterScreen(
                    onOpenLibrary = { nav.navigate(Screen.Library.path) { launchSingleTop = true } },
                    onOpenProfiles = { nav.navigate(Screen.Profiles.path) },
                    onOpenShizuku = { nav.navigate(Screen.Shizuku.path) },
                )
            }
            composable(Screen.Monitor.path) { MonitorScreen() }
            composable(Screen.More.path) { MoreScreen(onOpen = { nav.navigate(it.path) }) }
            composable(Screen.Lab.path) {
                PlannedScreen("Laboratorio avanzado", "Funciones experimentales, desactivadas por defecto.", 6,
                    listOf("Resolución y densidad con vista previa y restauración", "Solo si el dispositivo lo admite"))
            }
            composable(Screen.Shizuku.path) { ShizukuScreen() }
            composable(Screen.Network.path) {
                PlannedScreen("Diagnóstico de red", "Pruebas bajo demanda.", 8,
                    listOf("Estado de conexión", "Test de latencia y estabilidad", "Historial y comparación"))
            }
            composable(Screen.Toolbox.path) {
                PlannedScreen("Herramientas", "Utilidades para jugar.", 8,
                    listOf("Temporizador y cronómetro", "Notas por juego", "Prueba táctil y de pantalla", "Diagnóstico de almacenamiento"))
            }
            composable(Screen.Settings.path) {
                PlannedScreen("Configuración general", "Preferencias de la app y permisos.", 6,
                    listOf("Gestor de permisos (nombre, motivo, riesgo, estado)", "Panel flotante opcional", "Restaurar ajustes"))
            }
            composable(Screen.About.path) { AboutScreen() }
        }
    }
}

@Composable
private fun NexusBottomBar(current: String?, nav: NavHostController) {
    NavigationBar(containerColor = NexusBlack) {
        barItems.forEach { item ->
            NavigationBarItem(
                selected = current == item.screen.path,
                onClick = {
                    nav.navigate(item.screen.path) {
                        popUpTo(Screen.Dashboard.path) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(item.icon, contentDescription = item.screen.title) },
                label = { Text(item.screen.title) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = NexusRed,
                    selectedTextColor = NexusRed,
                    indicatorColor = NexusBlack,
                ),
            )
        }
    }
}
