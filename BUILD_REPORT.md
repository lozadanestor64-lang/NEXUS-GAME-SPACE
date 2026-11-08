# Informe de compilación

**Estado: sin verificar.** El entorno donde se escribe el código no tiene el SDK de Android ni acceso a la red, por lo que:
- No se ejecutó Gradle (compilación, pruebas, lint) fuera de GitHub Actions.
- No se probó en emulador ni en un Motorola Moto G15 físico.

La verificación real la hace GitHub Actions (`.github/workflows/android.yml`): pruebas unitarias, lint y APK de depuración.
Los resultados de esas ejecuciones deben copiarse aquí cuando existan.

| Elemento | Estado |
|---|---|
| Compilación Debug | Pendiente de confirmar en Actions |
| Pruebas unitarias | Pendiente de confirmar en Actions |
| Lint | Pendiente de confirmar en Actions |
| Pruebas instrumentadas | No existen todavía |
| Moto G15 físico | No probado |
