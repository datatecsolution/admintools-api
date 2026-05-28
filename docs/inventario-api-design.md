# Diseño — Subsistema de Inventario del API admin-tools

**Fecha**: 2026-05-25
**Estado**: Borrador para revisión
**Repo API**: `/Users/jdmayorga/Desktop/admintools`
**Repo Swing (migraciones Flyway + SPs)**: `/Users/jdmayorga/IdeaProjects/adminTools`
**Autor**: jdmayorga + Claude

---

## 1. Contexto y motivación

US-018 (ProductoController) arrancó como "completar el CRUD de productos", pero la exploración reveló que el modelo de datos lo contradice:

- La entidad JPA `Articulo` del API mapea a la **vista** `articulo_view` y está marcada `@Immutable` → es **solo lectura**. Un `PUT`/`save` por esa entidad no escribe nada (Hibernate lo ignora silenciosamente).
- El **stock (existencia) no es una columna**: se deriva del kardex (suma de movimientos). La vista lo calcula con la función `f_existencia_y_ordenes(codigo_articulo, 1)` — con **bodega 1 hardcodeada**.
- La gestión de inventario hoy la hace la **app Swing de escritorio** escribiendo en tablas de detalle; el API solo lee.

Por tanto, integrar inventario en el API no es "llenar un gap" de US-018: es diseñar un **subsistema de inventario** que respete el modelo de kardex existente. Este documento fija ese diseño antes de codear.

---

## 2. Cómo funciona el inventario hoy (confirmado contra la BD `admin_tools`)

### 2.1 Tipos de movimiento (`tipo_movimiento_kardex`)

| código | tipo |
|---|---|
| 1 | Entrada |
| 2 | Salida |
| 3 | Saldos (saldo corriente tras cada movimiento) |

### 2.2 Modelo de tablas del kardex

```
articulo                  → maestro del producto (TABLA real, escribible)
articulo_view             → vista @Immutable: articulo + existencia (bodega 1) + filtro estado=1
articulo_kardex           → header por (codigo_articulo, codigo_bodega) → codigo_kardex
movimiento_kardex         → movimientos: codigo_tipo_movimiento, cantidad, precio_unidad, total
detalle_movimiento_kardex → liga kardex ↔ movimiento (fecha, descripcion, no_documento)
bodega / departamento     → bodegas (espejo): 1=Tienda Principal, 2=Bodega 1
```

**El stock actual de un artículo en una bodega = el último `movimiento_kardex` tipo 3 (saldo)** de su kardex. Eso es lo que leen `f_can_saldo_kardex(art, bodega)` y `f_existencia_y_ordenes(art, bodega)` (esta última resta órdenes pendientes).

### 2.3 El inventario es event-driven por TRIGGERS en tablas de detalle

Cada operación de negocio inserta una fila de detalle y un **trigger `BEFORE INSERT`** calcula el kardex llamando al SP correcto. Patrón común de cada trigger: busca `cod_kardex` de (artículo, bodega) → si no existe lo crea (`INSERT articulo_kardex` + movimiento inicial) → llama al SP de la operación → marca `NEW.agrega_kardex = 1`.

| Operación | Tabla de detalle (trigger) | SP que llama | Dirección | Bodega |
|---|---|---|---|---|
| **Venta** | `detalle_factura` (`detalle_factura_b_insert`) | `crear_venta_kardex` (bien, tipo_articulo=1) / `crear_venta_insumo_kardex` (insumo, tipo=2) | 🔴 Salida | **1 hardcoded** |
| **Compra** | `detalle_factura_compra` (`detalle_compra_b_inset`) | `crear_compa_kardex` (o `crear_inventario_inicial_kardex` si 1er mov) | 🟢 Entrada | `NEW.codigo_bodega` |
| **Devolución de venta** | `detalle_devoluciones` (`detalle_devolucion_b_inset`) | `crear_dev_venta_kardex` | 🟢 Entrada | `cajas.codigo_bodega` |
| **Devolución de compra** | `detalle_devoluciones_compra` (`detalle_devolucion_compra_b_i`) | `crear_dev_compa_kardex` | 🔴 Salida | `NEW.codigo_bodega` |
| **Requisición (traslado)** | `detalle_requisicion` (`d_requisicion_b_insert`) | `crear_requisicion_entrada_kardex` (destino) + `crear_requisicion_salida_kardex` (origen) | 🟢+🔴 | origen → destino |
| **Ajuste manual** *(a descontinuar)* | *(sin tabla de detalle)* | `CALL crear_ajuste_inventario_kardex` directo | depende | — |

