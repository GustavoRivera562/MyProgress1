# Archivos modificados — Validación MyProgres

## Código y pruebas

- `app/src/main/java/com/example/myprogres/core/error/AppResult.kt` — categorías seguras NETWORK, AUTHENTICATION y FIRESTORE; mensajes comprensibles.
- `app/src/main/java/com/example/myprogres/core/error/ErrorManager.kt` — clasifica excepciones de Firebase Auth, Firestore y conectividad; traduce Firestore unavailable/deadline a NETWORK; persiste solo datos sanitizados.
- `app/src/main/java/com/example/myprogres/core/error/CrashReporter.kt` — acepta únicamente el código, no recibe excepción potencialmente sensible.
- `app/src/androidTest/java/com/example/myprogres/RoomAndErrorLoggingTest.kt` — añade pruebas instrumentadas de clasificación y sanitización para red, Auth y Firestore.

## Documentación nueva

- `docs/validation/RESUMEN_VALIDACION.md`
- `docs/validation/MATRIZ_PRUEBAS.md`
- `docs/validation/MANEJO_ERRORES.md`
- `docs/validation/FIREBASE_VALIDACION.md`
- `docs/validation/ARCHIVOS_MODIFICADOS.md`
- `docs/validation/evidencias/ejecuciones_sanitizadas.txt`

No se cambiaron Room, entidades, DAO, esquema, pantallas, UI, reglas Firebase, Gradle, dependencias ni código del SyncCoordinator. No se borraron datos. Los intentos temporales de configuración Robolectric fueron restaurados a su estado previo.
