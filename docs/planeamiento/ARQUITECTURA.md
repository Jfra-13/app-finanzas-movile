# Arquitectura técnica — Finanzas Independientes (App Android)

Especificación técnica del **estado objetivo** del cliente móvil Android. Describe cómo debe
quedar la app: capas, contratos internos, patrones de red y sesión, offline-first, inyección de
dependencias, testing y convenciones. Es el documento de referencia técnica; el roadmap para
llegar acá vive en [`PLAN.md`](PLAN.md) y el contrato con el backend en
[`docs/api/API-CONTRACT.md`](../api/API-CONTRACT.md).

> Ante diferencias de contrato, manda **Swagger** (`http://localhost:9090/swagger-ui.html`).

---

## 1. Stack y toolchain

| Área | Elección | Nota |
|---|---|---|
| Lenguaje | Kotlin | — |
| UI | **Android View System** (XML + ViewBinding) | NO Compose. Activities, sin Fragments ni Navigation Component. |
| Arquitectura | Clean Architecture por capas + MVVM | UI → ViewModel → Repository → Retrofit/Room |
| Async | Coroutines + `StateFlow` | ViewModels exponen estado, no callbacks |
| Red | Retrofit 2 + OkHttp + Gson | Envelope propio sobre Retrofit |
| Persistencia local | Room | Caché offline-first |
| Sesión segura | EncryptedSharedPreferences (Keystore) | Tokens nunca en texto plano |
| DI | **Hilt** | Sin ServiceLocator ni factories manuales |
| Gráficos | **Vico** (`views`, Cartesian: línea/columna) | Sin pie chart; ver §7 |
| Tests | JUnit (dominio, ViewModels) | Cada use case con su test |

Toolchain fijado en `gradle/libs.versions.toml`: AGP `9.0.1`, Kotlin/KSP `2.2.10`, Hilt `2.59.2`,
Room `2.7.2`, Retrofit `2.9.0`, OkHttp `4.12.0`. Requiere **JDK 17**. `minSdk 24`, `targetSdk 36`,
`compileSdk 36`.

---

## 2. Regla de dependencias

```
presentation  →  domain  →  data
   (UI)          (models,     (remote + local
                  use cases,    + repository impls)
                  interfaces)
```

- **La UI habla con ViewModels; los ViewModels con repositories; los repositories con Retrofit/Room.**
- Nada por encima de `data` conoce Retrofit, DTOs ni excepciones de red.
- El dominio no depende de Android ni de librerías de infraestructura; define **interfaces** que
  `data` implementa (inversión de dependencias).

---

## 3. Estructura de paquetes

```
app/src/main/java/com/example/finanzas_independientes_app/
├── core/
│   ├── network/     ← Retrofit, envelope ApiResponse, ApiResult, AppError,
│   │                  ApiCode, safeApiCall, AuthInterceptor, TokenAuthenticator, NetworkModule
│   └── session/     ← SessionManager, SecureStorage/EncryptedSecureStorage
├── data/
│   ├── remote/      ← FinanzasApi + dto/ (DTOs)
│   ├── local/       ← Room: AppDatabase, dao/, entity/, Converters
│   ├── mapper/      ← DTO ↔ modelo de dominio
│   └── repository/  ← AuthRepositoryImpl, FinanzasRepositoryImpl
├── domain/
│   ├── model/       ← modelos de negocio (Usuario, Transaccion, Meta, Categoria, …)
│   ├── repository/  ← interfaces (AuthRepository, FinanzasRepository)
│   └── usecase/     ← casos de uso (cada uno con test)
├── di/              ← módulos Hilt (AppModule, DatabaseModule, RepositoryModule, NetworkModule)
└── presentation/    ← una carpeta por feature:
                       splash, onboarding, auth, business, dashboard,
                       transacciones, categorias, analytics, calendar, profile, common
```

Convención de features: cada carpeta contiene su `Activity`, su `ViewModel` y, si aplica, su
`Adapter`. `common/` aloja lo compartido entre features (p. ej. bottom navigation, estados
universales).

---

## 4. Patrón de resultado de red (crítico)

Toda llamada al API atraviesa un envelope uniforme y devuelve un tipo de **dominio**, nunca
`Response<>` de Retrofit ni excepciones crudas.

- **`ApiResponse<T>`** modela el envelope del backend: `timestamp`, `status`, `code`, `message`,
  `data`, `path`, `details`.
- **`ApiCode`** es el catálogo (enum/sealed) de códigos de negocio (`LOGIN_SUCCESS`,
  `VALIDATION_ERROR`, `UNAUTHORIZED`, `EMAIL_DUPLICADO`, `OTP_INVALIDO`, …).
- **`safeApiCall` / `safeUnitCall`** envuelven la llamada y producen un sealed
  **`ApiResult<T>`** = `Success(data, code)` | `Error(AppError)`.
