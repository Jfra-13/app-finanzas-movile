# Plan Fase 6 — cierre de P0/P1 (ítems desbloqueados)

Fecha: 2026-07-14. Continúa `PLAN-INTEGRACION-BACKEND.md` (Fase 6 era el bucket
"Bloqueado"). Con las respuestas del backend, tres de sus ítems pasaron a
**contrato sellado** y son implementables. Este doc los baja a plan de ejecución.

## Estado y prerrequisitos

- **Contratos sellados**: P1.4 (`sinCategoria`), P1.5 (`metas/historial`, falta
  shape exacto), P1.3 (baja de cuenta).
- **Stack de merge del backend**: `analytics-filtro-fechas → sin-categoria →
  metas-historial → eliminar-cuenta`. **Pendiente de merge + deploy.** Se
  desarrolla contra la rama local (`http://10.0.2.2:9090/`); **no mergear el front
  a `main` hasta que el backend esté desplegado**.
- **PRs**: `gh` no autenticado en el entorno. Fases 1–5 commiteadas y pusheadas;
  los PRs quedan pendientes de `gh auth login`.
- **Fuera de alcance (sigue bloqueado)**: bucket P2-producto —
  recomendaciones, feedback, reset de estadísticas, suscripción/push. El backend
  no los construye hasta definir alcance.

## Convenciones (recordatorio, aplican a cada sub-fase)

- Regla de capas: **DTO → mapper → domain → repository → use case (con test solo
  si hay lógica real) → ViewModel → XML/ViewBinding**.
- Ramificar por `ApiCode`, **nunca** por `message`. Codes desconocidos → `UNKNOWN`.
- Dinero = `Double`. Identificadores de dominio en español. UI en clásico Android
  View + ViewBinding (no Compose).
- Use case solo cuando hay lógica de cliente; los fetch simples van ViewModel → repo.
- Cada commit compila (`:app:assembleDebug`) y pasa `:app:testDebugUnitTest`.
  Commits por unidad de trabajo; un PR por sub-fase.

## Orden de ejecución

1. **6.1 — P1.4 `sinCategoria`** (más chico, borra deuda client-side existente).
2. **6.2 — P1.5 `metas/historial`** (chico; requiere shape del backend antes de escribir).
3. **6.3 — P1.3 baja de cuenta** (más grande; seguridad + UX de reactivación).

Independientes entre sí; se pueden encadenar como PRs chicos.

---

## Sub-fase 6.1 — Filtro `sinCategoria` server-side (P1.4)

**Rama**: `feat/analytics-sin-categoria-server-filter`.

**Contrato sellado**:
- `GET /transacciones?sinCategoria=true` devuelve solo transacciones sin categoría.
- `sinCategoria=true` **+** `categoriaId` → `400 PARAMETRO_INVALIDO` (mutuamente
  excluyentes; el cliente debe evitar mandarlos juntos).
- `sinCategoria=false` o ausente = sin filtro (comportamiento actual).

**Objetivo**: reemplazar el workaround client-side que hoy existe en
`AnalyticsViewModel.cargarDetalleCategoria` (bucket "Sin categoría" trae egresos y
filtra `it.categoriaId == null` en memoria, marcado con comentario `ponytail:`).

**Trabajo**:
- `FinanzasApi.listarTransacciones`: agregar `@Query("sinCategoria") sinCategoria: Boolean? = null`.
- `FinanzasRepository` + `FinanzasRepositoryImpl`: propagar el parámetro.
- `AnalyticsViewModel.cargarDetalleCategoria`: para el bucket "Sin categoría" llamar
  `listarTransacciones(sinCategoria = true)` y **eliminar el filtro en memoria** y su
  comentario `ponytail:`. Garantizar que nunca se envíe `sinCategoria=true` junto con
  `categoriaId`.
- `ApiCode`: `PARAMETRO_INVALIDO` ya está catalogado — sin cambios.

**Codes nuevos**: ninguno.

**Done**: compila + tests; el drill-down "Sin categoría" trae los datos del server;
no queda filtrado client-side. `PARAMETRO_INVALIDO` manejado como error genérico.

---

## Sub-fase 6.2 — Historial de metas (P1.5)

**Rama**: `feat/metas-historial`.

**Contrato parcial**: `GET /finanzas/metas/historial?meses=N`. **Falta el shape
exacto de la respuesta** — pedirlo al backend (Swagger `/v3/api-docs` o captura)
antes de escribir DTO/mapper. Confirmar también el path exacto (`/finanzas/metas/…`
vs otro) y el default de `meses`.

**Decisión de producto (nuestra)**: el historial vive como entrada **"Historial de
metas"** dentro de Profile/Account (pantalla o sección propia, lista simple). No
crea destino de bottom-nav nuevo.

