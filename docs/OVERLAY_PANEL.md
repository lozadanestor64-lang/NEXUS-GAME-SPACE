# NEXUS BOOSTER: panel gaming flotante

El Booster **no es una pestaña ni una pantalla de la app**: es un panel que se dibuja encima del juego.
Se activa y configura en **Más → Configuración general**.

## Estado por grupos
| Grupo | Contenido | Estado |
|---|---|---|
| 0 | Servicio, pestaña lateral, panel, herramientas básicas | Escrito; el panel ya se mostró en un teléfono Samsung (Android 14) |
| 1 | Rediseño compacto, botón +, AGREGAR JUEGOS, favoritos circulares y reordenables, animaciones, CONFIGURACIÓN DEL PANEL (tamaño, posición, animaciones) | **Escrito en esta entrega; sin compilar ni probar** |
| 2a | MIRA (retícula cosmética) | **Escrita en esta entrega; sin compilar ni probar** |
| 2b | **COLOR STUDIO** (corrige el texto negro sobre negro) | **Escrito en esta entrega; sin compilar ni probar** |
| 2c | RGB MODE y Chroma | No empezado |
| 3 | Estilos REDMAGIC PANEL y XIAOMI PANEL | No empezado |
| 4 | Grabación de pantalla y captura (MediaProjection), tarjetas reducibles individualmente | No empezado |

No se ha probado en un Motorola Moto G15 ni con Android 15. La superposición no se da por terminada hasta verificarla allí.

## Grupo 1: qué cambia
- **Diseño compacto:** tipografía y tarjetas más pequeñas, botones propios de altura reducida, textos de una línea con puntos suspensivos en lugar de partirse. Encabezado y botón VOLVER AL JUEGO fuera del área que se desplaza.
- **Horizontal:** tres zonas (favoritos, perfiles, temporizador, notas y biblioteca a la izquierda; monitor al centro; controles a la derecha). **Vertical:** una columna con desplazamiento.
- **Botón + (circular, arriba):** abre AGREGAR JUEGOS dentro del panel. Lista las apps con actividad lanzable (`PackageManager`, visibilidad limitada de paquetes, sin `QUERY_ALL_PACKAGES`) con su icono original, buscador y selección múltiple. Al pulsar AGREGAR se vuelve al panel y los iconos ya aparecen en Favoritos.
- **Favoritos:** fila permanente con iconos en círculo. Tocar uno abre el juego. EDITAR muestra ◀ ✕ ▶ para reordenar o quitar (quitar no desinstala ni borra el juego de la biblioteca). Se guardan en el mismo almacenamiento que la biblioteca de la app, con su orden, y sobreviven a reinicios.
- **Anillos animados:** batería, RAM libre y estado térmico. Al abrir crecen desde cero hasta el valor real; cuando el dato cambia se animan desde el valor anterior. El anillo térmico muestra el nivel del sistema por pasos (Normal, Atención, Elevado, Crítico), no una temperatura de CPU. No hay animaciones infinitas.
- **Animaciones:** Desactivadas, Reducidas, Lentas, Normales o Rápidas.
- **Engranaje (arriba):** abre CONFIGURACIÓN DEL PANEL con tamaño (Compacto, Normal, Grande o Personalizado con control de tamaño global, ancho, alto máximo, texto, iconos, espaciado, márgenes y opacidad), posición (lado) y animaciones. El propio panel hace de vista previa en tiempo real. Todo se guarda con DataStore.

## Grupo 2b: COLOR STUDIO y corrección del texto negro
**Causa del error:** en el panel flotante no hay un contenedor que fije el color del contenido, así que todo `Text` sin color explícito usaba el negro por defecto de Compose y se perdía sobre el fondo oscuro.

**Corrección:** el panel fija ahora un color de contenido y un tema de Material 3 construidos desde una paleta central (`OvPalette`). Todos los componentes leen sus colores de ahí, incluidos chips, interruptores, casillas, deslizadores, campos de texto, botones, iconos y anillos. Los textos del panel llevan siempre un color explícito de la paleta.

**Color Studio** (engranaje → COLOR STUDIO): 24 colores personalizables en tres grupos.
- *Texto:* títulos, principal, secundario, descripciones, botones, controles, indicadores, números, valores de monitorización y acento.
- *Fondos:* panel completo, configuración, tarjetas, botones, menús y ventanas emergentes (AGREGAR JUEGOS).
- *Otros:* bordes, iconos, barras, anillos, indicadores e interruptores, botones activos, iluminación y efectos de selección.
- Selector con código HEX, deslizadores de matiz, saturación y brillo, y 16 colores rápidos.
- 6 paletas predeterminadas (NEXUS rojo, Azul hielo, Verde matrix, Morado neón, Naranja fuego y Alto contraste) y restaurar un color o todos.
- Vista previa en directo: el propio panel cambia al instante. Se guarda con DataStore y afecta a todas las pantallas del overlay, incluida la pestaña lateral.
- **Contraste:** avisos cuando un texto tiene menos de 4,5:1 sobre su fondo y **contraste automático** (activado por defecto) que sustituye por blanco o negro cualquier texto por debajo de 3:1. Así ningún texto queda invisible aunque el usuario elija mal.
- Las alertas térmicas mantienen sus colores de alerta (verde, ámbar, rojo) y no se pueden personalizar.

