# NEXUS BOOSTER PANEL

## Fases
| Fase | Contenido | Estado |
|---|---|---|
| 1 | BOOSTER CENTER dentro de la app (reemplaza «Optimizar») | Escrito; compilación y prueba en teléfono pendientes |
| 2 | FLOATING BOOSTER: pestaña lateral con WindowManager y `TYPE_APPLICATION_OVERLAY` | No empezada |
| 3 | Grabación de pantalla con MediaProjection | No empezada |
| 4 | Temporizador, notas y monitores dentro de la pestaña flotante | No empezada |

La superposición no se dará por terminada hasta probarla en un dispositivo físico o un emulador compatible.

## Fase 1: qué hace cada acceso
| Acceso | Funcionamiento real | Restauración |
|---|---|---|
| Perfil (Performance/Balanced/Battery Saver) | Cambia el perfil activo (guardado). Aún no aplica ajustes del sistema. | — |
| No molestar | `NotificationManager.setInterruptionFilter(PRIORITY)`. Requiere el acceso especial de No molestar. | Guarda el filtro original y lo restaura. |
| Brillo | `Settings.System` (brillo manual). Requiere el permiso especial de modificar ajustes. | Guarda valor y modo originales y los restaura. |
| Volumen | `AudioManager.adjustStreamVolume(..., FLAG_SHOW_UI)`: abre el panel del sistema. | — |
| Conectividad | Abre el panel de Internet del sistema (`Settings.Panel`). Android no deja cambiar el Wi-Fi desde apps. | — |
| Temporizador | Cronómetro propio (iniciar, pausar, reiniciar). Se pierde si Android cierra la app. | — |
| Notas | Se guardan con DataStore, en el teléfono. | — |
| Monitores | Batería, RAM libre, estado térmico y temperatura de batería (APIs públicas), cada 2 s solo con la pantalla visible. | — |
| Sesión | Muestra «Sin sesión activa»: el motor de sesiones no está hecho. | — |

Los valores originales se guardan en DataStore **antes** de cambiar algo y solo la primera vez, así que sobreviven si Android cierra la app y no se sobrescriben con un valor ya modificado.

## Archivos nuevos
- `domain/Stopwatch.kt` (cronómetro, formato de tiempo, cálculo de brillo, estado de restauración)
- `core/BoosterActions.kt`
- `features/booster/BoosterViewModel.kt`
- `features/booster/BoosterScreen.kt`
- `app/src/test/.../BoosterLogicTest.kt`
- `docs/BOOSTER_PANEL.md`, `docs/PERMISSIONS.md`

## Archivos modificados
- `data/NexusRepository.kt`: valores originales, notas.
- `ui/navigation/NexusApp.kt`: la ruta «optimizer» abre ahora BoosterScreen.
- `ui/navigation/Screen.kt`: la pestaña pasa a llamarse «Booster».
- `AndroidManifest.xml`: dos permisos especiales opcionales.
- `features/about/AboutScreen.kt`: texto de privacidad actualizado.
- `CHANGELOG.md`, `README.md`.

`Shizuku Center`, la biblioteca y los perfiles se conservan sin cambios.
