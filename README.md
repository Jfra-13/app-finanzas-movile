# Finanzas Independientes — App Android

Aplicación móvil nativa (Kotlin) para que trabajadores independientes registren sus ingresos y
egresos y controlen su **meta mensual** mediante una **cuota diaria** calculada por el backend.
Es el cliente móvil de la plataforma Finanzas Independientes; consume una API REST cuyo contrato
vive en [`docs/API-CONTRACT.md`](docs/API-CONTRACT.md).

## Funcionalidades

- **Autenticación segura** — registro, login y recuperación de contraseña por OTP, con sesión JWT
  y refresh rotativo. Tokens guardados en almacenamiento cifrado.
- **Dashboard diario** — termómetro del día, cuota diaria y progreso hacia la meta del mes.
- **Transacciones** — alta, edición, borrado e historial paginado de ingresos y egresos por categoría.
- **Metas y categorías** — fijar la meta mensual y gestionar categorías propias además de las base.
- **Analíticas** — resumen semanal, tendencia mensual, egresos por categoría y panel de salud
  financiera con señales accionables.
- **Experiencia** — design system consistente, dark mode, accesibilidad y caché offline-first.

## Stack

Kotlin · Android View System (XML + ViewBinding, **no Compose**) · MVVM con Coroutines + `StateFlow` ·
Retrofit 2 + OkHttp · Room (offline-first) · Hilt (DI) · Vico (gráficos) · EncryptedSharedPreferences.

Arquitectura Clean por capas: **UI → ViewModel → Repository → Retrofit/Room**. El detalle técnico
completo está en [`planeamiento/ARQUITECTURA.md`](docs/planeamiento/ARQUITECTURA.md).

```
core/network   ← Retrofit, envelope ApiResponse, ApiResult, AppError, interceptores, safeApiCall
core/session   ← SessionManager, tokens en EncryptedSharedPreferences
data/remote    ← FinanzasApi + DTOs + mappers
data/local     ← Room (caché offline-first)
data/repository← *RepositoryImpl (implementan interfaces de dominio)
domain         ← modelos de negocio, interfaces de repositorio, use cases
di             ← módulos Hilt
presentation/<feature> ← Activity + ViewModel (+ Adapter) por feature
```

## Requisitos

| Herramienta | Versión | Nota |
|---|---|---|
| JDK | **17** | Exigido por AGP 9. El JDK embebido de Android Studio sirve. |
| Android Studio | Ladybug o superior | Recomendado; también funciona solo con CLI + SDK. |
| Android SDK | API 36 (`compileSdk 36`) | Instalar vía SDK Manager. `minSdk 24`. |
| Gradle | Wrapper | No instalar a mano: usar `./gradlew` (`gradlew.bat` en Windows). |

## Quickstart

1. Cloná el repo y abrí la **carpeta raíz** en Android Studio (importa el proyecto Gradle).
2. Verificá que `local.properties` apunte a tu SDK (Android Studio lo genera; no se commitea):
   ```properties
   sdk.dir=C:\\Users\\<tu-usuario>\\AppData\\Local\\Android\\Sdk
   ```
3. Esperá el primer **Gradle Sync**.
4. Elegí un emulador o dispositivo físico y corré ▶ (`Shift+F10`).

Desde la CLI:

```bash
gradlew.bat :app:assembleDebug        # compilar APK debug (debe quedar en verde en cada PR)
gradlew.bat :app:installDebug         # instalar en dispositivo/emulador
gradlew.bat :app:testDebugUnitTest    # tests unitarios
gradlew.bat clean                     # limpiar build
```

> En Unix, reemplazá `gradlew.bat` por `./gradlew`.

## Conexión con el backend

`BASE_URL` se inyecta por `buildConfigField` según el build type — **nunca hardcodeada**:

| Build type | `BASE_URL` | Cuándo |
|---|---|---|
| `debug` | `http://10.0.2.2:9090/` | Desarrollo local (alias del `localhost` del host desde el emulador). |
| `release` | `https://businesscontrol.azurewebsites.net/` | Producción. |

- **Emulador → backend local:** dejá `10.0.2.2:9090` y levantá el backend primero.
- **Dispositivo físico → backend local:** `10.0.2.2` no resuelve; cambiá el `buildConfigField` de
  debug a la IP LAN de tu PC (ej. `http://192.168.1.X:9090/`), ambos en la misma red.

El prefijo `/api/v1` lo agrega la capa de red, no la base URL.

## Documentación

| Documento | Contenido |
|---|---|
| [`planeamiento/ARQUITECTURA.md`](docs/planeamiento/ARQUITECTURA.md) | Especificación técnica del estado objetivo (capas, red, sesión, offline, DI, testing). |
| [`planeamiento/PLAN.md`](docs/planeamiento/PLAN.md) | Roadmap por fases con estado (✅ / – / pendiente) y subtareas. |
| [`docs/API-CONTRACT.md`](docs/API-CONTRACT.md) | Contrato de la API REST (fuente de verdad del cliente). |
| [`docs/backend-analytics.md`](docs/backend-analytics.md) · [`docs/backend-profile.md`](docs/backend-profile.md) | Requerimientos pedidos al equipo de backend. |

## Convenciones

- Identificadores y clases de dominio en **español**; comentarios/docs en inglés por defecto.
- El cliente ramifica siempre por el campo `code` de la respuesta, nunca por `message`.
- DTOs solo en `data/remote/dto`; nunca se exponen a `presentation`.
- Cada use case de dominio ship con su test unitario.
- Toda PR deja `gradlew.bat :app:assembleDebug` en verde.

## Troubleshooting

| Síntoma | Causa probable | Solución |
|---|---|---|
| `Unsupported class file major version` | JDK distinto a 17 | Settings → Build Tools → Gradle → Gradle JDK → 17. |
| No conecta al backend en emulador | Backend caído o URL incorrecta | Levantar backend en `:9090`; verificar `10.0.2.2`. |
| No conecta en dispositivo físico | `10.0.2.2` no resuelve | Usar la IP LAN del host en el `buildConfigField` debug. |
| `SDK location not found` | Falta `local.properties` | Crear con `sdk.dir=<ruta-sdk>`. |