Limitaciones: la pantalla principal de la app (fuera del overlay) conserva su tema; el RGB, los estilos REDMAGIC/XIAOMI y las herramientas de voz y sonido llegan en entregas siguientes.

## Grupo 2a: MIRA
Herramienta «Mira» en el panel (interruptor + AJUSTAR MIRA). Es una ventana pequeña, **no táctil**, en el centro de la pantalla que solo dibuja formas fijas.
- **Diseños:** punto, cruz, círculo, cruz con punto, círculo con punto y personalizada (punto, cruz y círculo a elección).
- **Personalización:** color (8 colores; el selector completo llega con el grupo 2b), tamaño, grosor, separación central, transparencia y posición, con vista previa.
- **Calibración:** botones ◀ ▲ ▼ ▶ con paso de 1, 5 o 20 dp y CENTRAR. La mira real se mueve a la vez si está activada.
- **Por juego:** puedes elegir «Global» o uno de tus favoritos y guardar una mira propia; «USAR LA MIRA GLOBAL» quita la propia. También puedes **excluir** un juego para que la mira no aparezca.
- **Persistencia:** todo se guarda en el teléfono (DataStore).
- **Cosmética:** no detecta enemigos ni objetivos, no lee imágenes del juego, no automatiza disparos y no inyecta toques. La pantalla avisa de que algunos juegos competitivos prohíben las superposiciones externas.

Limitaciones de la mira:
- Sin el acceso de uso no se sabe qué juego está en primer plano, así que la mira (y las exclusiones) de un juego se aplican **cuando lo abres desde NEXUS GAME SPACE** (panel o biblioteca). Si abres el juego por otro camino sigue la última aplicada.
- En Android 12+ la ventana se limita a 80 % de opacidad para que no bloquee tus toques; la transparencia que eliges se aplica encima de eso.
- Algunos juegos o sistemas anticheat pueden ocultar o bloquear superposiciones externas.
- Aún no se ha comprobado cómo se ve sobre un juego real ni en el Moto G15.

## Limitaciones conocidas del grupo 1
- Los ajustes de tamaño se aplican al soltar el control, no mientras se arrastra.
- «Juego activo» sigue en «Sin detectar»: requiere el acceso de uso, que no está implementado.
- CPU y GPU no tienen lectura fiable en Android sin privilegios: aparecen como «No disponible».
- Escribir (notas, buscador de AGREGAR JUEGOS) hace que la ventana tome el foco, y algunos juegos se pausan mientras tanto.
- La opción de reducir tarjetas individualmente llega en el grupo 4.

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
`overlay/OverlayService.kt`, `overlay/OverlayPanel.kt`, `overlay/OverlayUi.kt`, `overlay/OverlayAddGames.kt`, `overlay/OverlayPanelSettings.kt`, `overlay/OverlayCrosshair.kt`, `overlay/OverlayPalette.kt`, `overlay/OverlayColorStudio.kt`, `domain/OverlayColors.kt`, `overlay/CrosshairView.kt`, `domain/Crosshair.kt`, `booster/BoosterController.kt`, `domain/Overlay.kt`, `domain/Stopwatch.kt`, `core/BoosterActions.kt`, `core/NetworkInfoProvider.kt`, `features/settings/SettingsScreen.kt`, `features/settings/SettingsViewModel.kt`, `res/drawable/ic_stat_nexus.xml`, `OverlayGesturesTest.kt`, `BoosterLogicTest.kt`, `FavoritesAndPrefsTest.kt`, `CrosshairTest.kt`, `ColorStudioTest.kt`.

## Archivos modificados
`data/NexusRepository.kt`, `data/JsonCodec.kt`, `domain/GameEntry.kt`, `domain/Overlay.kt`, `ui/components/AppIcon.kt`, `ui/theme/Theme.kt`, `MainActivity.kt` (ruta pedida desde el panel), `ui/navigation/NexusApp.kt` y `Screen.kt` (Configuración ahora es real; **Optimizar vuelve a ser la pantalla preparada original**), `features/dashboard/DashboardScreen.kt` (estado del panel), `AndroidManifest.xml`, `features/about/AboutScreen.kt`.

Shizuku Center, la biblioteca, los perfiles y el Monitor no cambian.
