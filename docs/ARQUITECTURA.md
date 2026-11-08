# Arquitectura (Fase 2)

Un solo módulo `app` organizado por paquetes (se podrá dividir en módulos Gradle más adelante):

- `domain/`: modelos puros de Kotlin (`DeviceSnapshot`, `ThermalLevel`). Sin Android.
- `core/`: acceso a APIs de Android (`DeviceInfoProvider`) y utilidades.
- `features/<función>/`: pantalla + ViewModel por función.
- `ui/`: tema, componentes y navegación.

Reglas: APIs públicas, sin permisos innecesarios, `null` = «No disponible», estados térmicos
derivados de `PowerManager.THERMAL_STATUS_*`. Inyección de dependencias manual por ahora (sin Hilt).

## Firma de producción
Se crea una keystore local y se referencia desde `keystore.properties` (ignorado por Git). Nunca se sube.
