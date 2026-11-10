# Permisos

La app no pide permisos peligrosos (contactos, SMS, llamadas, micrófono, cámara, ubicación). No usa accesibilidad, VPN, administrador del dispositivo ni `QUERY_ALL_PACKAGES`.

| Permiso | Tipo | Para qué | Cuándo se pide | Cómo revocarlo |
|---|---|---|---|---|
| `ACCESS_NOTIFICATION_POLICY` | Acceso especial | Activar No molestar durante el juego | Al pulsar CONCEDER ACCESO en el Booster | Ajustes → Aplicaciones → Acceso especial → Acceso No molestar |
| `WRITE_SETTINGS` | Acceso especial | Cambiar el brillo | Al pulsar CONCEDER ACCESO en el Booster | Ajustes → Aplicaciones → Acceso especial → Modificar ajustes del sistema |
| Autorización Shizuku | Opcional | Funciones avanzadas (aún sin operaciones) | Al pulsar SOLICITAR AUTORIZACIÓN | En la app Shizuku, lista de aplicaciones autorizadas |

Los dos accesos especiales aparecen declarados en el manifiesto porque Android lo exige para poder concederlos, pero no están concedidos al instalar. La app comprueba el estado real cada vez que vuelves de Ajustes.

## Previstos (no implementados aún)
- `SYSTEM_ALERT_WINDOW`: solo para el panel flotante, al activarlo.
- `POST_NOTIFICATIONS` y servicio en primer plano: para el panel flotante y la grabación.
- MediaProjection: consentimiento del sistema cada vez que se graba.
- `PACKAGE_USAGE_STATS`: solo para detectar el juego activo, opcional.