Los triggers de venta (`detalle_factura_b_insert`) también existen en las BD de caja (`admin_tools_caja_2`, `_3`), que escriben al kardex central de `admin_tools`.

### 2.4 Stored procedures de escritura (todos llaveados por `cod_kardex`)

```
ENTRADAS:  crear_compa_kardex(cod_kardex, no_factura, cantidad, precio)
           crear_inventario_inicial_kardex(cod_kardex, cantidad, precio, referencia)
           crear_ajuste_inventario_kardex(cod_kardex, cantidad, precio, referencia)   # a descontinuar
           crear_requisicion_entrada_kardex(cod_kardex, cod_requisicion, cantidad, precio)
SALIDAS:   crear_venta_kardex(cod_kardex, no_factura, cantidad)
           crear_venta_insumo_kardex(bodega, articulo, cantidad, desc, no_factura)
           crear_dev_venta_kardex(cod_kardex, no_factura, cantidad, precio)
           crear_dev_compa_kardex(cod_kardex, no_factura, cantidad, precio)
           crear_requisicion_salida_kardex(cod_kardex, cod_requisicion, cantidad)
HEADER:    get_cod_kardex(articulo, bodega)  # función; identifica/obtiene el kardex
```

Definiciones en `V7__recrear_funciones_procedures_triggers.sql` (repo Swing).

---

## 3. Decisiones de diseño

### D1 — La escritura de inventario se hace a NIVEL DOCUMENTO, no llamando SPs

El API **inserta en la tabla de detalle correcta** (`detalle_factura_compra`, `detalle_requisicion`, etc.) dentro de una transacción, y **los triggers existentes hacen el kardex**. No se reimplementa la lógica de kardex en Java ni se llaman los SPs directamente. Esto mantiene el inventario **consistente con el Swing y las cajas** (una sola fuente de verdad: los triggers + SPs en la BD).

> Excepción potencial: el ajuste manual directo (`crear_ajuste_inventario_kardex`) queda **descontinuado** — ver D3.

### D2 — Modelo de stock: tabla de saldos materializada + abstracción de lectura

**Problema con la vista actual**: `bodega=1` hardcodeada (no sirve multi-bodega) y recalcula con una **función escalar por fila** (lenta en listados grandes; el kardex crece a diario por las compras).

**Decisión**:
- El stock se lee detrás de una **abstracción de repositorio** en el API: `getExistencia(codigoArticulo, codigoBodega)` / `getExistencias(filtro, bodega, pageable)`. Los endpoints no dependen del mecanismo interno.
- **Objetivo de almacenamiento**: una **tabla de saldos materializada** `existencia_articulo_bodega (codigo_articulo, codigo_bodega, cantidad, fecha_actualizacion)`, mantenida por los mismos triggers/SPs del kardex en cada movimiento.
  - El **kardex (`movimiento_kardex`) sigue siendo el libro mayor inmutable** (auditoría).
  - La tabla de balance es el **saldo actual cacheado**, siempre consistente (se actualiza en la misma transacción del movimiento).
  - Lectura O(1) indexada, multi-bodega, escala para reportes.
- **Camino pragmático (fases)**: arrancar leyendo con `f_can_saldo_kardex(art, bodega)` parametrizado por bodega (correcto y multi-bodega ya), y **migrar al mecanismo materializado** cuando el rendimiento lo pida — sin cambiar los endpoints, gracias a la abstracción.

### D3 — Ajustes de inventario como DOCUMENTOS (no "setear stock")

No habrá endpoint de "fijar existencia a X". Todo cambio es un documento auditable:

