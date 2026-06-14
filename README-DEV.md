# Guía de desarrollo — App Android (Frontend)

Cómo levantar, correr y trabajar este proyecto. Es el **cliente móvil nativo Android** de Finanzas Independientes; consume la API REST descrita en [README-FRONTEND.md](README-FRONTEND.md).

> Para la arquitectura del proyecto, ver [README.md](README.md). Para el contrato de la API, ver [README-FRONTEND.md](README-FRONTEND.md).

## Requisitos

| Herramienta | Versión | Notas |
|---|---|---|
| JDK | **17** | AGP 9 lo exige. Android Studio trae un JDK embebido que sirve. |
| Android Studio | Ladybug o superior | Recomendado. También se puede solo con CLI + SDK. |
| Android SDK | API 36 (compileSdk 36.1) | Instalar vía SDK Manager. |
| Gradle | 9.2.1 | No instalar a mano: usa el wrapper (`./gradlew`). |

Versiones del toolchain (fijadas en `gradle/libs.versions.toml`):

- AGP `9.0.1`, Kotlin/KSP `2.2.10`, Hilt `2.59.2`, Room `2.7.2`, Retrofit `2.9.0`, OkHttp `4.12.0`.

Configuración del dispositivo objetivo:

- `minSdk = 24` (Android 7.0), `targetSdk = 36`.

## Setup inicial

1. Clonar el repo y abrir la **carpeta raíz** en Android Studio (importa el proyecto Gradle solo).
2. Verificar `local.properties` → debe apuntar a tu Android SDK:
   ```properties
   sdk.dir=C:\\Users\\<tu-usuario>\\AppData\\Local\\Android\\Sdk
   ```
   Android Studio lo genera automáticamente. **No se commitea** (está en `.gitignore`).
3. Esperar el primer **Gradle Sync** (descarga dependencias).

No hace falta configurar `BASE_URL` a mano: viaja por `BuildConfig` según el build type (ver abajo).

## Correr la app

### Desde Android Studio
1. Seleccionar un emulador (AVD) o conectar un dispositivo físico con depuración USB.
2. Elegir el build variant **debug** (por defecto).
3. Run ▶ (`Shift+F10`).

### Desde la CLI

```bash
# Compilar el APK debug
./gradlew :app:assembleDebug

# Instalar en el dispositivo/emulador conectado
./gradlew :app:installDebug

# Tests unitarios (dominio, ViewModels)
./gradlew :app:testDebugUnitTest

# Limpiar build
./gradlew clean
```

> En Windows usar `gradlew.bat` en lugar de `./gradlew`.

## Conexión con el backend

La URL base se inyecta por `buildConfigField` (ver `app/build.gradle.kts`), **no está hardcodeada en el código**:

| Build type | `BASE_URL` | Cuándo |
|---|---|---|
| `debug` | `http://10.0.2.2:9090/` | Desarrollo local |
| `release` | `https://businesscontrol.azurewebsites.net/` | Producción |

**Importante sobre `10.0.2.2`:** es el alias del emulador Android hacia el `localhost` de tu máquina host. Si el backend corre en tu PC en el puerto `9090`, el emulador lo alcanza por esa IP.

- **Emulador → backend local**: dejar `http://10.0.2.2:9090/`. Levantar el backend primero (ver su propio README).
- **Dispositivo físico → backend local**: `10.0.2.2` **no funciona**. Cambiar el `buildConfigField` de debug a la IP LAN de tu PC (ej. `http://192.168.1.X:9090/`) y que ambos estén en la misma red.
- **Cleartext HTTP**: el manifest tiene `usesCleartextTraffic="true"` para permitir `http://` en debug. Producción usa `https://`.

El prefijo de rutas (`/api/v1`) lo agrega la capa de red, no la `BASE_URL`.

## Estructura del proyecto

Arquitectura por capas (`data` / `domain` / `presentation`) + `core`, con `presentation` dividida por feature:

```
app/src/main/java/com/example/finanzas_independientes_app/
├── core/
│   ├── network/     ← Retrofit, envelope ApiResponse, AppError, interceptores
│   └── session/     ← SessionManager (tokens en EncryptedSharedPreferences)
├── data/
│   ├── remote/      ← FinanzasApi + DTOs + mappers
│   ├── local/       ← Room (cache offline-first)
│   └── repository/  ← AuthRepository, FinanzasRepository
├── domain/
│   ├── model/       ← modelos de negocio (no DTOs)
│   └── usecase/
├── di/              ← módulos Hilt
└── presentation/    ← splash, onboarding, auth, business, dashboard,
                       transacciones, categorias, analytics
```

Regla: **la UI habla con ViewModels → ViewModels con repositories → repositories con Retrofit**. Nada por encima de `data` conoce a Retrofit.

## Convenciones

- DI con **Hilt** (no hay `ServiceLocator` ni factories manuales).
- Identificadores y clases en **español**, consistente con el código existente.
- DTOs solo en `data/remote/dto`; nunca se exponen a `presentation`.
- El cliente decide siempre por el campo `code` de la respuesta, nunca por `message`.
- Cada use case de dominio con su test unitario.
- **Toda PR debe dejar `./gradlew :app:assembleDebug` en verde.**

## Troubleshooting

| Síntoma | Causa probable | Solución |
|---|---|---|
| `Unsupported class file major version` | JDK distinto a 17 | Usar JDK 17 (Settings → Build Tools → Gradle → Gradle JDK). |
| App no conecta al backend en emulador | Backend caído o URL incorrecta | Levantar backend en `:9090`; verificar `10.0.2.2`. |
| App no conecta en dispositivo físico | `10.0.2.2` no resuelve | Cambiar a IP LAN del host en el `buildConfigField` debug. |
| `SDK location not found` | Falta `local.properties` | Crear con `sdk.dir=<ruta-sdk>`. |
| Sync falla por dependencias jitpack (MPAndroidChart) | Repo no resuelto | Ya está en `settings.gradle.kts` (`maven { url = "https://jitpack.io" }`). Re-sync. |
