# Estado — Fase 10 (Release)

Última actualización: 2026-10-20. Punto de retome para la próxima sesión.

## Estado de `main`

`main` en `4d730cb`. Incluye los PR #15, #18, #19 y #20:

- #15 — Transacciones: recarga la lista después de eliminar/actualizar.
- #18 — `safeApiCall` propaga `CancellationException` en lugar de devolver `ApiResult.Error`; `logout()` limpia la sesión en `finally`.
- #19 — Transacciones: el filtro de fechas recarga vía `recargar()` aunque haya una carga en curso.
- #20 — Transacciones: una mutación fallida no libera `isLoading` mientras corre una carga.

45 tests unitarios y `assembleDebug` en verde.

## PRs abiertos

| PR | Rama | Nota |
| --- | --- | --- |
| #5 | `fix/wipe-user-data-on-logout` | Al resolver contra `main`, `endSession()` debe usar `withContext(NonCancellable + Dispatchers.IO)` (ver PR #18). Contiene la sección "Security rules" de `CLAUDE.md`. |
| #6 | `feat/encrypt-room-sqlcipher` | Lo prueba el dueño en dispositivo. No mergear sin su OK. |
| #7 | `feat/certificate-pinning` | Lo prueba el dueño en dispositivo. No mergear sin su OK. Pin vence 2027-10-19. |
| #8 | `docs/api-contract-sync` | Sincroniza el contrato de API con el backend. |
| #9 | `fix/login-bloqueado` | Cataloga `LOGIN_BLOQUEADO` (429). |
| #12 | `fix/monto-no-finito` | Rechaza montos no finitos. |
| #16 | `fix/dashboard-ingreso-hoy` | Evita frases de error en el campo de ingreso de hoy. |
| #17 | `feat/monto-con-coma` | Apilado sobre #12; retarget a `main` cuando #12 se mergee. |
| #10 → #11 → #13 → #14 | `test/viewmodels-fase-9*` | Cadena de tests de ViewModels (Fase 9). #14 no tiene el commit `d50931e` de #13: incluir #13 al verificar el árbol mergeado. |

Pendiente aparte: migración fuera de `androidx.security-crypto` (Keystore + DataStore), se ramifica desde `main` después de mergear #6.

## Plan de Fase 10

Un PR por slice, en orden. Mostrar plan corto y esperar OK antes de implementar cada uno.

### A) `applicationId` final + `app_name` — BLOQUEADO

Falta decidir el `applicationId` final y el `app_name`.

- Actual: `applicationId = "com.example.finanzas_independientes_app"` (`app/build.gradle.kts:16`), `app_name = "Mi App Financiera"` (`res/values/strings.xml:3`).
- Play rechaza `com.example.*` y el id **no se puede cambiar después de la primera subida**. Usar un dominio propio invertido (p. ej. `com.<dominio>.finanzas`).
- `namespace` no se toca (paquetes, `R`, `BuildConfig` y las vistas custom en XML quedan igual).
- Ningún otro archivo referencia el applicationId (sin `${applicationId}`, authorities ni `google-services.json`).
- `ExampleInstrumentedTest.kt:22` compara `packageName` con el id actual: borrarlo (test de plantilla) o actualizar el literal.
- En este mismo slice, corregir el comentario de `TransaccionesViewModel.kt:90`: dice que `safeApiCall` convierte la cancelación en `Error`, lo que dejó de ser cierto desde #18. El `ensureActive()` se mantiene. Ningún otro slice de Fase 10 toca ese archivo.
- Verificación: `assembleDebug`, `testDebugUnitTest`, `compileDebugAndroidTestKotlin`.

### B) Firma de release

- `signingConfig` leído de `keystore.properties` (local) o de variables de entorno (secrets de CI).
- Sin credenciales, `assembleRelease`/`bundleRelease` siguen en verde sin firmar (los PRs no tienen secrets).
- CI: agregar `bundleRelease`; solo en push a `main` y con secrets, subir el AAB firmado como artifact.
- README: cómo generar la upload key con `keytool` y qué 4 secrets cargar.
- La keystore nunca entra al repo (`.gitignore` ya excluye `*.jks`, `*.keystore`, `keystore.properties`).
- `versionCode` manual; sin auto-versionado ni publicación automatizada por ahora.

### C) Ícono adaptativo y splash

- Foreground/background + monochrome (themed icons) e ícono del splash.
- El foreground actual es el placeholder de Android Studio. Los assets los provee el dueño; no inventar marca.
- El splash ya usa `core-splashscreen` (`installSplashScreen()`).

### D) `docs/release/`

En español neutro:

- Política de privacidad (para publicar en la landing).
- Respuestas del formulario Data safety según lo que la app guarda y envía de verdad (Room, EncryptedSecureStorage, caché OkHttp, endpoints).
- Textos de la ficha de Play.

### Cierre

- `docs/release/CHECKLIST.md`: #5/#6/#7 y la migración de security-crypto mergeados, cadena de Fase 9, decisión sobre `FLAG_SECURE`, vencimiento del pinning 2027-10-19, AAB firmado instalado en un dispositivo real, internal testing.
- Marcar Fase 10 en `docs/planeamiento/PLAN.md`.

## Próximo paso

Decidir `applicationId` y `app_name`, luego implementar el slice A.