- **Sobrante** (físico > sistema) → **entrada**: se modela como **compra** o como un documento de **"ajuste de inventario" / "inventario inicial"** (nombre configurable).
- **Faltante** (sistema > físico, el caso común) → **salida**: **requisición** desde la bodega de trabajo hacia una bodega **"Pérdidas"**.

> **Implementación refinada (INV-9 / V25, 2026-05-28):** los sobrantes
> NO necesitan tabla ni endpoint propios. Se modelan como **compras a
> proveedores ficticios** (`proveedor.es_ajuste = 1`). Eso reusa todo
> el flujo de INV-5 (validación, trigger `detalle_compra_b_inset`,
> SP `crear_compa_kardex` con fix V19/V20) sin agregar infraestructura.
>
> **Simetría completa del modelo de ajustes:**
>
> | Caso         | Endpoint         | Sujeto ficticio del documento     |
> |--------------|------------------|-----------------------------------|
> | Faltante     | `POST /requisitions` | bodega **Pérdidas** (destination)  |
> | Sobrante     | `POST /purchases`    | proveedor con `es_ajuste=1`        |
> | Inicial      | `POST /purchases`    | proveedor **Inventario inicial**   |
> | Donación     | `POST /purchases`    | proveedor **Donación recibida**    |
>
> **Seeds en V25** (Swing common): proveedor `Inventario inicial` (id=1,
> preexistente) marcado `es_ajuste=1`; nuevos `Sobrante conteo físico` y
> `Donación recibida` con `es_ajuste=1`. La columna `proveedor.es_ajuste`
> permite al frontend filtrar el selector de proveedores reales
> (`WHERE es_ajuste=0` en el flujo de compras de verdad) sin hardcodear
> IDs.
>
> **Sin efectos colaterales en cuentas por pagar:** ni el trigger de
> compra ni el SP `crear_compa_kardex` tocan `cuentas_por_pagar`. La CXP
> se mantiene manual desde el Swing (proceso del negocio). Las compras
> ficticias entran con `paymentAmount=0` y no aparecen como deuda
> automática.
>
> **Reportes naturales** sin código nuevo: `GET /purchases?supplier={id}&from=&to=`
> lista todos los ajustes del motivo correspondiente. El SP histórico
> `crear_ajuste_inventario_kardex` queda formalmente descontinuado para
> nuevo desarrollo (no se le aplica el fix V19/V20 — ya no es ruta de
> ajuste).

### D4 — Multi-bodega

La app de pedidos actual solo ve bodega 1 (Tienda Principal) y **puede quedarse así**. La **app nueva (administración)** necesitará stock **por bodega**. El diseño es multi-bodega desde el modelo de datos (la abstracción de stock recibe `codigoBodega`); los endpoints aceptan/filtran por bodega.

### D5 — Conceptos configurables (data-driven, no hardcoded)

- La bodega **"Pérdidas"** es una fila en `bodega`/`departamento` que el usuario nombra.
- El **tipo/razón de "ajuste de inventario"** es configurable (catálogo o referencia en el documento), igual que la bodega.
- Ningún nombre ni código de bodega va hardcodeado en el API (el `bodega=1` de la vista actual es justamente lo que se elimina).

### D6 — Patrón de API (heredado de US-019)

DTOs de entrada/salida (records + `@Valid`), `@RestControllerAdvice` global (`ApiErrorResponse` uniforme), inyección por constructor, paginación, `@Tag`/`@Operation` (OpenAPI). Read model (vistas) separado del write model (tablas + documentos) — CQRS ligero, que ya existe de facto.

---

## 4. Prerrequisitos

- **Bodega/Departamento "Pérdidas"**: no existe (solo hay 1=Tienda Principal, 2=Bodega 1). Crearla vía **migración Flyway V17+** en el repo Swing para que sea consistente en todos los clientes.
- **(Si se adopta D2 materializado)**: migración V18+ que crea `existencia_articulo_bodega` y modifica los SPs/triggers de kardex para mantenerla, con un backfill inicial desde los saldos tipo 3 actuales.

---

## 5. Arquitectura propuesta del API