- La UI ramifica con `onSuccess {}` / `onError {}` **por el campo `code`**, jamás por `message`
  (el texto es para humanos y puede cambiar).
- Errores de validación (`details[]`) se mapean a errores **por campo** para los formularios.

`BASE_URL` se inyecta por `buildConfigField` según build type (debug → `http://10.0.2.2:9090/`,
release → Azure). El prefijo `/api/v1` lo agrega la capa de red, **no** la base URL.

---

## 5. Autenticación y sesión

- **JWT Bearer** + refresh rotativo (access 15 min, refresh 30 días). La identidad del usuario
  se deriva del token en cada request protegido; ningún endpoint recibe `usuarioId`.
- **`SessionManager`** persiste identidad (`usuarioId`, `nombre`, `email`, `tipoNegocio`) y
  **tokens en almacenamiento cifrado** (`EncryptedSecureStorage` sobre Keystore). Nunca tokens en
  `SharedPreferences` plano.
- **`AuthInterceptor`** inyecta `Authorization: Bearer <token>` en rutas protegidas.
- **`TokenAuthenticator`** (OkHttp `Authenticator`): ante **401**, llama a `POST /refresh`, guarda
  el **par nuevo** (rotación) y reintenta. Ante `REFRESH_TOKEN_INVALIDO` → limpia sesión y redirige
  a login. El refresh debe **sincronizarse** para no dispararse N veces si varias requests fallan
  401 en simultáneo.
- **Arranque:** Splash decide login vs. dashboard según sesión válida.

---

## 6. Datos y offline-first

- **Modelos de dominio** en `domain/model` — nunca DTOs cruzan a `presentation`. Los mappers
  (`data/mapper`) convierten DTO ↔ dominio; los DTOs no salen de `data`.
- **Room** cachea historial, metas y categorías (`entity/`, `dao/`, `AppDatabase`, `Converters`).
  Estrategia offline-first: lecturas instantáneas desde caché, sincronización con el backend, y
  escritura optimista donde tenga sentido.
- **Paginación** del historial vía infinite scroll sobre el endpoint paginado.

---

## 7. UI y gráficos

- **View System puro**: Activities + XML en `res/layout/`, `viewBinding = true`. Listas con
  **RecyclerView + Adapter** y `DiffUtil`. Material Components + ConstraintLayout.
- **Design system**: tokens de color/tipografía/espaciado sobre grilla de 8pt (`dp`), split de
  color 60/30/10. Dark mode con `res/values/` + `res/values-night/`.
- **Estados universales** consistentes: loading (shimmer/skeleton), empty, error, success.
- **Gráficos con Vico** (`com.patrykandpatrick.vico:views`, solo Cartesian: línea/columna). En v3
  el AAR `views` aplana todo bajo `com.patrykandpatrick.vico.views.*` (no hay `.core`); los rellenos
  sólidos usan el constructor `Fill(colorInt)`. El teléfono solo pinta datos ya calculados: el
  procesamiento pesado vive en el servidor.

---

## 8. Inyección de dependencias

Hilt en toda la app. Módulos en `di/`:

- **`NetworkModule`** (en `core/network`) — Retrofit, OkHttp, interceptores, `FinanzasApi`.
- **`DatabaseModule`** — `AppDatabase` y DAOs.
- **`RepositoryModule`** — bindea interfaces de `domain/repository` a sus `*Impl`.
- **`AppModule`** — resto de singletons (SessionManager, storage).

ViewModels reciben sus dependencias por constructor (`@HiltViewModel`). Sin factories manuales.

---

## 9. Testing

- **Unit:** cada use case de dominio con su test (patrón ya establecido:
  `CalcularCuotaDiariaUseCaseTest`, `ProyectarTendenciaUseCaseTest`). Mappers y lógica de
  ViewModels con repos fake.
- **Repository tests:** con `MockWebServer`, validando el parseo del envelope y el manejo por `code`.
- **Auth tests:** flujo de refresh (401 → refresh → retry) y refresh inválido → logout.
- **UI tests:** Espresso en flujos críticos (login, registrar transacción, fijar meta).
- **Gate de PR:** `gradlew.bat :app:assembleDebug` debe quedar en verde en cada PR.

---

## 10. Convenciones

- Identificadores y nombres de clase de dominio en **español** (`Transaccion`, `Meta`, `Categoria`,
  `CalcularCuotaDiariaUseCase`), consistente con el código existente. Comentarios/documentación en
  inglés por defecto, según el archivo.
- Layouts: `activity_<pantalla>.xml`; parciales reutilizables `layout_<feature>_<bloque>.xml`;
  ítems de lista `item_<cosa>.xml`.
- DTOs solo en `data/remote/dto`; nunca expuestos a `presentation`.
- Dependencias vía version catalog (`libs.*`), no coordenadas hardcodeadas.
- El cliente decide siempre por `code`, nunca por `message`.
