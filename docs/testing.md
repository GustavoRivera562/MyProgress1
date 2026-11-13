# Pruebas de MyProgres

## Ejecutar pruebas locales

Desde la raíz del proyecto:

- `gradlew.bat :app:testDebugUnitTest`: pruebas de validación y UI Compose con Robolectric.
- `gradlew.bat :app:jacocoTestReport`: ejecuta las pruebas locales y genera cobertura JaCoCo en `app/build/reports/jacoco/jacocoTestReport/html/index.html`.
- `gradlew.bat :app:connectedDebugAndroidTest`: pruebas instrumentadas de Room y ErrorManager; requiere emulador o dispositivo conectado.

## Registro de resultados

Cada prueba usa `TestErrorLogRule`. Registra inicio y éxito con `[TEST_START]` y `[TEST_PASS]`. Si falla, registra `[TEST_ERROR]`, el nombre de la prueba, el tipo y mensaje de la excepción y su stack trace. Las pruebas instrumentadas también escriben el error en Logcat con el tag `MyProgresTest`.

Gradle conserva los resultados XML y HTML en `app/build/test-results/` y `app/build/reports/tests/`. Los registros de prueba describen errores técnicos; no deben incluir credenciales ni datos reales de usuarios.

## Primera ejecución

- Pasaron 8 pruebas locales: 7 de validación y la prueba unitaria de ejemplo.
- La prueba de pantalla `HomeScreenTest` falla al iniciar `ComponentActivity` con Robolectric antes de evaluar la interfaz. El error y stack trace quedan en el reporte de Gradle; es un problema del entorno de prueba, no una aserción fallida de la pantalla.
- En el HONOR MAGIC 8 Lite (modelo ADB MTN_NX3) pasaron las 3 pruebas instrumentadas: 2 de Room/ErrorManager y 1 de contexto Android; 0 fallos y 0 omitidas. Reporte: `app/build/reports/androidTests/connected/debug/index.html`.