```
web/controller/      ProductoCtl (read), BodegaCtl (CRUD), CompraCtl, RequisicionCtl,
                     DevolucionCompraCtl, (Factura/Venta → US-020), AjusteInventarioCtl
domain/dto/          ProductResponse, ExistenciaResponse, CompraRequest, RequisicionRequest, ...
domain/service/      ProductoService, InventarioService (lectura de stock abstraída),
                     CompraService, RequisicionService, BodegaService
domain/repository/   ExistenciaRepository (getExistencia abstraído), CompraRepository, ...
persistence/         entidades escribibles (Articulo real, Bodega, EncabezadoCompra,
                     DetalleCompra, EncabezadoRequisicion, DetalleRequisicion),
                     entidad read-only para stock, mappers
```

**Lectura de stock**: `InventarioService.getExistencia(articulo, bodega)` → `ExistenciaRepository` → (fase 1) función `f_can_saldo_kardex` / (fase 2) tabla materializada. El `ProductResponse` incluye `existencia` poblada por esta vía, por la bodega solicitada.

**Escritura (documentos)**: cada servicio (`CompraService`, `RequisicionService`, ...) inserta encabezado + detalle en una transacción `@Transactional`; el trigger hace el kardex. El API NO toca `movimiento_kardex` directo.

---

## 6. Desglose en historias (epic de inventario)

Dependencias: **INV-0 → INV-1 → {INV-3, INV-4} → {INV-5..INV-9}**. INV-8 se cruza con US-020.

| ID | Historia | Descripción | Depende de |
|---|---|---|---|
| **INV-0** | Bodega "Pérdidas" + catálogos | Migración Flyway V17+: bodega/departamento "Pérdidas" y catálogo de razones de ajuste configurables | — |
| **INV-1** | Lectura de stock abstraída (multi-bodega) | `ExistenciaRepository.getExistencia(art, bodega)`; `ProductResponse` con existencia por bodega; elimina el `bodega=1` hardcoded. Cierra US-018 (lado lectura) | INV-0 |
| **INV-2** | (Perf) Tabla de saldos materializada | `existencia_articulo_bodega` + mantenimiento en SPs/triggers + backfill. Solo si el rendimiento lo pide | INV-1 |
| **INV-3** | CRUD Bodega/Departamento | Entidades escribibles espejo + endpoints CRUD | INV-0 |
| **INV-4** | CRUD maestro de Producto | Entidad escribible sobre tabla `articulo` real → el PUT/POST que US-018 no podía hacer (nombre, precio, categoría, impuesto, estado, tipo) | — |
| **INV-5** | Compras (entrada) | `POST /compras`: encabezado_factura_compra + detalle → trigger sube stock. Flujo diario | INV-1, INV-4 |
| **INV-6** | Requisiciones / Mermas (salida a Pérdidas) | `POST /requisiciones`: encabezado_requisicion + detalle (origen→destino) → trigger mueve stock. Faltante = origen trabajo → Pérdidas | INV-1, INV-3 |
| **INV-7** | Devoluciones de compra (salida) | `POST /devoluciones-compra`: detalle_devoluciones_compra → trigger baja stock | INV-5 |
| **INV-8** | Ventas / Facturación (salida automática) | Crear factura inserta detalle_factura → trigger descuenta stock solo. **Se cruza con US-020** | INV-1 |
| **INV-9** | Ajuste de inventario (documento configurable) ✅ **CERRADO 2026-05-28** | Sobrantes = `POST /purchases` con proveedor ficticio (`es_ajuste=1`); faltantes = `POST /requisitions` a bodega Pérdidas. **Sin código nuevo en API**: solo V25 (Swing) que agrega columna `proveedor.es_ajuste` y siembra los proveedores ficticios. Ver D3 refinado. | INV-0, INV-1, INV-5, INV-6 |
| **INV-CC** | **Fix de concurrencia del kardex** | Serializar movimientos por (artículo, bodega) con lock; corrige un lost-update YA presente en los SPs. Solo BD. Ver §9 | — (independiente) |

---

## 7. Entorno de pruebas

