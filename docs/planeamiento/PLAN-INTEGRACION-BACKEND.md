# Plan de integración frontend — endpoints backend post-auditoría

Fecha: 2026-07-14. Fuentes: `RESPUESTA-BACKEND-AUDIT.md`, `RESPUESTA-FRONT-A-BACKEND.md`,
`docs/endpoints-audit.md`. Estado backend: todo P0/P1 (salvo P1.3/P1.5) y P2 parcial ya
implementado en la rama `feature/analytics-filtro-fechas` del backend, **pendiente de merge**.

## Estado actual del front

`FinanzasApi.kt` consume los 19 endpoints base de la sección A. **No consume nada** de lo
nuevo: ni filtros `desde`/`hasta`, ni `resumen-diario`, ni `tendencia` con granularidad, ni
`ingresos-por-dia-semana`, ni `GET/PUT /usuarios/me`, ni `logout`, ni `PUT/DELETE
categorias/{id}`, ni presupuestos/comparación/proyección. `ApiCode.kt` tampoco tiene los
codes nuevos (falta incluso `RANGO_FECHAS_INVALIDO`).

## Prerrequisito global (backend)

- **Merge + deploy de `feature/analytics-filtro-fechas`**. Hasta entonces nada de este plan
  es verificable contra el entorno real. Se puede desarrollar contra la rama local del
  backend (`http://10.0.2.2:9090/`), pero no mergear al main del front sin backend desplegado.

---

## Fase 0 — Catálogo de codes y contratos (sin UI)

Base para todas las fases. Rama sugerida: la fase 1 la incluye (no amerita PR propio).

- `ApiCode.kt`: agregar éxito `DAILY_SUMMARY_OK`, `TREND_OK`, `WEEKDAY_INCOME_OK`,
  `PROFILE_OK`, `PROFILE_UPDATED`, `LOGGED_OUT`, `CATEGORY_UPDATED`, `CATEGORY_DELETED`,
  `BUDGET_SET`, `BUDGETS_OK`, `BUDGET_DELETED`, `CATEGORY_COMPARISON_OK`,
  `MONTHLY_PROJECTION_OK`; errores `PARAMETRO_INVALIDO`, `RANGO_FECHAS_INVALIDO`; señales de
  salud `EGRESOS_SUPERAN_INGRESOS`, `TASA_AHORRO_BAJA`, `TASA_AHORRO_SANA`,
  `PRESUPUESTO_EXCEDIDO`, `PROYECCION_BAJO_META`.
- `ErrorMessages.kt`: mensajes para los codes de error nuevos.

**¿Tengo todo?** Sí. Codes listados en `RESPUESTA-BACKEND-AUDIT.md` §5.

## Fase 1 — Sesión y perfil (P1.2, P0.6, P1.1)

Prioridad máxima: el logout solo-local es un hueco de seguridad reconocido por ambos lados.
Rama sugerida: `feat/profile-session-server`.

- `POST /usuarios/logout`: enviar refresh token al cerrar sesión (idempotente, funciona con
  access vencido). Integrar en el flujo de logout existente de `SessionManager` + UI.
- `GET /usuarios/me` (`PROFILE_OK`): shape `{id, nombre, email, telefono, fotoUrl,
  tipoNegocio, plan}` — campo `tipoNegocio` (decisión sellada), `fotoUrl`/`plan` llegan
  `null` pero se bindean desde ya. Alimenta `ProfileActivity` / `AccountActivity` (hoy sin
  ViewModel: crear `ProfileViewModel`).
- `PUT /usuarios/me` (`PROFILE_UPDATED`): update parcial `nombre`/`telefono`, email no
  editable. Devuelve perfil actualizado.
- Nuevos: DTOs (`PerfilDTO`, `UpdatePerfilRequest`), mapper a `domain/model/Perfil`,
  `PerfilRepository` + impl, use cases con test (`ObtenerPerfilUseCase`,
  `ActualizarPerfilUseCase`, `LogoutUseCase`).

**¿Tengo todo?** Sí. Contratos completos en la respuesta del backend.

## Fase 2 — CRUD de categorías (P0.5)

Rama sugerida: `feat/categorias-crud`.

- `PUT /categorias/{id}` (`CATEGORY_UPDATED`): edita solo `nombre` (`tipo` inmutable — no
  ofrecer edición de tipo en UI).
- `DELETE /categorias/{id}` (`CATEGORY_DELETED`): las transacciones quedan "Sin categoría";
  el backend borra los presupuestos de esa categoría.
- Manejo de codes: categoría base del sistema → `403 ACCESO_DENEGADO` (deshabilitar
  editar/borrar en UI para categorías base, no solo reaccionar al error); ajena →
  `404 CATEGORIA_NO_ENCONTRADA`.
- UI en `CategoriasActivity`/`CategoriasViewModel` + invalidar caché Room de categorías.

**¿Tengo todo?** Sí. Falta confirmar en `CategoriaDTO` si hay flag "categoría base" para
deshabilitar acciones preventivamente; si no lo hay, pedirlo al backend (nice-to-have, el
403 cubre el caso mientras tanto).

## Fase 3 — Filtros de fechas y calendario (P0.1, P0.2)

Rama sugerida: `feat/transacciones-rango-fechas`.

- `GET /transacciones?desde=YYYY-MM-DD&hasta=YYYY-MM-DD`: agregar `@Query` en
  `FinanzasApi.listarTransacciones`, propagar por repository → `TransaccionesViewModel`.
  UI de rango en `TransaccionesActivity`. Manejar `400 RANGO_FECHAS_INVALIDO` (y validar
  client-side `desde <= hasta` antes de llamar).
