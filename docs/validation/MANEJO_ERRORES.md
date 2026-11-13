# Manejo de errores — MyProgres

## Arquitectura actual

Las operaciones de presentación pasan por `ProgressViewModel`; los resultados se representan con `AppResult.Success` o `AppResult.Failure`. `ErrorManager.execute` traduce excepciones a `AppError`, registra errores técnicos en Room mediante `ErrorLogDao` y entrega al usuario solo `safeMessage`.

`ErrorLogEntity` conserva código, tipo, nivel, operación y mensaje seguro. No guarda correo, contraseña, token, entrada del usuario ni stack trace. La tabla es local; SyncCoordinator no la consulta ni la sube.

## Clasificación

| Tipo | Código | Ejemplos | Mensaje seguro |
|---|---|---|---|
| VALIDATION | INPUT_001 | Validación de entrada | Mensaje de validación controlado por la app |
| DATABASE | DB_001 | SQLiteException | No se pudo guardar; los datos previos siguen disponibles |
| NETWORK | NET_001 | FirebaseNetworkException, IOException, Firestore unavailable/deadline | Datos locales se conservan y se reintentará |
| AUTHENTICATION | AUTH_001 | FirebaseAuthException | No se pudo verificar la sesión |
| FIRESTORE | FS_001 | Permiso/otros errores Firestore | No se pudo sincronizar; los datos siguen locales |
| APPLICATION | APP_001 | Excepción inesperada | Ocurrió un problema; inténtalo nuevamente |

ValidationException no crea registro técnico. CancellationException se vuelve a lanzar para conservar cancelación estructurada. Un error al insertar el log no sustituye el resultado original.

## Protección de datos

- Los textos internos de Firebase no se muestran ni se guardan en safeMessage.
- CrashReporter ahora recibe únicamente un código estable, no la excepción completa.
- ErrorLog usa datos limitados y de diagnóstico. La entidad no tiene campos de credenciales/identidad.
- Las pruebas instrumentadas lanzan excepciones con mensajes sintéticos privados y comprueban que no aparecen en safeMessage.

## Operaciones y limitaciones

- Room es el almacenamiento inmediato; la solicitud de sync se dispara después de que la operación local devuelve éxito.
- Las fallas remotas quedan registradas por ErrorManager; Room conserva PENDING porque el SyncCoordinator solo aplica SYNCED tras ACK y CAS.
- El resultado de la sincronización de fondo no se publica como estado específico en UI; actualmente el ViewModel ignora el AppResult que devuelve ErrorManager para esa llamada y registra el error. Los contadores de pendientes pueden informar que falta sync.
- No hay transición operativa a FAILED o CONFLICT aunque las constantes estén declaradas.
- La causa de Robolectric impide ejecutar la prueba Compose JVM; no se alteraron sus aserciones.

## Pruebas asociadas

Pruebas instrumentadas: fallo inesperado sanitizado, red Firebase, Auth y Firestore. Pruebas JVM de validación: peso negativo y repeticiones fuera de rango. Ver [MATRIZ_PRUEBAS.md](MATRIZ_PRUEBAS.md).
