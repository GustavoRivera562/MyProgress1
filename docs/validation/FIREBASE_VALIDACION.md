# Validación Firebase — MyProgres

## Configuración inspeccionada

- Proyecto de desarrollo configurado: `myprogress-76f0c`.
- Reglas locales: `firestore.rules`.
- Firestore Emulator: puerto 8080, host utilizado por pruebas locales `127.0.0.1:8080`.
- Debug configura `FirebaseFirestore.useEmulator(host, 8080)` desde el proveedor Hilt antes de entregar la instancia; si falta el host Debug falla de forma explícita.
- Release declara host vacío y la llamada a `useEmulator()` está bajo la condición Debug; no usa la configuración de host local.
- No se cambió Firebase Console, reglas de producción, Gradle ni dependencias en esta validación.

## Reglas

Ejecutado directamente, con `FIRESTORE_EMULATOR_HOST=127.0.0.1:8080`:
`& 'C:\Program Files\nodejs\node.exe' --test tests/firestore.rules.test.js`

Resultado real: 29 tests, 29 pass, 0 fail. Se probaron aislamiento A/B, usuario no autenticado, campos/tipos/rangos, subcolecciones, escritura batch con getAfter(), reintentos y rutas denegadas.

`npm.cmd run test:rules` falló al iniciar el proceso hijo porque `node` no estaba en PATH; no fue un fallo de las reglas. La ejecución directa anterior usó explícitamente el emulador local.

## Sincronización de aplicación

Inspección estática de SyncCoordinator/DAO/FirestoreProgressDataSource:
- Requiere `FirebaseAuth.currentUser.uid`; no recibe UID de UI.
- Evita solapamientos mediante Mutex.
- Consulta solo PENDING del propietario autenticado, excluye ownerUid nulo y no sincroniza error_logs.
- Workout se confirma antes de sus sets; hidratación se sube por separado.
- Cada ACK precede la actualización condicional a SYNCED. Si el contenido local cambia durante el envío, la condición CAS no lo marca.
- Cambio de sesión detectado antes/después de escrituras cancela la operación.
- Las escrituras usan IDs locales estables bajo `users/{uid}`.

En esta continuación se verificó el dispositivo ADB autorizado AN8J6R6426001877, TCP local 127.0.0.1:8080 accesible, instalación de :app:installDebug y apertura de com.example.myprogres. La pantalla actual es el acceso sin sesión activa. No ingresé credenciales ni creé registros, así que no hay evidencia de documentos nuevos ni de cambios PENDING→SYNCED. La prueba queda pendiente de que el usuario inicie sesión manualmente en el teléfono.

## Producción

No se ejecutó `firebase deploy`, no se tocaron reglas de producción y no se realizaron escrituras en Firestore de producción. Las pruebas de reglas se dirigieron únicamente a localhost y proyecto de prueba.
