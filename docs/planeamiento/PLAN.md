# Plan de Desarrollo — Finanzas Independientes (App Android)

Plan por fases desde el estado actual hasta el producto terminado: funcionalidad completa, UI/UX, rendimiento, seguridad y release.

> **Fuente de verdad del contrato:** [`docs/api/API-CONTRACT.md`](../api/API-CONTRACT.md) y Swagger (`http://localhost:9090/swagger-ui.html`). Ante cualquier diferencia, manda Swagger.

---

## Hallazgo crítico — punto de partida (histórico)

> Esta sección y la de "inventario" describen el **estado inicial** (pre-refactor) que motivó las Fases 0–3. Ya están resueltas; se conservan para explicar el porqué del plan, no el estado actual.

El cliente original fue construido contra un contrato anterior del backend. Hoy **no coincide** con la API vigente. No es un problema de "faltan pantallas": la capa de datos está desalineada en lo fundamental.

| Aspecto | Estado en el código actual | Contrato real del backend |
|---|---|---|
| Login | `iniciarSesion(...): Response<Long>` | Envelope con `token`, `refreshToken`, `usuarioId`, `nombre`, `email`, `tipoNegocio` |
| Identidad del usuario | Se pasa `usuarioId` como parámetro/body | Se deriva del **JWT** en cada request protegido |
| `cuota-diaria` | `GET .../cuota-diaria/{usuarioId}` | `GET .../cuota-diaria` con `meta`/`dias` opcionales (sin path param) |
| Transacción | DTO incluye `usuarioId` | El servidor lo toma del token; el body no lo lleva |
| Respuestas | Se leen como tipos crudos (`Long`, `Double`) | **Envelope** uniforme; el cliente decide por `code` |
| Autenticación | No existe | JWT Bearer + refresh rotativo (access 15 min, refresh 30 días) |
| Almacenamiento de sesión | No existe | Tokens en almacenamiento seguro (Keystore / EncryptedSharedPreferences) |
| Metas | `meta=3000`, `dias=10` hardcoded en `DashboardViewModel` | Metas **persistidas** por usuario (`POST /metas`) |
| Cobertura de endpoints | 4 cableados | ~25 en el contrato (auth, transacciones CRUD, metas, categorías, analíticas) |

**Conclusión:** las Fases 0–2 son de reconstrucción de cimientos, no opcionales. Sin ellas, ninguna pantalla nueva funciona contra el backend real.

---

## Inventario inicial (histórico)

**Existía al arrancar:**
- 11 Activities (Splash, 2 Onboarding, Login, Register, ForgotPassword, Verification, NewPassword, SelectBusiness, Dashboard).
- `DashboardActivity` modularizada en parciales (`layout_dashboard_*`, `layout_bottom_navigation`).
- 3 ViewModels (`Login`, `Registro`, `Dashboard`) que llaman a `RetrofitClient` directo.
- 1 use case con test (`CalcularCuotaDiariaUseCase`).
- DTOs sueltos en `data/remote/dto`.

**Problemas estructurales (del README principal):**
- Activities en el paquete raíz; `RegisterActivity` con carpeta/paquete desincronizados.
- Sin capa repository. Sin `core/network` ni `core/session`.
- `presentation` plana (no por feature).
- `BASE_URL` hardcodeada y `usesCleartextTraffic="true"` global.
- Retrofit/Lifecycle fuera del version catalog (de hecho **no están** en `libs.versions.toml`).
- Recursos huérfanos (`activity_main.xml`) y drawables con typos.

---

## Mapa de fases

**Leyenda de estado:** `✅` terminada · `–` a pulir (funcional, con pendientes) · *(vacío)* sin empezar.
Estado reflejado según el código al **2026-07-28**.

