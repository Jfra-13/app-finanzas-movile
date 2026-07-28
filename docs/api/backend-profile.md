# Requerimientos de Backend — Perfil, Cuenta y Gestión de Usuario

Documento de trabajo para el equipo de backend. Define **todo** lo que la app móvil necesita
para que las pantallas de **Perfil** y **Cuenta** dejen de mostrar botones "Próximamente" y
queden funcionales, más los huecos de **Categorías**, **Calendario** e **Historial de metas**.

> **Complementa** a [`API-CONTRACT.md`](API-CONTRACT.md) y a
> [`backend-analytics.md`](backend-analytics.md). Se reutilizan sus mismas
> convenciones: envelope de respuesta, decisión por campo `code` (nunca por `message`),
> prefijo `/api/v1`, JWT Bearer, montos decimales con 2 posiciones, fechas ISO
> (`2026-06-12T10:30:00`), zona horaria del servidor.
>
> **Fuente de verdad:** Swagger. Este documento es la especificación pedida; al implementarse,
> Swagger manda.

## Estado actual de los botones (qué necesita backend y qué no)

Pantalla **Perfil** (`ProfileActivity`):

| Botón | Estado hoy | Backend |
|---|---|---|
| Suscripción | stub "Próximamente" | **Sí** — producto completo (§7) |
| Cuenta | abre `AccountActivity` | parcial (ver abajo) |
| Categorías | funciona (listar + crear) | **Sí** — falta editar/borrar (§2) |
| Movimientos | funciona | ok |
| Apariencia | stub "Próximamente" | **No** — tema local, dark mode ya resuelto en el cliente |
| Notificaciones | stub "Próximamente" | **Sí** — push + preferencias (§7) |
| Ayuda y comentarios | stub "Próximamente" | **Sí** — endpoint de feedback (§7) |
| Invitar a un amigo | stub "Próximamente" | **Sí** — referidos (§7) |

Pantalla **Cuenta** (`AccountActivity`):

| Botón | Estado hoy | Backend |
|---|---|---|
| Correo electrónico | muestra el email de la sesión | falta `GET /me` real (§1.1) |
| Cambiar número de teléfono | stub "Próximamente" | **Sí** (§1.2) |
| Resetear estadísticas mensuales | stub "Próximamente" | **Sí** (§1.5) |
| Eliminar cuenta | stub "Próximamente" | **Sí** (§1.6) |
| Cerrar sesión | borra el token local | opcional: revocar refresh token (§1.7) |

## Principios (heredados de los otros docs)

1. **El cliente ramifica por `code`.** Todo texto es para humanos y puede cambiar sin aviso.
2. **Aislamiento por usuario.** La identidad sale del JWT; ningún endpoint recibe `usuarioId`.
3. **Retrocompatibilidad.** Agregar campos/endpoints, nunca romper los existentes.

---

## Orden de implementación (por impacto)

