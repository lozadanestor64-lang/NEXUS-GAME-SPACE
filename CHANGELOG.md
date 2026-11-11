# Cambios

## 0.4.0 (en desarrollo) — NEXUS BOOSTER: panel flotante sobre el juego
- Corrección de diseño: el Booster **no** es una pestaña ni una pantalla. Se retira la pantalla Booster y «Optimizar» vuelve a ser la pantalla preparada original.
- Nuevo `OverlayService` (servicio en primer plano, `TYPE_APPLICATION_OVERLAY`): pestaña lateral arrastrable que cambia de lado, se abre con toque o deslizando hacia dentro, y panel expandido para vertical y horizontal con animación.
- Herramientas del panel: monitor, perfiles, apps favoritas, No molestar, brillo, volumen, rotación, panel Wi-Fi, temporizador, notas y biblioteca. Los cambios temporales guardan el valor original y se restauran.
- Configuración general ahora es real: activar/desactivar el panel, lado, opacidad, tamaño, herramientas visibles, estado de permisos y restaurar cambios.
- El Panel muestra si el panel flotante está activo.
- No probado en dispositivo. Captura y grabación (MediaProjection) pendientes.
- 20 pruebas unitarias nuevas entre gestos, cronómetro y restauración.

## 0.4.0 (en desarrollo) — parte 1: Shizuku
- Módulo `shizuku/` con la API oficial (dev.rikka.shizuku 13.1.5): detección de instalación, servicio (Binder), autorización, versión y entorno (ADB/root), listeners de Binder recibido/perdido y resultado de permiso, diagnóstico y registro de eventos.
- Pantalla SHIZUKU CENTER con COMPROBAR, SOLICITAR AUTORIZACIÓN, ABRIR SHIZUKU, ACTUALIZAR ESTADO y VER INSTRUCCIONES. Se vuelve a comprobar al regresar a la app.
- El Panel muestra el estado real de Shizuku.
- Corrección: los colores no seleccionados del editor de perfiles dibujaban un borde fino.
- 14 pruebas unitarias nuevas del resolvedor de estado.
- Aún no se ejecuta ninguna operación privilegiada.
- Pendiente de la 0.4.0: motor de sesiones, perfiles por juego, motor del optimizador, Game Mode, monitor por proveedores, Permission Center, historial.

## 0.3.0 (en desarrollo) — biblioteca y perfiles
- Biblioteca de juegos real: detecta apps lanzables con visibilidad limitada de paquetes (sin QUERY_ALL_PACKAGES), iconos y nombres originales, búsqueda, favoritos, recientes, añadir y quitar sin desinstalar, iniciar juego, perfil por juego.
- Perfiles Performance, Balanced, Battery Saver y Custom con edición, guardado, cancelar y restaurar predeterminados. Persisten con DataStore.
- Panel: perfil activo y número de juegos reales.
- Pruebas unitarias del guardado (JsonCodec).
- Pendiente de esta versión: optimizador, sesiones, Shizuku, permisos, monitor ampliado, panel flotante, red.

## 0.2.0 — Fase 2
- Proyecto Gradle (Kotlin 2.0.21, Compose, Material 3), tema negro/rojo.
- Navegación con las pantallas preparadas, Panel y Monitor con datos reales.
- GitHub Actions: pruebas, lint y APK de depuración.