| Fase | Estado | Nombre | Foco | Bloquea a |
|---|---|---|---|---|
| 0 | ✅ | Cimientos técnicos | Refactor de estructura, version catalog, ViewBinding, limpieza | Todas |
| 1 | ✅ | Núcleo de red | Envelope, modelo de errores por `code`, interceptores | 2–5 |
| 2 | ✅ | Autenticación y sesión | JWT, refresh rotativo, almacenamiento seguro, `SessionManager` | 4–5 |
| 3 | ✅ | Dominio y repositories | Modelos de negocio, `AuthRepository`, `FinanzasRepository`, DI Hilt | 4–5 |
| 4 | – | Features funcionales | Auth completo, dashboard real, transacciones, metas, categorías | 5–6 |
| 5 | ✅ | Analíticas y visualización | Gráficos (semanal, tendencia, categorías), salud financiera | 6 |
| 6 | – | UI/UX y design system | Sistema visual, estados, accesibilidad, dark mode; **falta responsive** | 7 |
| 7 | – | Rendimiento | Paginación e infinite scroll, caché offline, Splash API; **falta sync completa** | 9 |
| 8 | – | Seguridad endurecida | Cleartext off, network security config, R8, backups; **falta pinning y FLAG_SECURE** | 9 |
| 9 | – | Calidad y testing | Unit del core de red, mappers, ViewModel, CI; **falta Espresso y cobertura de ViewModels** | 10 |
| 10 | | Release | Signing, variantes, Play Store | — |

---

## Fase 0 ✅ — Cimientos técnicos

**Objetivo:** dejar el proyecto en la estructura objetivo del README principal antes de tocar lógica de negocio. Cambios mecánicos, bajo riesgo, alto retorno.

**Tareas:**
1. Mover Activities/ViewModels a paquetes por feature (`presentation/{splash,onboarding,auth,business,dashboard}`). Corregir el mismatch de `RegisterActivity`.
2. Crear paquetes `core/network` y `core/session` (vacíos por ahora).
3. Adoptar **ViewBinding** en todas las Activities (eliminar `findViewById`).
4. Migrar dependencias al version catalog: Retrofit, Gson, OkHttp (logging + interceptors), Lifecycle (`viewModel-ktx`, `runtime-ktx`), Coroutines, DataStore, EncryptedSharedPreferences, Coroutine test.
5. Limpieza: eliminar `activity_main.xml`, renombrar drawables con typos (`cloud_view_log_sing.xml`, `one_boarding_two.png`).

**Definition of Done:**
- El proyecto compila con la estructura por feature.
- `libs.versions.toml` es la única fuente de versiones; no hay strings de versión en `build.gradle.kts`.
- Ninguna Activity usa `findViewById`.

**Riesgo:** Bajo. El refactor de paquetes del IDE actualiza manifest e imports.

---

## Fase 1 ✅ — Núcleo de red

**Objetivo:** que el cliente hable el idioma real del backend: envelope uniforme y decisión por `code`.

**Tareas:**
1. `ApiResponse<T>` genérico que modele el envelope (`timestamp`, `status`, `code`, `message`, `data`, `path`, `details`).
2. Catálogo de `code` como sealed/enum alineado con el README (`LOGIN_SUCCESS`, `VALIDATION_ERROR`, `UNAUTHORIZED`, `EMAIL_DUPLICADO`, `OTP_*`, etc.).
3. Wrapper de resultado de dominio (`Result<Success, AppError>`) para que la UI nunca vea `Response<>` de Retrofit ni excepciones crudas.
4. `OkHttpClient` con `HttpLoggingInterceptor` (solo en debug) y timeouts.
5. Mover `RetrofitClient` a `core/network`. Sacar `BASE_URL` a `buildConfigField` (debug → `10.0.2.2:9090`, release → URL productiva). Quitar la URL hardcodeada.
6. Mapear errores de validación (`details[]`) a errores por campo para los formularios.

**Definition of Done:**
- Toda llamada devuelve un tipo de dominio, nunca `Response<>` ni tipos crudos.
- La UI ramifica por `code`, jamás por `message`.
- `BASE_URL` sale de `BuildConfig`.

**Riesgo:** Medio. Es el corazón del cliente.

---

## Fase 2 ✅ — Autenticación y sesión

**Objetivo:** sesión JWT real, segura y con refresh automático.

