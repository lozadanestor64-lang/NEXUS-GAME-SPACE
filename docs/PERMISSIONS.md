# Permisos

La app no pide permisos peligrosos (contactos, SMS, llamadas, micrófono, cámara, ubicación). No usa accesibilidad, VPN, administrador del dispositivo ni `QUERY_ALL_PACKAGES`.

| Permiso | Tipo | Para qué | Cuándo se pide | Cómo revocarlo |
|---|---|---|---|---|
| `SYSTEM_ALERT_WINDOW` | Acceso especial | Dibujar el panel flotante sobre juegos | Al pulsar CONCEDER PERMISO DE SUPERPOSICIÓN en Configuración | Ajustes → Aplicaciones → Acceso especial → Mostrar sobre otras apps |
| `ACCESS_NOTIFICATION_POLICY` | Acceso especial | No molestar desde el panel | Al pulsar CONCEDER ACCESO en el panel o en Configuración | Ajustes → Acceso especial → No molestar |
| `WRITE_SETTINGS` | Acceso especial | Brillo y rotación desde el panel | Al pulsar CONCEDER ACCESO | Ajustes → Acceso especial → Modificar ajustes del sistema |
| `POST_NOTIFICATIONS` | Runtime (Android 13+) | Aviso «panel activo» | Al activar el panel | Ajustes de notificaciones de la app |
| `FOREGROUND_SERVICE` y `FOREGROUND_SERVICE_SPECIAL_USE` | Normal (al instalar) | Mantener el panel mientras juegas | Se instalan con la app; el servicio solo arranca al activar el panel | Desactivar el panel |
| `ACCESS_NETWORK_STATE` | Normal (al instalar) | Mostrar si hay Wi-Fi o datos | Al instalar | — |
| Autorización Shizuku | Opcional | Funciones avanzadas (aún sin operaciones) | Al pulsar SOLICITAR AUTORIZACIÓN | En la app Shizuku |

Los accesos especiales aparecen declarados en el manifiesto porque Android lo exige para poder concederlos, pero **no están concedidos al instalar**. La app comprueba el estado real cada vez que vuelves de Ajustes.

Foreground service: tipo `specialUse` (obligatorio en Android 14+), con la propiedad `PROPERTY_SPECIAL_USE_FGS_SUBTYPE`. Se inicia únicamente desde una acción del usuario en la app.

## Previstos (no implementados)
- MediaProjection: consentimiento del sistema cada vez que se graba.
- `PACKAGE_USAGE_STATS`: solo para detectar el juego activo, opcional.
