# Guía de Shizuku

## Qué hace NEXUS GAME SPACE con Shizuku (v0.4.0, parte 1)
Solo **detecta, solicita autorización y diagnostica**. Todavía no ejecuta operaciones privilegiadas.
Cuando se añadan, serán únicamente operaciones de una lista permitida, documentadas y reversibles.
Nunca se ejecutará un comando escrito por el usuario, descargado o recibido de Internet.

## Estados
| Estado | Significado |
|---|---|
| Shizuku no instalado | No existe la app `moe.shizuku.privileged.api`. |
| Shizuku no iniciado | Instalada, pero el servicio (Binder) no responde. |
| Esperando autorización | Servicio activo; falta pulsar SOLICITAR AUTORIZACIÓN. |
| Permiso denegado | Hay que autorizar la app dentro de Shizuku. |
| Conectado | Binder activo y permiso concedido, con una versión de servicio compatible. |
| Conectado sin los privilegios requeridos | Responde, pero la versión no es compatible. |
| Conexión perdida | El Binder desapareció con la app abierta. |
| Error al comprobar | La API lanzó una excepción; el detalle aparece en pantalla. |

«Conectado» nunca se muestra solo porque la app Shizuku esté instalada.

## Entorno
El uid del servicio indica cómo se inició: 2000 = ADB (shell), 0 = root. ADB y root no son equivalentes.

## Activar sin root
1. Instala Shizuku desde su sitio oficial.
2. Activa las opciones de desarrollador y la depuración inalámbrica.
3. En Shizuku, elige «Iniciar mediante depuración inalámbrica» y empareja.
4. En NEXUS GAME SPACE: Más → Shizuku → COMPROBAR SHIZUKU → SOLICITAR AUTORIZACIÓN.
Sin root, tras reiniciar el teléfono hay que iniciar Shizuku de nuevo.

## Detalles técnicos
- Dependencias: `dev.rikka.shizuku:api` y `provider` 13.1.5.
- `ShizukuProvider` declarado en el manifiesto (recibe el Binder).
- Los listeners se registran en el ViewModel y se liberan en `onCleared`.
- No hay sondeo continuo: se lee el estado al abrir la pantalla, al volver a ella y cuando Shizuku avisa.
