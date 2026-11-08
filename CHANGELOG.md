# Cambios

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
