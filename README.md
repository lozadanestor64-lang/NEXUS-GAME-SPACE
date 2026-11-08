# NEXUS GAME SPACE

Centro de control gaming para Android, pensado primero para el **Motorola Moto G15 (Android 15)**.
No es un limpiador de RAM: solo muestra y aplica lo que Android permite de verdad, y marca como
«No disponible» lo que no se puede medir.

> **Estado: Fase 2 de 10.** Hay esqueleto, diseño, navegación, panel con datos reales y monitor.
> Perfiles, biblioteca, Shizuku, panel flotante, red y herramientas están planificados (ver `CHANGELOG.md`).

## Qué funciona hoy
- Panel con batería, RAM, almacenamiento, estado térmico del sistema y temperatura de batería (por separado).
- Monitor con información real y sin inventar FPS.
- Pantalla «Acerca de y privacidad».
- La app **no pide ningún permiso** y no usa Internet.

## Requisitos
- Android 8.0 (API 26) o superior. Objetivo: Android 15.
- Para compilar: Android Studio (Ladybug o superior) o JDK 17 + Gradle 8.10.

## Instalar el APK de depuración (sin Android Studio)
1. Sube este proyecto a un repositorio de GitHub.
2. Abre la pestaña **Actions** → ejecución **Android CI** → sección **Artifacts**.
3. Descarga `nexus-game-space-debug-apk`, descomprímelo y copia `app-debug.apk` al teléfono.
4. Ábrelo y permite «instalar apps desconocidas» para tu gestor de archivos cuando Android lo pida.

## Compilar en Android Studio
Abre la carpeta del proyecto, espera la sincronización de Gradle y pulsa **Run**.

## Limitaciones
- No se pueden medir los FPS de juegos de terceros.
- No se tocan CPU, GPU ni límites térmicos.
- Shizuku (Fase 6) será opcional y nunca ejecutará comandos arbitrarios.

## Privacidad
Sin cuentas, sin telemetría, sin publicidad. Todo se guardará localmente.

## Licencia
Apache-2.0. Añade el texto oficial como `LICENSE` (en GitHub: *Add file → Create new file → LICENSE* y elige la plantilla Apache 2.0). 
