# Preparación de beta QA — MyProgres

## Estado actual

La APK de depuración se genera en:

`app/build/outputs/apk/debug/app-debug.apk`

Se instaló en el HONOR MAGIC 8 Lite (ADB: `MTN_NX3`) y se abrió `MainActivity`. Tras declarar `MyProgresApplication` en el manifiesto —necesaria para que Hilt cree las dependencias de la app— el proceso permaneció activo durante la comprobación de arranque.

Las pruebas instrumentadas en el teléfono terminaron con 3 aprobadas, 0 fallidas y 0 omitidas. El reporte está en `app/build/reports/androidTests/connected/debug/index.html`. En local pasaron las 7 pruebas de validación y la prueba unitaria básica. La prueba de Compose con Robolectric sigue fallando al resolver/iniciar `ComponentActivity`; requiere corregir ese harness antes de considerar completa la verificación de UI.

## Funciones presentes que se pueden evaluar en QA

- Inicio y navegación por las pantallas existentes.
- Sesión de entrenamiento local, selección del catálogo inicial de ejercicios, registro individual de peso/repeticiones y finalización.
- Temporizador de descanso de 1, 2 o 3 minutos mientras la pantalla está activa.
- Registro rápido/personalizado de agua y resumen diario orientativo.
- Historial resumido de entrenamientos.
- Diagnóstico local y registro sanitizado de errores.
- Persistencia offline mediante Room.

## Pendiente según el reporte funcional del producto

1. **Cuenta y datos multiusuario:** Firebase Authentication, recuperación/cierre de sesión, aislamiento local por UID, cambio seguro de cuenta y eliminación de cuenta/datos.
2. **Sincronización remota:** Firestore, Security Rules probadas, App Check, cola/outbox, WorkManager, idempotencia, reintentos, resolución de conflictos y estado fiable de sincronización.
3. **Experiencia y alcance:** perfil/preferencias, avatar animal y evolución, widget Android, métricas/gráficos de progreso más completos y gestión/corrección de registros.
4. **Temporizador de descanso:** aviso/notificación al finalizar y comportamiento cuando la app pasa a segundo plano o se cierra.
5. **Errores en producción:** Crashlytics configurado y consentido, control/fallback del registro, verificar que no se envíen datos personales.
6. **Privacidad y publicación:** política de privacidad, retención/exportación/borrado de datos, revisión de permisos y declaraciones de datos, accesibilidad, rendimiento y compatibilidad de dispositivos.
7. **Calidad:** resolver la prueba local de Compose, ampliar pruebas de flujos UI, ciclo de vida del temporizador, Room/migraciones, offline y (cuando exista Firebase) reglas con Emulator Suite.
8. **Artefacto de distribución:** definir firma de release, versionCode/versionName de beta, generar y validar el artefacto release apropiado y preparar instrucciones de instalación/rollback.

## Diferencia entre APK debug y lanzamiento

La APK generada es una compilación `debug`, adecuada para instalación manual y pruebas internas controladas. No es una publicación de producción de Google Play. No se subió ningún artefacto a Play Console. Para una beta en canal de Play hace falta preparar una compilación de release firmada, revisar la configuración de publicación y completar los datos y verificaciones del canal.

## Rúbrica solicitada

No se encontró `Rubrica.jpge` ni variantes habituales de extensión en `C:/Users/Gustavo/Desktop`. Esta lista refleja el reporte de MyProgres y el estado observado del código; no certifica todavía los criterios específicos de esa rúbrica. Incorporar la rúbrica al proyecto o adjuntarla permitirá hacer la matriz criterio por criterio.