**Tareas:**
1. `SessionManager`: persistir `usuarioId`, `nombre`, `email`, `tipoNegocio` (DataStore) y los **tokens en almacenamiento seguro** (EncryptedSharedPreferences / Keystore). Nunca tokens en `SharedPreferences` plano.
2. `AuthInterceptor`: inyecta `Authorization: Bearer <token>` en rutas protegidas.
3. `TokenAuthenticator` (OkHttp `Authenticator`): ante **401**, llama a `POST /refresh` con el refresh token, guarda el **par nuevo** (rotación) y reintenta. Ante `401 REFRESH_TOKEN_INVALIDO` → limpia sesión y redirige a login.
4. Flujo de arranque: Splash decide login vs. dashboard según sesión válida.
5. Recuperación de contraseña: `forgot-password` → `verify-otp` (opcional) → `reset-password`, ramificando por `OTP_INVALIDO`/`OTP_EXPIRADO`/`OTP_BLOQUEADO`.

**Definition of Done:**
- Un 401 en ruta protegida dispara refresh transparente y reintento.
- Refresh inválido cierra sesión limpiamente.
- Los tokens no son legibles en almacenamiento plano.

**Riesgo:** Medio-alto. Concurrencia del refresh (varias requests fallando 401 a la vez) debe sincronizarse para no refrescar N veces.

---

## Fase 3 ✅ — Dominio y repositories

**Objetivo:** desacoplar la UI de Retrofit. La UI habla con ViewModels; los ViewModels con repositories; los repositories con la API.

**Tareas:**
1. Modelos de dominio en `domain/model` (no DTOs): `Usuario`, `Transaccion`, `Meta`, `Categoria`, `ProgresoMetas`, `ResumenSemanal`, `SaludFinanciera`, etc.
2. Mappers DTO ↔ dominio. Los DTOs no salen de `data`.
3. `AuthRepository` (login, registro, refresh, recuperación, actualizar negocio).
4. `FinanzasRepository` (transacciones CRUD, hoy, cuota-diaria, resumen-semanal, progreso-metas, metas, categorías, analíticas).
5. ViewModels reciben el repository por constructor (`ViewModelProvider.Factory` o Hilt si se decide DI formal).
6. Eliminar el hardcode de meta/días de `DashboardViewModel`: usar la meta persistida.

**Definition of Done:**
- Ningún ViewModel importa `RetrofitClient` ni Retrofit.
- Los repositories son testeables sin red (interfaz + fake).

**Riesgo:** Medio.

**Decisión a tomar:** DI manual (`Factory`) vs. Hilt. Recomendación: Hilt si el equipo crece o el grafo de dependencias se complica; manual si se quiere mínimo overhead. Se decide al inicio de esta fase.

---

## Fase 4 – — Features funcionales

**Objetivo:** todas las pantallas operando contra el backend real.

**Tareas:**
1. **Auth UI completo:** registro (con `tipoNegocio`), login, recuperación de contraseña, selección de negocio (`PUT /me/negocio`).
2. **Dashboard real:** termómetro diario (`/hoy`), cuota diaria (`/cuota-diaria` con meta persistida), progreso (`/progreso-metas`).
3. **Transacciones:** alta (ingreso/egreso, descripción, fecha, categoría), historial paginado (`GET /transacciones`), edición (`PUT`), borrado (`DELETE`). Manejo de `403 ACCESO_DENEGADO` y `404`.
4. **Metas:** fijar/editar meta del mes (`POST /metas`) y leer la activa (`GET /metas/actual`, con `404 META_NO_ENCONTRADA` → invitar a crear meta).
5. **Categorías:** listar base + propias (`GET /categorias`), crear propia (`POST /categorias`).

> **Heredado de Fase 2 (infra lista, falta cablear presentación):** las Fases 1–2 dejaron la API, los DTOs y los interceptores listos, pero estas piezas de UI quedaron pendientes y se cierran acá:
> - Cablear las pantallas `forgot-password` → `verify-otp` → `reset-password` a los endpoints nuevos (`forgotPassword`/`verifyOtp`/`resetPassword` ya existen en `FinanzasApi`), ramificando por `OTP_INVALIDO`/`OTP_EXPIRADO`/`OTP_BLOQUEADO`.
> - Migrar `SelectBusinessActivity` a `PUT /me/negocio` (`actualizarNegocio`) y actualizar `tipoNegocio` en `SessionManager`; reemplazar el flag plano `ELIGIO_NEGOCIO` que aún lee el Splash.
> - Quitar el hardcode `meta=3000`/`dias=10` de `DashboardViewModel` y usar la meta persistida.
> - Quitar el parámetro transitorio `usuarioId` de `DashboardViewModel.cargarCuotaDiaria`/`registrarIngreso` (la identidad ya viaja en el JWT).

