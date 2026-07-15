Fase 5 desbloqueada — shapes P2 definitivos (verificados contra el código del backend):

  1. Presupuestos — /api/v1/finanzas/presupuestos

  - POST body { "categoriaId": 3, "montoMensual": 500.00 } (montoMensual ≥ 0.01). Es upsert: re-enviar la misma
  categoriaId reemplaza el tope (un presupuesto por usuario+categoría). → BUDGET_SET, data = el presupuesto con estado.
  Errores: 404 CATEGORIA_NO_ENCONTRADA, VALIDATION_ERROR.
  - GET → BUDGETS_OK, data lista de:

  {
    "id": 1,
    "categoriaId": 3,
    "categoriaNombre": "Combustible",
    "montoMensual": 500.00,
    "gastadoMes": 620.00,
    "restante": -120.00,
    "consumoPct": 124.00,
    "excedido": true
  }

    Contrato: restante va a negativo al excederse; consumoPct no tiene tope (puede superar 100); excedido = gasto pasó el
  tope. Estado siempre del mes en curso.
  - DELETE /{id} → BUDGET_DELETED. Errores: 404 PRESUPUESTO_NO_ENCONTRADO (code nuevo, agréguenlo al catálogo), 403
  ACCESO_DENEGADO.

  2. Comparación — GET /finanzas/analiticas/comparacion-categorias

  - Params opcionales: desde/hasta (YYYY-MM-DD inclusivos; sin params = mes en curso hasta hoy) y compararCon =
  PERIODO_ANTERIOR (default, ventana previa del mismo largo) | MISMO_PERIODO_ANIO_ANTERIOR (ojo: ANIO, sin ñ). desde >
  hasta → 400 RANGO_FECHAS_INVALIDO.
  - → CATEGORY_COMPARISON_OK:

  {
    "periodoActual": { "desde": "2026-07-01", "hasta": "2026-07-14" },
    "periodoAnterior": { "desde": "2026-06-17", "hasta": "2026-06-30" },
    "categorias": [
      { "categoria": "Combustible", "actual": 320.0, "anterior": 280.0, "deltaAbs": 40.0, "deltaPct": 14.29 }
    ],
    "totalActual": 320.0,
    "totalAnterior": 280.0,
    "totalDeltaPct": 14.29
      }

        deltaPct: null cuando anterior es 0 (aplica también a totalDeltaPct). Lista = unión de categorías de ambos períodos;
      las sin categoría entran con su bucket.

      3. Proyección — GET /finanzas/proyeccion-mensual (sin params)

      - Sin meta activa → 404 META_NO_ENCONTRADA (mismo manejo "Sin meta activa" del Dashboard, como asumían).
      - → MONTHLY_PROJECTION_OK:

      {
        "periodo": "2026-07",
        "diasTranscurridos": 14,
        "diasDelMes": 31,
        "diasHabilesRestantes": 12,
        "ingresoActual": 1800.0,
        "egresoActual": 700.0,
        "utilidadActual": 1100.0,
        "ingresoProyectado": 3985.71,
        "egresoProyectado": 1550.0,
        "utilidadProyectada": 2435.71,
        "metaMensual": 3000.0,
        "brechaProyectada": -564.29,
        "enCamino": false
      }

        Método: run-rate lineal por días calendario (proyectado = actual × diasDelMes / diasTranscurridos). brechaProyectada
      negativa = por debajo de la meta; enCamino = proyección alcanza o supera la meta.

      4. Salud financiera ampliada — GET /finanzas/salud-financiera

      - Item: { "tipo": "ALERTA"|"FELICITACION", "code": string, "severidad": "ALTA"|"MEDIA"|"BAJA", "mensaje": string,
      "categoriaId": Long|null }.
      - categoriaId viene solo en PRESUPUESTO_EXCEDIDO (una señal por presupuesto excedido — puede haber varias); en el resto
      es null. Deep-link solo ahí, como planeaban.
      - Catálogo completo con severidad fija: GASTO_DIARIO_ALTO (MEDIA), META_CERCA (BAJA, felicitación), META_EN_RIESGO
      (ALTA), EGRESOS_SUPERAN_INGRESOS (ALTA), TASA_AHORRO_BAJA (MEDIA), TASA_AHORRO_SANA (BAJA, felicitación),
      PRESUPUESTO_EXCEDIDO (ALTA), PROYECCION_BAJO_META (MEDIA).

      Todo esto ya está en Swagger al levantar el backend (/swagger-ui.html). Code nuevo a catalogar:
      PRESUPUESTO_NO_ENCONTRADO.

                    ---
                    Detalle que les va a importar: en su borrador escribieron MISMO_PERIODO_AÑO_ANTERIOR implícito — el valor real es
                    MISMO_PERIODO_ANIO_ANTERIOR. Y PRESUPUESTO_NO_ENCONTRADO no estaba en el catálogo de codes que armaron en la sección 5
                    de su respuesta.
                    Jump to bottom (ctrl+End) ↓