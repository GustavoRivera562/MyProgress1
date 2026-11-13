# Matriz de pruebas — MyProgres

Evidencia resumida en [ejecuciones_sanitizadas.txt](evidencias/ejecuciones_sanitizadas.txt).

| ID | Escenario | Resultado esperado | Resultado obtenido | Estado | Evidencia |
|---|---|---|---|---|---|
| U01 | HomeScreen muestra resumen | Se ven entrenamiento y 500 ml | Robolectric no resuelve ComponentActivity; no ejecuta aserciones | FALLIDO | Gradle: `:app:testDebugUnitTest`, stack en ActivityScenarioRule |
| U02 | Validaciones de peso y repeticiones | Rechazo de peso negativo y repeticiones inválidas | Suite JVM pasó 8 pruebas en total; incluye validador de entrada | APROBADO | `:app:testDebugUnitTest` (8 aprobadas) |
| L01 | Guardar entrenamiento/series/hidratación y filtrar por UID | Room conserva valores; otros usuarios no los leen | Prueba Room en base en memoria pasó | APROBADO | `:app:connectedDebugAndroidTest` |
| L02 | Finalizar entrenamiento y reflejar historial | Historial contiene entrenamiento y sus series | Cubierto por prueba de repositorio instrumentada | APROBADO | `:app:connectedDebugAndroidTest` |
| E01 | Firebase no disponible | Registrar en Room sin esperar Firestore | La separación Room-first se verificó por inspección; no se inyectó caída real en este caso | NO EJECUTADO | Requiere prueba de fallo remoto |
| E02 | Sin Internet | Persistencia local sin pérdida | No se desconectó red ni se reinició app durante el caso | NO EJECUTADO | HONOR quedó en AOD/bloqueo |
| E03 | Error al guardar en Room | No mostrar éxito | ErrorManager devuelve Failure; no se forzó DAO cerrado ni se probó ViewModel en este caso | NO EJECUTADO | Requiere fake de Repository/DAO y aserción de UI |
| E04 | Error al sincronizar | Registro queda PENDING | Semántica de escritura seguida de CAS inspeccionada; no se inyectó fallo remoto | NO EJECUTADO | Requiere fake de Firestore |
| E05 | Reintento | No duplica documentos | IDs deterministas y escritura .set inspeccionados; prueba de reintento de app no ejecutada | NO EJECUTADO | Requiere prueba SyncCoordinator |
| E06 | Sin sesión | No hacer escrituras remotas | Coordinador obtiene UID desde FirebaseAuth y retorna sin usuario; no ejecutado como prueba aislada | NO EJECUTADO | Requiere fake Auth |
| E07 | Cambio de usuario durante envío | No mezclar datos | Chequeos de UID antes/después inspeccionados; carrera no simulada | NO EJECUTADO | Requiere fake Auth controlable |
| E08 | Peso negativo | Entrada rechazada | Prueba de validador en suite JVM aprobada | APROBADO | `:app:testDebugUnitTest` |
| E09 | Repeticiones inválidas | Entrada rechazada | Prueba de repeticiones cero en suite JVM aprobada | APROBADO | `:app:testDebugUnitTest` |
| E10 | Excepción inesperada | Error local sanitizado, sin cierre en prueba | Prueba instrumentada verifica categoría/código y que texto privado no aparece en ErrorLog | APROBADO | `:app:connectedDebugAndroidTest` |
| E11 | Auth/Firestore/red | Categoría segura y mensaje sin detalle privado | Dos pruebas instrumentadas pasaron para errores de Firebase; una cubre red y otra Auth+Firestore | APROBADO | `:app:connectedDebugAndroidTest` |
| B01 | Compilar Debug | APK Debug compilable | Gradle finalizó correctamente | APROBADO | `:app:assembleDebug` |
| B02 | Compilar Release | APK Release compilable | Gradle finalizó correctamente | APROBADO | `:app:assembleRelease` |
| B03 | Compilar código instrumentado | Kotlin de androidTest compilable | Gradle finalizó correctamente | APROBADO | `:app:compileDebugAndroidTestKotlin` |
| B04 | Pruebas instrumentadas | Suite sin fallos | 13 aprobadas, 0 fallidas en dispositivo conectado | APROBADO | `:app:connectedDebugAndroidTest` |
| F01 | Reglas contra Emulator | Aislamiento y validaciones de reglas | 29 aprobadas, 0 fallidas usando host local 127.0.0.1:8080 | APROBADO | Node directo: `--test tests/firestore.rules.test.js` |
| F02 | Flujo completo app → Emulator | Workout, dos series e hidratación; Room PENDING→SYNCED | No se ejecutó: app instalada y abierta, pero presenta pantalla de inicio de sesión sin sesión activa; no se ingresaron credenciales ni se crearon datos | BLOQUEADO | Requiere desbloqueo e interacción manual |

## Conteos

- JVM: 8 aprobadas, 1 fallida.
- Instrumentadas Android: 13 aprobadas, 0 fallidas.
- Reglas Emulator: 29 aprobadas, 0 fallidas.
- Compilaciones requeridas: Debug y Release aprobadas.
- E01–E09: solo E08 y E09 fueron probadas en automatización; E10 y E11 cubren errores sanitizados. Los casos etiquetados NO EJECUTADO no se presentan como éxito.