**Definition of Done:**
- Cada pantalla maneja éxito, vacío, carga y error (por `code`).
- Sin valores hardcoded de negocio.

**Riesgo:** Medio.

---

## Fase 5 ✅ — Analíticas y visualización

**Objetivo:** gráficos alimentados por el JSON listo del backend.

**Tareas:**
1. Gráfico de barras semanal (`/resumen-semanal`, siempre 7 días).
2. Gráfico de líneas de tendencia mensual (`/tendencia-mensual`, arrays paralelos, más antiguo primero).
3. Gráfico de torta de egresos por categoría (`/resumen-categorias`, incluye "Sin categoría").
4. Panel de salud financiera (`/salud-financiera`): tarjetas de alerta/felicitación ramificando por `code` (`GASTO_DIARIO_ALTO`, `META_CERCA`, `META_EN_RIESGO`).

**Definition of Done:**
- Gráficos resilientes a datos en cero/vacíos.
- Cero lógica de cálculo en el cliente (el backend ya entrega listo para graficar).

**Riesgo:** Bajo-medio. Elegir librería de charts (MPAndroidChart o equivalente) al inicio.

---

## Fase 6 – — UI/UX y design system

**Objetivo:** producto pulido y consistente, no pantallas sueltas.

**Tareas:**
1. **Design system:** tokens de color, tipografía, espaciado, componentes reutilizables (botones, inputs, tarjetas). Theming en Material 3.
2. **Estados universales:** loading (skeletons/shimmer), empty, error, success — consistentes en toda la app.
3. **Responsive:** soporte de tamaños de pantalla y orientación (el commit `DashboardActivity completo - falta responsive` ya marca esto pendiente).
4. **Dark mode.**
5. **Accesibilidad:** `contentDescription`, tamaños táctiles mínimos, contraste, escalado de fuente.
6. **Microinteracciones:** feedback de carga, transiciones, validación inline en formularios.

**Definition of Done:**
- Auditoría de accesibilidad básica pasada.
- Mismo lenguaje visual en todas las pantallas.
- Sin layouts rotos en pantallas chicas o landscape.

**Riesgo:** Medio (alcance amplio; priorizar pantallas de mayor uso).

---

## Fase 7 – — Rendimiento

**Objetivo:** fluidez y consumo responsable de red/batería.

**Tareas:**
1. **Paginación** real del historial (`Paging 3` o paginación manual sobre el endpoint).
2. **Caché / offline-first:** Room para historial, metas y categorías; estrategia de sincronización. Lecturas instantáneas, escritura optimista donde tenga sentido.
3. **Arranque:** optimizar Splash (Splash Screen API), evitar trabajo en el hilo principal.
4. **Red:** caché HTTP donde aplique, evitar requests redundantes (p. ej. recalcular cuota solo cuando cambian datos).
5. **Render:** layouts planos, `RecyclerView` con `DiffUtil`, evitar overdraw.

**Definition of Done:**
- Historial fluido con cientos de movimientos.
- App utilizable sin red para datos ya cargados.
- Sin jank perceptible en listas (medido con Profiler / Macrobenchmark).

**Riesgo:** Medio. Offline-first agrega complejidad de sincronización.

---

## Fase 8 – — Seguridad endurecida

**Objetivo:** cerrar la superficie de ataque antes de release.

**Tareas:**
1. ✅ **Desactivar `usesCleartextTraffic`** global. Network Security Config: cleartext solo para `10.0.2.2`/`localhost`/`127.0.0.1` en debug (overlay en `src/debug/res/xml/`); HTTPS forzado en release y en el resto de los hosts de debug.
2. **Certificate pinning** contra el dominio productivo. **Pendiente:** requiere el certificado real de `businesscontrol.azurewebsites.net` y un plan de rotación antes de tocar código; un pin vencido deja la app sin red.
3. ✅ **R8/ProGuard** con ofuscación, `minify` y `shrinkResources` en release. APK 17,2 MB → 6,6 MB. Reglas propias solo para Gson (los DTOs y el envelope fijan sus fields); Retrofit/OkHttp/Room/Hilt/Vico traen sus consumer rules.
4. ✅ Tokens solo en `EncryptedSecureStorage` (EncryptedSharedPreferences + master key en Keystore); ningún `SharedPreferences` plano guarda credenciales.
5. `FLAG_SECURE` en pantallas sensibles: **pendiente**, es decisión de producto (bloquea capturas de pantalla legítimas del usuario). ✅ Backups: `allowBackup="false"` y exclusiones reales de `sharedpref`/`database` en ambos archivos de reglas, incluida la transferencia device-to-device de API 31+.
6. ✅ Logs: `HttpLoggingInterceptor` ya está en `Level.NONE` fuera de debug (`NetworkModule.kt`).

