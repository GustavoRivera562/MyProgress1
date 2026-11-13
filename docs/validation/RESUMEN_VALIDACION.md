# Resumen de validación — MyProgres

**Fecha:** 2026-11-08  
**Alcance:** estabilidad, Room, errores, sincronización y pruebas locales.

## Resultado

Se revisó la implementación existente y se hicieron cambios acotados para clasificar los errores Firebase y evitar que el CrashReporter reciba excepciones completas. La persistencia Room continúa siendo la fuente local inmediata. Se añadieron pruebas instrumentadas de sanitización y clasificación de errores.

La validación queda **parcial**. Debug y Release compilan; las 13 pruebas instrumentadas y las 29 pruebas de reglas pasan. La suite JVM mantiene 8 aprobadas y una fallida: `HomeScreenTest.homeScreen_showsTrainingAndWaterSummary` no puede iniciar `ComponentActivity` en Robolectric antes de ejecutar sus aserciones. No se alteraron ni desactivaron las aserciones.

## Problemas y correcciones

- **Robolectric / Compose:** stack trace termina en `ActivityScenarioRule.before`; Robolectric no resuelve `org.robolectric.default/androidx.activity.ComponentActivity`. La falla ocurre antes de `setContent` y de las aserciones de texto. Se comprobó la declaración de actividad y el manifiesto de prueba, y se intentó incluir el manifiesto de Compose en el classpath de test; estas variantes no resolvieron la causa. Se restauraron esos archivos al estado inicial. No se cambiaron versiones.
- **Clasificación Firebase:** `ErrorManager` asignaba Auth/Firestore al error genérico. Ahora clasifica excepciones de Firebase Auth, Firestore y red con códigos y mensajes seguros; Firestore `UNAVAILABLE` y `DEADLINE_EXCEEDED` se tratan como conectividad.
- **Posible exposición de excepciones:** `CrashReporter` recibía el objeto `Exception`. Su contrato ahora recibe solo el código estable, evitando que una implementación futura registre mensajes de Firebase con detalles privados.

## Resultados reales

| Validación | Resultado |
|---|---|
| `:app:testDebugUnitTest` | 8 aprobadas, 1 fallida (Robolectric descrita arriba). |
| `:app:compileDebugAndroidTestKotlin` | Aprobada. |
| `:app:connectedDebugAndroidTest` | 13 aprobadas en el dispositivo ADB conectado. |
| `:app:assembleDebug` | Aprobada. |
| `:app:assembleRelease` | Aprobada. |
| Reglas Firestore (Node directo contra emulador local) | 29 aprobadas, 0 fallidas. |
| `npm.cmd run test:rules` | No ejecutó: el proceso hijo no encontró `node` en PATH. El mismo archivo de pruebas sí se ejecutó con la ruta explícita de Node. |

Los resultados se resumen en [MATRIZ_PRUEBAS.md](MATRIZ_PRUEBAS.md) y [evidencias/ejecuciones_sanitizadas.txt](evidencias/ejecuciones_sanitizadas.txt).

## Room y sincronización

La inspección confirma que las mutaciones locales se completan en Room y el ViewModel solicita la subida de forma asíncrona. El coordinador consulta PENDING por UID, omite registros sin propietario y no procesa `error_logs`. Sube el entrenamiento antes de las series y luego procesa hidratación. Ante una excepción de escritura remota, la llamada no alcanza la actualización condicional a SYNCED; el registro sigue PENDING. Las actualizaciones locales condicionales evitan marcar como sincronizado un snapshot modificado concurrentemente.

Las pruebas instrumentadas cubren persistencia en base en memoria, propietario, entrenamiento/serie/hidratación, finalización, aislamiento entre usuarios y sanitización de errores. No se simuló una interrupción real de red durante sincronización ni se inspeccionaron transiciones PENDING/SYNCED en una sesión de la app en Emulator.

## Pendientes y limitaciones

1. Resolver la inicialización de la actividad en Robolectric. El test original debe conservarse y sus dos aserciones deben ejecutarse.
2. La app ya está instalada y abierta en el HONOR; inicia sesión manualmente con la cuenta de prueba y avísame cuando la pantalla principal esté visible. Después se validará entrenamiento, dos series, hidratación y estado Room contra Firestore Emulator.
3. Automatizar fallos del SyncCoordinator (sin conexión, permisos, cambio de usuario y reintentos) mediante fake/remoto/emulador; los resultados estáticos no cuentan como prueba de ejecución.
4. Persistencia tras cerrar y reabrir la app y el flujo completo de entrenamiento no se probaron manualmente en esta corrida.
5. Los estados FAILED y CONFLICT están declarados, pero el coordinador actual conserva PENDING ante fallas; no hace transición a esos estados. WorkManager y sincronización bidireccional no forman parte del alcance actual.

**Producción:** no se ejecutó `firebase deploy`, no se modificaron reglas y no se realizaron escrituras en Firestore de producción. La suite de reglas se dirigió a `127.0.0.1:8080`.