| | `admin_tools` (real) | `PacRoc_backup` (copia migrada) |
|---|---|---|
| Datos reales (35,372 art.) | ✅ | copia |
| SPs/funciones/triggers de kardex | ✅ | ✅ |
| `articulo_view` | ❌ falta | ✅ |
| usuario de prueba | admin / tecnico / ventas | caja1 / 4321 |

Cliente MySQL local: `/usr/local/mysql/bin/mysql` (root / `Jdmm123.`).
La entidad `Articulo` del API requiere `articulo_view`. Para correr el API contra `admin_tools` hay que **crear `articulo_view` ahí** (definida en `V1__baseline.sql`; sus funciones ya existen) o seguir probando contra `PacRoc_backup`. **Decisión pendiente** (ver §8).

---

## 8. Decisiones abiertas

1. **BD de pruebas**: ¿crear `articulo_view` en `admin_tools` y probar ahí con usuario real, o seguir en `PacRoc_backup`?
2. **Modelo de stock fase 1 vs fase 2**: arrancar con función parametrizada (INV-1) y materializar después (INV-2), ¿o ir directo a la tabla materializada?
3. **Nombre/código de la bodega "Pérdidas"** y nombre del documento de "ajuste de inventario".
4. **Bodega por defecto** de cada flujo cuando el cliente no la especifica (hoy ventas asumen bodega 1).
5. **Orden de implementación**: ¿arrancamos por INV-1 (cerrar lectura de US-018) o por INV-6 (faltante→Pérdidas, el caso operativo más común)?
6. **Relación con US-020**: INV-8 (ventas) y la facturación se solapan — definir si se hace una sola vez en US-020.
7. ~~**Política de sobreventa**~~ → **resuelto en §9.3**: preservar el flag existente `config_user_facturacion.facturar_sin_inventario` por usuario, y hacer cumplir el modo "bloqueado" **atómicamente dentro del SP bajo el lock** (SIGNAL si insuficiente).

---

## 9. Concurrencia — race condition existente en el kardex (bug actual) y solución

> Esto NO lo introduce la tabla materializada: **ya está presente hoy** en los SPs `crear_*_kardex`. El fix es **solo en la BD** (SPs + triggers + un índice), vía migración Flyway V17+ en el repo Swing.

### 9.1 El problema

Los SPs `crear_*_kardex` calculan el nuevo saldo con un **read-modify-write sin bloqueo**: leen el último saldo (movimiento tipo 3) con un `SELECT` simple, restan/suman la cantidad, e insertan el nuevo saldo. Dos transacciones concurrentes sobre el mismo (artículo, bodega) leen el **mismo** saldo inicial → una pisa a la otra (lost update).

Ejemplo (stock 167, Caja A vende 3, Caja B vende 5 a la vez):
```
A: lee existencia_old = 167  ┐ ambas leen lo mismo (snapshot, sin lock)
B: lee existencia_old = 167  ┘
A: inserta saldo = 164
B: inserta saldo = 162   ← último saldo = 162 ❌ (debería ser 159)
```
El inventario queda **más alto** que la realidad → faltante. **Escenario real**: las cajas (`caja_1/2/3`) venden de la **misma bodega central (1)** simultáneamente → la carrera ocurre en producción, intermitente y difícil de rastrear.

Dos sub-carreras:
1. **Lost update del saldo** (la principal).
2. **Header duplicado**: dos primeros-movimientos concurrentes del mismo (artículo, bodega) pueden crear dos filas en `articulo_kardex`.

### 9.2 La solución (DB-only)

**(a) Serializar por (artículo, bodega) bloqueando el HEADER `articulo_kardex`** (fila estable, una por artículo+bodega). Debe ser el PRIMER statement del SP:
```sql
SELECT codigo_kardex INTO v_lock_dummy
FROM articulo_kardex
WHERE codigo_kardex = p_cod_kardex
FOR UPDATE;          -- la 2ª transacción espera el commit de la 1ª
```
> Por qué el header y no el saldo: `... ORDER BY DESC LIMIT 1 FOR UPDATE` **bloquea la fila identificada como "última" en ese momento**, no re-evalúa cuando se libera. La segunda sesión leería la fila vieja, no la nueva insertada por la primera. Validado empíricamente — ver §9.4. El header es estable → seguro.