- `GET /resumen-diario?mes=YYYY-MM` (`DAILY_SUMMARY_OK`): `[{fecha, ingresos, egresos}]`,
  solo días con actividad, ascendente. Alimenta `CalendarActivity`/`CalendarViewModel`
  (hoy sin datos server-side por día). Sin `mes` = mes en curso.

**¿Tengo todo?** Sí.

## Fase 4 — Analytics server-side (P0.3, P0.4, P1.6)

Rama sugerida: `feat/analytics-server-granularity`. Desbloquea los controles que hoy están
deshabilitados (`btnGranSemana`, `btnPeriodo1M` — ver comentario "Fase 2" en `FinanzasApi.kt`).

- `GET /tendencia?granularidad=SEMANA|MES&ventana=N` (`TREND_OK`): shape
  `{periodos, ingresos, egresos}` arrays paralelos, más viejo primero. `SEMANA` rotula con
  el lunes `yyyy-MM-dd` → formatear label client-side (decisión sellada). Habilitar toggle
  "Semana" y ventana "1M" (`MES&ventana=1`). `tendencia-mensual` queda para compat hasta
  migrar; luego retirar su uso.
- `GET /ingresos-por-dia-semana?ventana=N` (`WEEKDAY_INCOME_OK`): siempre 7 items
  lunes→domingo, `dia` en mayúsculas sin tildes. Gráfico "¿Qué día ganás más?" con ventana
  seleccionable (default 4 semanas).
- `resumen-categorias?desde&hasta`: agregar params; sin params conserva mes en curso.
- Borrar el bloque de comentario "Fase 2 — EN DESARROLLO" de `FinanzasApi.kt` al cerrar.

**¿Tengo todo?** Sí. Contratos completos.

## Fase 5 — P2 adelantado: presupuestos, comparación, proyección, salud

Rama sugerida: `feat/presupuestos-proyeccion`. Última porque es funcionalidad nueva, no
deuda de integración.

- Presupuestos: `POST`/`GET /finanzas/presupuestos`, `DELETE /presupuestos/{id}`
  (`BUDGET_SET`, `BUDGETS_OK`, `BUDGET_DELETED`), con `consumoPct`/`excedido` del mes.
  UI probable: sección en Categorías o card en Analytics.
- `GET /finanzas/analiticas/comparacion-categorias` (`CATEGORY_COMPARISON_OK`):
  `deltaPct: null` cuando la base es 0 (mostrar "—", no 0 %).
- `GET /finanzas/proyeccion-mensual` (`MONTHLY_PROJECTION_OK`): run-rate lineal; sin meta →
  `404 META_NO_ENCONTRADA` (mismo manejo "Sin meta activa" del Dashboard).
- Salud financiera ampliada: pipeline listo en ambos lados; render de `severidad`
  (`ALTA`/`MEDIA`/`BAJA`) y deep-link por `categoriaId` en `PRESUPUESTO_EXCEDIDO`.

**¿Tengo todo?** No del todo — ver "Necesito del backend" abajo. Además **dos decisiones
de producto nuestras**: (a) en qué pantalla vive salud financiera (card Dashboard vs
Analytics), (b) dónde vive la UI de presupuestos.

## Fase 6 — Bloqueado (no planificar implementación todavía)

| Ítem | Bloqueado por |
|---|---|
| P1.4 `sinCategoria=true` en transacciones | Backend: implementado en su rama **sin commit**. No tocar `FinanzasApi.kt` hasta que esté mergeado y desplegado. Rama futura acordada: `feat/analytics-sin-categoria-server-filter`. Contrato sellado: `sinCategoria=true` + `categoriaId` → `400 PARAMETRO_INVALIDO`; `sinCategoria=false` = sin filtro. |
| P1.5 `GET /metas/historial?meses=N` | Acordado on-the-fly cuando se pida. Único ítem implementable hoy sin decisión de producto — si se quiere adelantar, pedirlo al backend primero. |
| P1.3 `DELETE /usuarios/me` | Decisión de producto nuestra: soft-delete vs físico + período de gracia. |
| Reset estadísticas, recomendaciones, feedback, suscripción/push | Decisión de producto. Backend explícito: no implementar hasta definir alcance. |

---

## Necesito del backend (resumen)

1. **Merge + deploy** de `feature/analytics-filtro-fechas` (prerrequisito de todo).
2. **Shapes exactos de P2** (fase 5): la respuesta a la auditoría da codes pero no el JSON
   completo de `POST/GET presupuestos`, `comparacion-categorias` (¿qué params de período
   recibe?) ni `proyeccion-mensual`. Dicen que está en Swagger (`/v3/api-docs`) — con un
   export o captura alcanza.
3. **Commit de P1.4** con el contrato acordado (`sinCategoria` booleano) + el test de que
   el listado sin filtros sigue trayendo huérfanas (cambio inner join → IS NULL).
4. (Nice-to-have, fase 2) flag "categoría base del sistema" en `CategoriaDTO` si no existe,
   para deshabilitar editar/borrar en UI sin esperar el 403.

## Orden y criterio

Fases 1→4 son deuda de integración con contrato cerrado: se pueden encadenar como PRs
chicos e independientes (cada una compila y pasa `testDebugUnitTest` sola). Fase 5 depende
del punto 2 de arriba. Cada fase respeta la regla de capas (DTO → mapper → domain →
repository → use case con test → ViewModel → XML/ViewBinding) y ramifica por `ApiCode`,
nunca por `message`.
