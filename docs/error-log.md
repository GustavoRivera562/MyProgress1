# Registro de errores de MyProgres

Documento de seguimiento para registrar problemas detectados y documentarlos antes de corregirlos. Este archivo no implica cambios en código ni configuración.

## E-001 — Incompatibilidad de metadatos Kotlin en Firebase Auth

- **Estado:** Abierto
- **Área:** Compilación / Firebase
- **Detección:** Después de integrar Firebase Gradle y ejecutar `:app:assembleDebug`.
- **Síntoma:** La tarea `:app:compileDebugKotlin` falla porque Firebase Auth 25.0.0 contiene metadatos Kotlin 2.4.0, mientras que el compilador del proyecto espera metadatos 2.2.0.
- **Impacto:** No se puede compilar la variante Debug con la combinación actual.
- **Siguiente paso:** Revisar la compatibilidad de la versión de Firebase Auth y la configuración Kotlin antes de proponer una modificación.
- **Nota:** No se cambiaron versiones ni código como respuesta a este error.

## E-002 — Prueba unitaria Robolectric no inicia ComponentActivity

- **Estado:** Abierto
- **Área:** Pruebas unitarias / Robolectric
- **Síntoma:** `HomeScreenTest.homeScreen_showsTrainingAndWaterSummary` falla antes de llegar a las aserciones de UI; Robolectric no puede resolver o iniciar `androidx.activity.ComponentActivity`.
- **Impacto:** La suite completa `testDebugUnitTest` no queda verde.
- **Siguiente paso:** Revisar el reporte HTML de Gradle en `app/build/reports/tests/testDebugUnitTest/` y las dependencias/configuración de Robolectric.
- **Nota:** Las pruebas instrumentadas reportadas previamente pasaron en el dispositivo HONOR Magic8 Lite; las pruebas filtradas de validación y la prueba unitaria básica también pasaron.

## E-003 — Permisos insuficientes en Firebase CLI

- **Estado:** Abierto
- **Área:** Firebase / permisos del proyecto
- **Síntoma:** Firebase CLI pudo listar proyectos, pero las operaciones `firestore:databases:list` y `init auth` contra el proyecto `myprogress-76f0` devolvieron HTTP 403 o indicaron que la cuenta no tenía permisos para obtener el proyecto.
- **Impacto:** No se pudo verificar/crear la configuración remota de Firestore/Auth desde CLI.
- **Siguiente paso:** Confirmar que la cuenta usada por Firebase CLI tenga el rol requerido en el proyecto y repetir la operación cuando se autorice.
- **Nota de seguridad:** No incluir aquí tokens, códigos de autenticación ni datos personales.

## Verificaciones registradas

- Gradle Sync: completado correctamente después de los cambios de integración Firebase.
- `:app:assembleDebug`: falló en `:app:compileDebugKotlin` por E-001.
- No se han aplicado correcciones para los errores de este registro.