**(b) Leer el saldo TAMBIÉN como locking read** (current read, bypassa snapshot de REPEATABLE READ):
```sql
SELECT mk.cantidad, mk.total, mk.precio_unidad
  INTO existencia_old, total_old, precio_old
FROM articulo_kardex ak
JOIN detalle_movimiento_kardex dmk ON ak.codigo_kardex = dmk.codigo_kardex
JOIN movimiento_kardex mk        ON dmk.codigo_movimiento = mk.codigo_movimiento
WHERE ak.codigo_kardex = p_cod_kardex AND mk.codigo_tipo_movimiento = 3
ORDER BY dmk.codigo_movimiento DESC LIMIT 1
FOR UPDATE;          -- current read: bypassa snapshot establecido por las lecturas previas del trigger
```
> Por qué también con lock: los triggers hacen lecturas planas ANTES del `CALL` (buscar `cod_kardex`, leer `tipo_articulo`). Esas lecturas **establecen el snapshot de REPEATABLE READ** del transaction. Si la lectura del saldo en el SP fuera no-locking, leería del snapshot (datos previos al commit de la sesión A) aunque el header ya esté lockeado. Con FOR UPDATE hace *current read* → ve los datos commiteados más recientes.

**(c) UNIQUE para impedir headers duplicados:**
```sql
ALTER TABLE articulo_kardex ADD UNIQUE KEY uk_articulo_bodega (codigo_articulo, codigo_bodega);
```
+ los triggers usan `INSERT ... ON DUPLICATE KEY` o capturan el duplicado y re-seleccionan.

**(d) (cuando exista la tabla materializada) decremento relativo** `UPDATE existencia_articulo_bodega SET cantidad = cantidad ± delta` — atómico, con lock de fila (defensa en profundidad).

### 9.3 Política de sobreventa — ya es configurable, pero el bug rompe el bloqueo

La política **ya existe** por usuario: `config_user_facturacion.facturar_sin_inventario` (TINYINT, default 0). Hoy el Swing chequea el flag + existencia en Java **antes** del insert (`CtlFacturarFrame.java`, `CtlOrdenVenta.java`) — pero ese chequeo es **no atómico** respecto al kardex → bajo concurrencia dos cajeros bloqueados pueden pasar el chequeo a la vez y ambos vender → sobreventa pese al bloqueo. La regla aplica solo a `tipo_articulo = 1` (bienes); insumos siempre permiten negativos.

**Diseño correcto del fix (atómico, dentro del SP, bajo el lock)** — preserva la config existente:

```sql
-- crear_venta_kardex(p_cod_kardex, p_no_factura, p_cantidad)  (esquema)
-- 1) lectura del saldo con lock (FOR UPDATE)
SELECT mk.cantidad, mk.total, mk.precio_unidad
  INTO existencia_old, total_old, precio_old
FROM articulo_kardex ak
JOIN detalle_movimiento_kardex dmk ON ak.codigo_kardex = dmk.codigo_kardex
JOIN movimiento_kardex mk        ON dmk.codigo_movimiento = mk.codigo_movimiento
WHERE ak.codigo_kardex = p_cod_kardex AND mk.codigo_tipo_movimiento = 3
ORDER BY dmk.codigo_movimiento DESC LIMIT 1
FOR UPDATE;

-- 2) leer la config del usuario de ESTA factura
SET v_permite = COALESCE((
  SELECT cu.facturar_sin_inventario
  FROM encabezado_factura ef
  JOIN config_user_facturacion cu ON cu.usuario = ef.usuario
  WHERE ef.numero_factura = p_no_factura
), 0);     -- default = bloquear

SET newExistencia = existencia_old - p_cantidad;

-- 3) enforcement atómico bajo el lock
IF v_permite = 0 AND newExistencia < 0 THEN
  SIGNAL SQLSTATE '45000'
    SET MESSAGE_TEXT = 'Stock insuficiente y usuario sin permiso para sobrevender';
END IF;

-- 4) insertar movimientos (entrada/salida + saldo nuevo) como ya hace hoy
```

