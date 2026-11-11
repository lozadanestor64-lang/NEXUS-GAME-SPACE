# NEXUS BOOSTER: panel gaming flotante

El Booster **no es una pestaña ni una pantalla de la app**: es un panel que se dibuja encima del juego.
Se activa y configura en **Más → Configuración general**.

## Estado
| Parte | Estado |
|---|---|
| Servicio `OverlayService` (primer plano, `TYPE_APPLICATION_OVERLAY`) | Escrito; sin compilar ni probar en dispositivo |
| Pestaña lateral (arrastrar, cambiar de lado, deslizar hacia dentro, ocultar) | Escrita; sin probar |
| Panel expandido (vertical y horizontal) | Escrito; sin probar |
| Configuración (lado, opacidad, tamaño, herramientas, permisos) | Escrita; sin probar |
| Captura de pantalla y grabación (MediaProjection) | No implementadas |
| Detección automática del juego activo y perfil por juego en sesión | No implementadas |

**La superposición no se da por terminada** hasta verificarla en el Moto G15 o en un emulador compatible.

## Cómo funciona
- **Activar:** Configuración → CONCEDER PERMISO DE SUPERPOSICIÓN (Ajustes de Android) → ACTIVAR PANEL FLOTANTE. En Android 13+ pide también notificaciones (el panel funciona aunque lo rechaces).
- **Pestaña:** toca para abrir, o desliza hacia el interior de la pantalla. Arrástrala para moverla; si la sueltas pasada la mitad de la pantalla cambia de lado.
- **Panel:** botón «⇄ LADO», «OCULTAR» (vuelve con «Mostrar» en la notificación) y «VOLVER AL JUEGO». Tocar fuera también lo cierra.
- **Detener:** DESACTIVAR en Configuración o «Detener» en la notificación.
- El servicio se inicia solo cuando el usuario lo activa y es `START_NOT_STICKY`: si Android lo cierra no se reinicia solo.

## Herramientas del panel (todas ocultables)
| Herramienta | Qué hace de verdad | Restauración |
|---|---|---|
| Monitor | Batería, RAM libre, estado térmico, temperatura de batería, tipo de conexión, SoC, frecuencia de pantalla (rotulada «no son FPS»), estado de Shizuku. Lee cada 2 s solo con el panel abierto. | — |
| Perfiles | Cambia el perfil activo (guardado). Aún no cambia ajustes del sistema por sí solo. | — |
| Apps favoritas | Iconos de los juegos con estrella; tocar uno lo abre. | — |
| No molestar | `setInterruptionFilter(PRIORITY)` con acceso especial. | Guarda y restaura el filtro original |
| Brillo | Brillo manual con `WRITE_SETTINGS`. | Guarda y restaura valor y modo |
| Volumen | Subir/bajar y abrir el panel de volumen del sistema. | — |
| Rotación | Activa o bloquea la rotación automática con `WRITE_SETTINGS`. | Guarda y restaura |
| Panel Wi-Fi | Abre el panel de Internet de Android (las apps no pueden cambiar el Wi-Fi). | — |
| Temporizador | Cronómetro (iniciar, pausar, reiniciar). | — |
| Notas | Texto guardado en el teléfono. Para escribir, la ventana toma el foco y algunos juegos se pausan. | — |
| Biblioteca | Abre NEXUS GAME SPACE en la biblioteca. | — |

No se muestran: ventilador, frecuencias desbloqueadas, FPS ni controles de otros fabricantes. CPU y GPU aparecen como «No disponible» donde Android no ofrece una lectura fiable.

## Limitaciones conocidas
- Algunos juegos pueden pausarse al perder el foco (solo al escribir una nota).
- En Android 12+ las ventanas superpuestas pueden bloquear toques en la zona que ocupan; el panel solo ocupa su propio rectángulo.
- Fabricantes con ahorro de batería agresivo pueden cerrar el servicio; en ese caso hay que activarlo de nuevo.
- Captura y grabación: Android solo permite capturar otras apps con MediaProjection y consentimiento; fase posterior.

## Archivos nuevos
`overlay/OverlayService.kt`, `overlay/OverlayPanel.kt`, `booster/BoosterController.kt`, `domain/Overlay.kt`, `domain/Stopwatch.kt`, `core/BoosterActions.kt`, `core/NetworkInfoProvider.kt`, `features/settings/SettingsScreen.kt`, `features/settings/SettingsViewModel.kt`, `res/drawable/ic_stat_nexus.xml`, `OverlayGesturesTest.kt`, `BoosterLogicTest.kt`.

## Archivos modificados
`data/NexusRepository.kt`, `MainActivity.kt` (ruta pedida desde el panel), `ui/navigation/NexusApp.kt` y `Screen.kt` (Configuración ahora es real; **Optimizar vuelve a ser la pantalla preparada original**), `features/dashboard/DashboardScreen.kt` (estado del panel), `AndroidManifest.xml`, `features/about/AboutScreen.kt`.

Shizuku Center, la biblioteca, los perfiles y el Monitor no cambian.