**Definition of Done:**
- ✅ Release sin cleartext.
- ✅ APK ofuscado.
- Security review (skill `/security-review`) sin hallazgos críticos — pendiente de correr.

**Riesgo:** Medio. El pinning mal hecho rompe conectividad — plan de rotación de certificados.

---

## Fase 9 – — Calidad y testing

**Objetivo:** red de seguridad para iterar sin miedo.

Punto de partida: 12 tests, todos de use cases. Hoy: **34**.

**Tareas:**
1. Unit: ✅ use cases (ya venían), ✅ mappers estructurales, **parcial** ViewModels — solo `AccountViewModel` (con repo fake). Quedan 14 ViewModels sin cubrir.
2. ✅ **`MockWebServer`** validando el parseo del envelope y el manejo de `code`, en `SafeApiCallTest`. Se testea el embudo (`safeApiCall`/`safeUnitCall`) y no cada repository: ahí es donde vive la lógica, un test por repo sería re-testear Retrofit.
3. ✅ **Auth tests:** `TokenAuthenticatorTest` cubre 401 → refresh → retry, refresh inválido → logout, reuso del token si otro hilo ya refrescó, y corte de reintentos.
4. **UI tests (Espresso): pendiente.** Necesita emulador en CI, es lento y frágil; se pospone hasta que los flujos de UI dejen de moverse.
5. ✅ **CI:** `.github/workflows/ci.yml` corre tests + `assembleDebug` + `assembleRelease` en cada push a `main` y en cada PR. El release entra al gate porque es el único paso que ejecuta R8.

**Definition of Done:**
- Flujos críticos cubiertos por tests — ✅ los de red y sesión; falta la capa de UI.
- ✅ CI verde como gate de merge.

**Nota de calidad:** los tests de `TokenAuthenticator` se validaron por mutación
(persistir el refresh token viejo en vez del rotado) para confirmar que fallan
cuando la lógica se rompe, no solo que pasan.

**Riesgo:** Bajo-medio.

---

## Fase 10 — Release _(pendiente)_

**Objetivo:** publicar.

**Tareas:**
1. **Signing config** (keystore productiva fuera del repo, en secrets/CI).
2. **Build variants** (debug/release) con `BASE_URL` y flags correctos.
3. Íconos adaptativos, splash final, versionado (`versionCode`/`versionName`).
4. Ficha Play Store: capturas, descripción, política de privacidad.
5. Pre-lanzamiento: test en dispositivos reales, internal testing track.

**Definition of Done:**
- Build de release firmado e instalable.
- Listado en Play Console listo para revisión.

**Riesgo:** Bajo (administrativo), salvo políticas de Play.

---

## Decisiones abiertas (resolver al avanzar)

| Decisión | Cuándo | Opciones |
|---|---|---|
| DI manual vs. Hilt | Fase 3 | Manual (mínimo overhead) / Hilt (escala) |
| Librería de charts | Fase 5 | MPAndroidChart / Vico / custom |
| Offline-first con Room | Fase 7 | Caché total / solo lecturas frecuentes |
| Migración futura a Compose | Post-MVP | Mantener Views / migrar incremental |

---

## Convenciones (del README principal)

- Identificadores y clases en español, consistente con el código existente.
- Layouts: `activity_<pantalla>.xml`; parciales `layout_<feature>_<bloque>.xml`.
- DTOs solo en `data/remote/dto`; nunca expuestos a `presentation`.
- Cada use case de dominio con su test unitario.
- El cliente decide siempre por `code`, nunca por `message`.