**Trabajo (una vez con el shape)**:
- DTO `MetaHistorialItemDTO` (campos según Swagger), `domain/model/MetaHistorial…`,
  mapper.
- `FinanzasApi`: `@GET("api/v1/finanzas/metas/historial")` con `@Query("meses")`.
- `FinanzasRepository` + Impl: `obtenerHistorialMetas(meses: Int? = null)`.
- UI: entrada en Profile/Account → pantalla/sección con RecyclerView + adapter
  (`MetaHistorialAdapter`) y estados loading/empty/error reusando `ViewStateHelper`.

**Codes nuevos**: a confirmar con el shape (probablemente reusa `GOALS_PROGRESS_OK`
u otro ya catalogado).

**Prerrequisito**: **shape de la respuesta** (pedido a backend pendiente).

**Done**: compila + tests; la pantalla lista el historial; estados cubiertos.

---

## Sub-fase 6.3 — Baja de cuenta (P1.3)

**Rama**: `feat/eliminar-cuenta`.

**Contrato sellado** (soft-delete + gracia de 30 días):
- `POST /api/v1/usuarios/me/eliminar`, body `{ "password": "..." }`.
  (Se movió de `DELETE` a `POST` — el body en `DELETE` es frágil en la red).
- `200 ACCOUNT_DELETED`. Password incorrecta → `401 CREDENCIALES_INVALIDAS`
  (**code nuevo**). Password ausente → `VALIDATION_ERROR`.
- Al eliminar, el backend revoca todos los refresh tokens **y rechaza el access
  token en el acto** (el filtro JWT carga el usuario de la DB por request). No hay
  ventana de 15 min que modelar en el front.
- **Reactivación por login**: loguearse dentro de los 30 días reactiva la cuenta con
  todos los datos, sin endpoint extra. La respuesta de login trae un campo **nuevo
  y additive `cuentaReactivada: Boolean`** — `true` solo cuando ese login reactivó
  una cuenta pendiente de baja (`false` en login normal y en refresh).
- Login pasada la gracia → `404 USUARIO_NO_ENCONTRADO` (ya catalogado). Purga física
  en job diario 03:00.
- Copy sugerido de baja: *"Tu cuenta se eliminará en 30 días; iniciá sesión para
  recuperarla"*.

**Trabajo**:
- `ApiCode`: agregar `ACCOUNT_DELETED` y `CREDENCIALES_INVALIDAS`.
- `ErrorMessages`: `CREDENCIALES_INVALIDAS` → "Contraseña incorrecta." (uso **inline**
  en el diálogo de baja, **no** por el flujo `UNAUTHORIZED` de sesión expirada, para
  no expulsar al usuario por tipear mal la password).
- DTO `DeleteAccountRequest { password }`. Login DTO (`AuthData`/`LoginDTO`): agregar
  `cuentaReactivada: Boolean` (additive; default `false` si el server no lo manda).
- API auth (`FinanzasApi`, junto a `usuarios/*`): `@POST("api/v1/usuarios/me/eliminar")`.
- `AuthRepository` + impl: `eliminarCuenta(password): ApiResult<Unit>`; `AuthMapper`
  propaga `cuentaReactivada` al modelo de sesión/login.
- `SessionManager`: en `200 ACCOUNT_DELETED`, limpiar sesión local y navegar a Login
  (reusar el flujo de logout existente).
- **Flujo de login**: leer `cuentaReactivada`; si `true`, mostrar aviso *"Tu cuenta
  iba a eliminarse; quedó reactivada"* y ofrecer rehacer la baja con el mismo endpoint.
- **UI**: "danger zone" en `AccountActivity` — acción "Eliminar cuenta" → diálogo con
  confirmación por password → llamada. Mapear `CREDENCIALES_INVALIDAS` a error inline
  del campo password.

**Codes nuevos**: `ACCOUNT_DELETED`, `CREDENCIALES_INVALIDAS`.

**Done**: compila + tests; baja con password → logout local + Login; password
incorrecta muestra error inline sin cerrar sesión; login reactivador muestra el aviso.

---

## Pedidos pendientes a backend

1. **Shape de `GET /finanzas/metas/historial`** (P1.5) — bloquea 6.2.
2. **Deploy del stack** `analytics-filtro-fechas → sin-categoria → metas-historial →
   eliminar-cuenta` para poder verificar contra el entorno real.

## Verificación y entrega

- Cada sub-fase: `:app:assembleDebug` + `:app:testDebugUnitTest` verdes; commits por
  unidad de trabajo; un PR chico por rama.
- Verificación funcional pendiente de: backend levantado en local + dispositivo/emulador.
- PRs pendientes de `gh auth login`.
