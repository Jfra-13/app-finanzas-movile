# Finanzas Independientes — App Android

Aplicación móvil para que trabajadores independientes registren ingresos y controlen su meta mensual mediante una cuota diaria calculada por el backend.

## Stack

- Kotlin + Android Views (XML), ViewBinding pendiente de adoptar
- MVVM con `ViewModel` + `StateFlow`
- Retrofit 2 + Gson para red
- Backend REST: `api/v1/usuarios`, `api/v1/finanzas`
- Tests unitarios con JUnit (dominio)

## Estructura actual

```
app/src/main/java/com/example/finanzas_independientes_app/
├── LoginActivity.kt                  ← paquete raíz
├── DashboardActivity.kt              ← paquete raíz
├── RegisterActivity.kt               ← carpeta raíz, pero declara package .presentation
├── data/
│   └── remote/
│       ├── FinanzasApi.kt
│       ├── RetrofitClient.kt
│       └── dto/ (LoginDTO, UsuarioRegistroDTO, TransaccionRegistroDTO)
├── domain/
│   └── usecase/CalcularCuotaDiariaUseCase.kt
└── presentation/
    ├── SplashActivity, OnboardingOne/Two, ForgotPassword,
    ├── Verification, NewPassword, SelectBusiness
    └── LoginViewModel, RegistroViewModel, DashboardViewModel
```

## Problemas detectados

1. **Activities dispersas**: `LoginActivity`, `DashboardActivity` y `RegisterActivity` están fuera de `presentation`. `RegisterActivity.kt` además tiene la carpeta y el paquete desincronizados.
2. **Sin capa repository**: los ViewModels llaman a `RetrofitClient.apiService` directamente. Eso acopla la UI a Retrofit y hace imposible testear los ViewModels sin red.
3. **`presentation` plana**: todas las pantallas en una sola carpeta. Con 10+ Activities ya no escala.
4. **Configuración de red hardcodeada**: `BASE_URL` comentada/descomentada a mano en `RetrofitClient`, y `usesCleartextTraffic="true"` global en el manifest.
5. **Dependencias fuera del version catalog**: Retrofit y Lifecycle declaradas con strings en `build.gradle.kts` mientras el resto usa `libs.*`.
6. **Recursos huérfanos y con typos**: `activity_main.xml` no tiene Activity asociada; drawables como `cloud_view_log_sing.xml` y `one_boarding_two.png` tienen nombres erróneos.
7. **Estado de sesión sin centralizar**: el `usuarioId` viaja como parámetro entre pantallas; no hay un lugar único que gestione la sesión.

## Estructura objetivo

Se mantiene la separación por capas (`data` / `domain` / `presentation`) y se subdivide `presentation` por feature:

```
app/src/main/java/com/example/finanzas_independientes_app/
├── core/
│   ├── network/
│   │   └── RetrofitClient.kt          ← BASE_URL desde BuildConfig
│   └── session/
│       └── SessionManager.kt          ← persistencia del usuarioId (DataStore)
├── data/
│   ├── remote/
│   │   ├── api/FinanzasApi.kt
│   │   └── dto/
│   └── repository/
│       ├── AuthRepository.kt          ← login, registro, recuperación
│       └── FinanzasRepository.kt      ← transacciones, cuota diaria
├── domain/
│   ├── model/                         ← modelos de negocio (no DTOs)
│   └── usecase/
│       └── CalcularCuotaDiariaUseCase.kt
└── presentation/
    ├── splash/SplashActivity.kt
    ├── onboarding/
    │   ├── OnboardingOneActivity.kt
    │   └── OnboardingTwoActivity.kt
    ├── auth/
    │   ├── LoginActivity.kt + LoginViewModel.kt
    │   ├── RegisterActivity.kt + RegistroViewModel.kt
    │   ├── ForgotPasswordActivity.kt
    │   ├── VerificationActivity.kt
    │   └── NewPasswordActivity.kt
    ├── business/
    │   └── SelectBusinessActivity.kt
    └── dashboard/
        ├── DashboardActivity.kt
        └── DashboardViewModel.kt
```

Regla general: **la UI habla con ViewModels, los ViewModels con repositories, los repositories con Retrofit**. Nada por encima de `data` conoce a Retrofit.

## Plan de migración (orden sugerido)

| Paso | Cambio | Riesgo |
|------|--------|--------|
| 1 | Mover Activities/ViewModels a sus paquetes por feature y corregir el mismatch de `RegisterActivity` | Bajo — refactor de paquetes con el IDE actualiza manifest e imports |
| 2 | Crear `AuthRepository` y `FinanzasRepository`; los ViewModels reciben el repository (constructor + `ViewModelProvider.Factory`) | Medio |
| 3 | Mover `RetrofitClient` a `core/network` y sacar `BASE_URL` a `buildConfigField` (debug → `10.0.2.2`, release → URL productiva) | Bajo |
| 4 | Crear `SessionManager` para el `usuarioId` | Medio |
| 5 | Pasar Retrofit/Lifecycle al version catalog (`libs.versions.toml`) | Bajo |
| 6 | Limpieza: eliminar `activity_main.xml`, renombrar drawables con typos | Bajo |

Los pasos 1, 3, 5 y 6 son mecánicos y pueden hacerse antes de conectar el backend. Los pasos 2 y 4 conviene hacerlos junto con las próximas conexiones al API para no refactorizar dos veces.

## Convenciones

- Identificadores y nombres de clases en español, consistente con el código existente.
- Layouts: `activity_<pantalla>.xml`, parciales reutilizables `layout_<feature>_<bloque>.xml`.
- DTOs solo en `data/remote/dto`; nunca se exponen a `presentation`.
- Cada use case de dominio con su test unitario (patrón ya iniciado en `CalcularCuotaDiariaUseCaseTest`).