Comportamiento resultante:
- Usuario con `facturar_sin_inventario = 1` → sigue permitiendo negativos (como hoy).
- Usuario con `facturar_sin_inventario = 0` → la venta **aborta atómicamente** si no hay stock; el `SIGNAL` hace rollback del INSERT en `detalle_factura` → la app recibe el error y maneja el "stock insuficiente". Esto **cierra el bug**: bajo concurrencia, solo una de las dos ventas pasa.
- `crear_venta_insumo_kardex` (tipo 2): no aplica el check (consistente con Swing).

> Implicación frontend: las cajas/API deben **manejar el error SQL** (ya no asumir que toda venta enviada se commitea). Es un cambio menor en el manejo de errores del flujo de venta.

### 9.4 Validación — PROTOTIPO PROBADO (2026-05-25)

Prototipado y probado contra `admin_tools` local con kardex de prueba aislado (artículo `319694`, eliminado tras el test). 3 escenarios concurrentes (dos sesiones simultáneas envueltas en `START TRANSACTION ... COMMIT`, simulando el contexto del trigger):

| # | Modo | Cantidades | Saldo inicial | Esperado | Obtenido | Tiempo |
|---|---|---|---|---|---|---|
| A | permisivo (=1) | 3 + 5 | 167 | 159 | **159** ✅ | 4s |
| B | bloqueado (=0), no cabe | 100 + 100 | 167 | 67 + SIGNAL | **67** ✅ + `ERROR 1644 (45000) Stock insuficiente` | 4s |
| C | bloqueado (=0), sí cabe | 30 + 30 | 167 | 107 | **107** ✅ | — |

Tiempo 4s con SLEEP(2) dentro del SP = lock realmente serializa.

**Nota sobre la cobertura del prototipo**: el test usó la versión con FOR UPDATE solo en el header (saldo non-locking) y pasó porque al ser un CALL directo (sin trigger), las lecturas planas previas no ocurrieron y el snapshot se estableció DESPUÉS de la espera del lock. **La versión final que va a la migración debe llevar FOR UPDATE en ambas lecturas** (§9.2.a + §9.2.b) para cubrir el contexto trigger en prod. Esto se validará en la fase de migración con una simulación que incluya las lecturas previas del trigger.

### 9.4.1 Insight crítico: contexto de transacción del caller

El SP solo serializa si **corre dentro de una transacción del caller**. Con `autocommit=1` + `CALL` directo desde un cliente, MySQL **auto-commitea cada statement interno del SP** → el `FOR UPDATE` libera el lock al instante y la serialización se pierde.

- ✅ **En producción funciona naturalmente**: los SPs se invocan desde **triggers `BEFORE INSERT`** sobre las tablas de detalle. Un trigger corre dentro de la transacción implícita del INSERT que lo disparó → el lock del header se mantiene durante todo el trigger+SP.
- ⚠️ **Si el API llama estos SPs vía `CALL` directo** (caso del ajuste manual deprecado, o cualquier llamada futura), DEBE envolver con `START TRANSACTION ... COMMIT` explícito, o usar `@Transactional` en Spring. Sin eso, el lock no sirve.

Esto se debe documentar como invariante para cualquier código (Java o SQL) que invoque los SPs corregidos.

### 9.5 Alcance
Afecta todos los SPs con read-modify-write de saldo: `crear_venta_kardex`, `crear_venta_insumo_kardex`, `crear_compa_kardex`, `crear_dev_venta_kardex`, `crear_dev_compa_kardex`, `crear_inventario_inicial_kardex`, `crear_requisicion_entrada_kardex`, `crear_requisicion_salida_kardex` (+ ajuste, si no se descontinúa antes) y sus triggers. La migración recrea esos objetos.

---

## 10. Referencias

- Flujo y SPs: `V7__recrear_funciones_procedures_triggers.sql` (repo Swing).
- Vista `articulo_view`: `V1__baseline.sql` (repo Swing).
- Memoria del proyecto: `project_inventario_kardex_arquitectura.md`, `project_sprint3_plan.md`.
- Patrón DTO/advice de referencia: US-019 (CustomerCtl, GlobalExceptionHandler) en `main`.