| # | Capacidad | Impacto |
|---|-----------|---------|
| 1 | Perfil de usuario (`GET/PUT /me`, foto, password, borrar, logout) | **Alto.** Hoy el perfil es una fachada: nombre/email salen sólo del JWT, la foto es un placeholder estático |
| 2 | CRUD de categorías (editar/borrar) | **Alto.** Sin esto la pantalla es sólo-agregar; un error no se puede corregir |
| 3 | Resumen diario / calendario | **Alto.** Desbloquea el calendario real y 2 gráficos nuevos (§5) |
| 4 | Historial de metas | Medio |
| 5 | Gráficos nuevos (se habilitan solos con #3 y el rango de fechas del doc de analíticas) | Medio |
| 7 | Features de producto (suscripción, notificaciones, referidos, feedback) | **Decisión de negocio primero.** No son "un endpoint" |

---

## 1. Perfil de usuario

Hoy el nombre y el email salen del `SessionManager` (decodificados del JWT). **No existe forma
de leer el perfil completo, editarlo, ni subir foto.** El avatar (`ivProfilePhoto`) es un
placeholder fijo.

### 1.1 `GET /api/v1/usuarios/me`

Devuelve el perfil del usuario autenticado. Fuente única para la cabecera de Perfil y el detalle
de Cuenta.

Respuesta (`code: "PROFILE_OK"`):
```json
{
  "data": {
    "id": 42,
    "nombre": "Juan Pérez",
    "email": "juan@example.com",
    "telefono": "+51987654321",
    "fotoUrl": "https://cdn.example.com/avatars/42.jpg",
    "negocio": "Servicio de taxi",
    "plan": "FREE"
  }
}
```

- `telefono`, `fotoUrl`, `negocio` pueden ser `null`.
- `plan`: `FREE` | `PRO` (o el catálogo que defina negocio). Ver §7 si Suscripción entra.

### 1.2 `PUT /api/v1/usuarios/me`

Edita los campos editables del perfil (nombre y teléfono). El email no se cambia acá (identidad).

Request (envía sólo lo que cambia):
```json
{ "nombre": "Juan P. Pérez", "telefono": "+51987654321" }
```

- Validación: `nombre` no vacío si viene; `telefono` en formato válido si viene.
- Respuesta (`code: "PROFILE_UPDATED"`): el perfil actualizado (mismo shape que §1.1).
- Errores: `400 VALIDATION_ERROR`.

> Cubre el botón **"Cambiar número de teléfono"** de Cuenta.

### 1.3 `POST /api/v1/usuarios/me/foto` (multipart)

Sube/reemplaza el avatar. `multipart/form-data`, campo `file` (JPEG/PNG).

- Validación: tipo de imagen y tamaño máximo (definir tope, ej. 5 MB) → `400 ARCHIVO_INVALIDO`.
- Respuesta (`code: "PHOTO_UPDATED"`): `{ "fotoUrl": "https://..." }`.
- Opcional: `DELETE /api/v1/usuarios/me/foto` para volver al placeholder (`code: "PHOTO_DELETED"`).

### 1.4 `POST /api/v1/usuarios/me/password`

Cambiar la contraseña **estando logueado** (distinto del flujo `forgot-password`, que es sin sesión
y con OTP).

Request:
```json
{ "actual": "claveVieja123", "nueva": "claveNueva456" }
```

- Validación: `actual` debe coincidir → si no, `400 PASSWORD_ACTUAL_INCORRECTA`.
  `nueva` cumple la política de contraseñas → `400 VALIDATION_ERROR`.
- Respuesta (`code: "PASSWORD_CHANGED"`).
- Recomendado: invalidar los refresh tokens activos salvo el del dispositivo actual.

### 1.5 `POST /api/v1/usuarios/me/reset-estadisticas`

Resetea las estadísticas mensuales del usuario (define negocio el alcance exacto: ¿borra
transacciones del mes?, ¿archiva?, ¿sólo pone contadores a cero?). **Acción destructiva.**

- Requiere confirmación en el cliente (ya se hace patrón `AlertDialog`).
- Respuesta (`code: "STATS_RESET"`).
- **Ojo:** decisión de negocio pendiente sobre qué borra exactamente. No implementar hasta
  tener esa definición.

> Cubre el botón **"Resetear estadísticas mensuales"** de Cuenta.

### 1.6 `DELETE /api/v1/usuarios/me`

Elimina la cuenta del usuario autenticado. **Acción irreversible.**

- Requiere reconfirmación en el cliente. Recomendado: pedir la contraseña en el body para
  confirmar identidad.
- Respuesta (`code: "ACCOUNT_DELETED"`). El cliente limpia sesión y vuelve a Login.
- Define negocio: borrado duro vs. soft-delete + período de gracia (recomendado soft-delete).

> Cubre el botón **"Eliminar cuenta"** de Cuenta.

### 1.7 `POST /api/v1/usuarios/logout` (opcional pero recomendado)

Hoy "Cerrar sesión" sólo borra el token local; el refresh token sigue válido en el servidor.

Request: `{ "refreshToken": "..." }` → revoca ese token.
Respuesta (`code: "LOGGED_OUT"`). Idempotente: token ya inválido también devuelve `200`.

---

## 2. CRUD de categorías

Hoy la API sólo tiene `GET /finanzas/categorias` y `POST /finanzas/categorias`. La pantalla de
Categorías puede listar y crear, **pero no editar ni borrar**. Un nombre mal escrito queda para
siempre.

### 2.1 `PUT /api/v1/finanzas/categorias/{id}`

Edita una categoría propia (nombre y/o color/ícono, según el modelo actual).

Request:
```json
{ "nombre": "Combustible", "color": "#FF7043" }
```

- Respuesta (`code: "CATEGORY_UPDATED"`): la categoría actualizada.
- Errores: `404 CATEGORIA_NO_ENCONTRADA`, `403 ACCESO_DENEGADO`, `400 VALIDATION_ERROR`.

### 2.2 `DELETE /api/v1/finanzas/categorias/{id}`

Elimina una categoría propia.

- **Decisión de negocio:** qué pasa con las transacciones que la usaban. Recomendado:
  reasignarlas a "Sin categoría" (`categoriaId = null`), no borrarlas.
- Respuesta (`code: "CATEGORY_DELETED"`).
- Errores: `404 CATEGORIA_NO_ENCONTRADA`, `403 ACCESO_DENEGADO`.

---

## 3. Resumen diario (calendario)

El `CalendarViewModel` documenta su propia limitación: **el backend no tiene endpoint por fecha,
así que sólo la semana en curso tiene datos reales**; cualquier otro día cae en `conDatos = false`.
Esto bloquea el calendario y el heatmap.

> El filtro `desde`/`hasta` de `GET /transacciones` ya está pedido en
> [`backend-analytics.md` §1](backend-analytics.md). Con eso alcanza para el
> drill-down por día, pero para pintar un mes entero conviene un endpoint agregado (una llamada
> en vez de 30).

### `GET /api/v1/finanzas/resumen-diario`

Agregado diario de ingresos/egresos para un mes. El teléfono sólo pinta.

| Query param | Tipo | Descripción |
|---|---|---|
| `mes` | `YYYY-MM` (opcional) | Mes a resumir. Default: mes en curso |

Respuesta (`code: "DAILY_SUMMARY_OK"` — lista, un item por día **con actividad**):
```json
{
  "data": [
    { "fecha": "2026-06-01", "ingresos": 120.00, "egresos": 40.00 },
    { "fecha": "2026-06-02", "ingresos": 0.00,   "egresos": 85.50 }
  ]
}
```

- Días sin movimientos: omitirlos (lista vacía si el mes no tuvo actividad, `200` + `data: []`).
- Habilita: calendario real por día + heatmap + flujo de caja diario (§5).

### 3.1 Filtro "Sin categoría" server-side (fix chico)

En `AnalyticsViewModel` (drill-down de la torta) hay un `ponytail:` marcando que "Sin categoría"
se filtra en el cliente porque la API no filtra sin-categoría. Agregar soporte de
`categoriaId` **nulo/vacío** en `GET /transacciones` para filtrar los movimientos sin categoría
server-side. Respuesta sin cambios.

---

## 4. Historial de metas

Hoy sólo existe `GET /finanzas/metas/actual`. No hay forma de ver metas pasadas ni si se
cumplieron. Alimenta un gráfico de cumplimiento histórico (§5).

### `GET /api/v1/finanzas/metas/historial`

| Query param | Tipo | Descripción |
|---|---|---|
| `meses` | entero (opcional) | Cuántos períodos hacia atrás. Default: 6 |

Respuesta (`code: "GOALS_HISTORY_OK"`):
```json
{
  "data": [
    { "periodo": "2026-05", "metaMensual": 3000.00, "utilidadReal": 3120.00, "cumplida": true },
    { "periodo": "2026-06", "metaMensual": 3000.00, "utilidadReal": 2450.00, "cumplida": false }
  ]
}
```

---

## 5. Gráficos nuevos habilitados

No requieren endpoints extra más allá de §3, §4 y el rango de fechas del doc de analíticas. El
teléfono usa **MPAndroidChart**; el cálculo vive en el servidor.

| Gráfico | Se alimenta de | Valor |
|---|---|---|
| **Heatmap de calendario** (gasto por día, estilo GitHub) | `GET /resumen-diario` (§3) | Alto impacto visual |
| **Flujo de caja diario** (área ingresos vs egresos del mes) | `GET /resumen-diario` (§3) | Ver el mes de un vistazo |
| **Balance acumulado / neto** (línea que sube) | derivable del `resumen-diario`, sin endpoint nuevo | Sensación de progreso |
| **Tendencia por categoría** (barras apiladas por mes) | `GET /tendencia-categorias?meses=N` (nuevo) | Ver en qué crece el gasto |
| **Cumplimiento de metas** (histórico) | `GET /metas/historial` (§4) | Motivación / accountability |

### 5.1 `GET /api/v1/finanzas/tendencia-categorias` (para el gráfico apilado)

| Query param | Tipo | Descripción |
|---|---|---|
| `meses` | entero (opcional) | Ventana de meses. Default: 6 |

Respuesta (`code: "CATEGORY_TREND_OK"`):
```json
{
  "data": [
    { "periodo": "2026-05", "categorias": { "Gasolina": 242.00, "Peaje": 90.00 } },
    { "periodo": "2026-06", "categorias": { "Gasolina": 320.00, "Peaje": 75.50 } }
  ]
}
```

---

## 7. Features de producto (decisión de negocio antes que endpoint)

Estos botones **no son "un endpoint"**: son productos. No implementar hasta tener la decisión de
negocio. Se listan para dejar registrado el hueco, no para construir ya.

- **Suscripción** → estado del plan, catálogo de planes, integración de pagos/billing, webhooks.
  El campo `plan` de §1.1 es el mínimo para empezar a mostrar estado.
- **Notificaciones** → registro de device token (FCM/push) + preferencias por tipo de alerta.
  Endpoints mínimos futuros: `POST /usuarios/me/dispositivos`, `PUT /usuarios/me/notificaciones`.
- **Invitar a un amigo (referidos)** → generar/consultar código de referido, atribución al
  registrarse, recompensa. `GET /usuarios/me/referido`.
- **Ayuda y comentarios (feedback)** → el más simple: `POST /soporte/feedback`
  `{ "categoria": "BUG|IDEA|OTRO", "mensaje": "..." }` → `code: "FEEDBACK_RECEIVED"`.

**No requiere backend:** *Apariencia* (tema claro/oscuro) es 100% cliente y ya funciona.

---

## Catálogo de nuevos códigos

Agregar al catálogo canónico (README-READINESS.md).

**Éxito:** `PROFILE_OK`, `PROFILE_UPDATED`, `PHOTO_UPDATED`, `PHOTO_DELETED`, `PASSWORD_CHANGED`,
`STATS_RESET`, `ACCOUNT_DELETED`, `LOGGED_OUT`, `CATEGORY_UPDATED`, `CATEGORY_DELETED`,
`DAILY_SUMMARY_OK`, `GOALS_HISTORY_OK`, `CATEGORY_TREND_OK`, `FEEDBACK_RECEIVED`.

**Error:**

| HTTP | `code` | Cuándo |
|---|---|---|
| 400 | `PASSWORD_ACTUAL_INCORRECTA` | La contraseña actual no coincide (§1.4) |
| 400 | `ARCHIVO_INVALIDO` | Foto con tipo/tamaño no permitido (§1.3) |
| 403 | `ACCESO_DENEGADO` | Recurso de otro usuario (categorías) |
| 404 | `CATEGORIA_NO_ENCONTRADA` | Categoría inexistente (§2) |

---

## Notas de consistencia

- **Montos**: decimal con 2 posiciones, misma moneda que el resto (S/).
- **Fechas de rango**: `YYYY-MM-DD` / `YYYY-MM`, zona del servidor.
- **Retrocompatibilidad**: los endpoints actuales no cambian de shape.
- **Vacío ≠ error**: listas vacías → `200` + `data: []`.
- **Acciones destructivas** (§1.5, §1.6, §2.2): documentar el efecto exacto antes de implementar;
  el cliente ya confirma con diálogo.
