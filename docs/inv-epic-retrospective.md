# Retrospectiva del epic INV — 2026-05-25 / 2026-05-28

> Documento escrito al cerrar el epic. Sirve para entender por qué el código
> de inventario quedó como quedó y qué evitar al evolucionarlo.

## 1. Qué se construyó

Toda la operación de inventario del negocio expuesta por API REST, sin que el
API toque kardex en ningún lado. Cada operación inserta documentos en BD; los
triggers existentes (o reescritos) hacen el inventario solos y de forma
serializable.

```
INV-1  /inventory/stock                  Lectura multi-bodega
INV-3  /warehouses CRUD                  Bodegas (mirror a departamento)
INV-4  /products CRUD                    Catálogo de productos
INV-5  /purchases POST/GET               Compras a proveedor
INV-6  /requisitions POST/GET            Transferencias entre bodegas (mermas)
INV-7  /purchase-returns POST/GET        Devoluciones a proveedor
INV-8  /invoices/from-order/{id}         Facturación definitiva
INV-9  (sin endpoint propio)             Ajustes via compra a proveedor ficticio
       /sale-returns POST/GET            Devoluciones del cliente
INV-CC (sin endpoint)                    Fix de concurrencia en los SPs
```

## 2. Métricas

| Tema | Valor |
|---|---|
| Duración del epic | 4 días (2026-05-25 → 2026-05-28) |
| Historias del epic completadas | 10 (INV-1, 3, 4, 5, 6, 7, 8, 9, Sale Returns, INV-CC) |
| Migraciones Flyway nuevas (Swing common) | 11 (V17–V27) |
| Migraciones Flyway nuevas (Swing caja) | 1 (V8) |
| Commits API en `main` (incluye US-016/17/18/19/20/21/22) | 28 |
| Commits Swing en `master` | 12 |
| Líneas en `*Service.java` del epic | 1 183 |
| Líneas en `*Ctl.java` del epic | 590 |
| Bases de datos tocadas | `admin_tools` + N cajas (`admin_tools_caja_*`) |
| Stored procedures reescritos | 7 (concurrencia V19/V20) |
| Triggers nuevos o curados | 1 (V8 caja) |
| Endpoints REST nuevos | 24 entre POST y GET |

## 3. El arco del epic — cronología corta

1. **Exploración** (2026-05-25). Mapeamos la BD desde cero: tipos de movimiento,
   triggers existentes, qué tabla detona qué SP. Hallazgo: el inventario en
   producción **no se descontaba automáticamente desde las cajas viejas** —
   les faltaba el trigger del baseline porque se crearon antes de Flyway.
   Bomba de tiempo descubierta de casualidad.

2. **Diseño** (`docs/inventario-api-design.md`, mismo día). Acordamos las
   reglas: "API escribe documentos, no llama SPs directo"; "ajustes son
   documentos auditables"; "multi-bodega desde el modelo"; "concurrencia se
   arregla con header lock + FOR UPDATE". El diseño aguantó la implementación
   sin cambios estructurales.

3. **Foundation** (V17–V20, INV-1/3/4). Bodega "Pérdidas", saldo materializado
   (`existencia_articulo_bodega`), fix de concurrencia, lectura abstraída.

4. **Flujos de negocio** (INV-5/6/7). Compras, requisiciones, devoluciones de
   compra. Patrón estable: entity → CRUD → DTO → Service → Controller; ~120-150
   líneas por service.

5. **Multi-tenant** (US-017). Necesario para INV-8 porque las facturas viven
   per-caja, no en admin_tools. `AbstractRoutingDataSource` + `ThreadLocal` +
   `HandlerInterceptor` desde el JWT.

6. **Rename Factura → Orden** (refactor). Antes de meter INV-8 detectamos que
   el entity llamado `Factura` apuntaba a `encabezado_factura_temp` (que son
   órdenes, no facturas). Liberar el nombre era pre-requisito para que las
   entidades definitivas de la caja no chocaran.

7. **INV-8** (facturación definitiva). Segundo árbol de persistencia JPA con
   `tenantEntityManagerFactory` paralelo al common. Validación cross-tenant.
   UPDATE optimista de la orden para cerrar la ventana de doble facturación
   sin un FK nuevo.

8. **US-021** (RBAC). Jerarquía ADMIN > INVENTORY > CASHIER > SELLER > USER
   derivada de `usuario.tipo_permiso` del Swing — fuente única, deploy
   zero-friction.

9. **US-022** (cleanup typos + cutover frontend). Costomer → Customer en API y
   en orders app.

10. **INV-9** — el momento del epic donde una decisión bien tomada redujo
    el alcance de "2 horas de código + 3 tablas + 1 trigger + fix V25" a
    "una migración seed de 60 líneas". La idea fue del usuario: si los faltantes
    son "requisición a bodega ficticia (Pérdidas)", los sobrantes deben ser
    "compra a proveedor ficticio". Simetría perfecta.

11. **Sale Returns** (2026-05-28). Cierra el epic. Mismo patrón que INV-7 pero
    con validación cross-tenant porque las facturas viven en cajas distintas.

## 4. Decisiones arquitectónicas clave

### D1 — API no toca kardex (todas las US del epic)

Cada operación de inventario tiene una tabla de detalle con un trigger
`b_insert` que dispara el SP correcto. El API solo inserta filas en esas
tablas. Todo `crear_*_kardex` se ejecuta dentro de la transacción del
INSERT — atómico, sin código nuevo de coherencia kardex/balance.

**Impacto**: cualquier escritura futura (otros canales, jobs batch, mantenimiento
manual) que pase por la misma tabla obtiene el mismo comportamiento. La regla
de inventario vive en la BD, no en los servicios.

### D2 — Saldo materializado + función fallback

`existencia_articulo_bodega` (V18) es la tabla de saldos. Los 7 SPs hacen
UPSERT en la misma transacción del kardex. El API lee de ahí directo. La
función `f_can_saldo_kardex` siguió viva como red de seguridad / herramienta
de reconciliación, pero ya no es la ruta de lectura.

### D3 — Ajustes son documentos auditables (refinado en INV-9)

Diseñado como endpoint propio `/inventory-adjustments` con tabla de motivos y
trigger nuevo. **Refinado en INV-9**: sobrantes son compras a proveedor ficticio
(`proveedor.es_ajuste=1`), faltantes son requisiciones a bodega "Pérdidas"
(INV-6). Simetría completa, cero código nuevo.

### D4 — Multi-bodega desde el modelo

`ExistenciaRepository.getExistencia(art, bodega)` recibe la bodega como
parámetro desde el día uno. El histórico `bodega=1` hardcoded del Swing
quedó cerrado. La app de pedidos vieja sigue funcionando con bodega 1; la
app nueva puede ramificarse por bodega del usuario.

### D5 — Multi-tenant solo para facturas y devoluciones de venta

`commonDataSource` (admin_tools, `@Primary`) cubre el 90% del API. El segundo
árbol (`tenantRoutingDataSource` + `tenantEntityManagerFactory`) solo lo usan
las entidades de `persistence/tenant/entity` (EncabezadoFactura, DetalleFactura).
El routing es transparente: el `TenantInterceptor` pobla el `TenantContext`
desde el JWT y `AbstractRoutingDataSource` resuelve solo.

### D6 — Concurrencia se arregla con lock de header

El patrón validado (`SELECT FOR UPDATE` sobre `articulo_kardex` por
`(articulo, bodega)` + `SELECT FOR UPDATE` sobre el saldo) serializa
movimientos del mismo artículo sin bloquear los demás. Lo crítico: ese lock
SOLO funciona si el SP corre dentro de una transacción del caller — los
triggers lo proveen automáticamente; cualquier `CALL` directo desde Java
debe envolverse en `@Transactional`.

### D7 — Catálogos data-driven, no hardcoded

Bodegas, proveedores ficticios (`es_ajuste`), tipo de impuesto, motivos
(implícito en INV-9 vía nombre del proveedor). Nada de IDs mágicos en el
código del API.

## 5. Lo que salió bien

- **El diseño aguantó la implementación.** Cero refactors estructurales después
  del documento inicial. Eso es signo de que la exploración fue suficiente.

- **Patrón "API no toca kardex" pagó dividendos.** Cada nueva operación se
  reduce a "entity + CRUD + DTO + Service + Controller" y el inventario sale
  solo. Las 10 historias del epic comparten el mismo esqueleto.

- **Migraciones idempotentes con helper SP.** El patrón `vN_alter_si_tipo_difiere`
  (V21) se reusó en V22/V23/V24/V26 sin un solo bug en deploy.

- **El refinamiento de INV-9 vía proveedor ficticio.** Ahorrar 2 horas de
  trabajo cuando el plan estaba claro y aprobado, mediante una pregunta del
  usuario ("¿no podría modelar el sobrante como compra?"), fue la mejor
  decisión arquitectónica del epic. Lección: pausar antes de codear lo
  diseñado y preguntar si el modelo puede simplificarse.

- **`tipo_permiso` como fuente única de roles.** El Swing ya tenía el campo y
  el negocio ya pensaba en términos de root/supervisor/cajero/vendedor. No
  necesitamos catálogo nuevo, no necesitamos seeding en producción, no
  necesitamos coordinar nada. Solo `CustomUserDetailsService` mapea
  tipo_permiso → ROLE_*.

- **Validación E2E con curl + asserts contra BD.** Cada US cerró con un script
  que ejecuta el flujo POST y verifica las invariantes de stock/kardex/balance
  contra mysql. Probablemente más útil que tests unitarios para este nivel de
  acoplamiento con BD.

## 6. Lo que dolió y cómo se resolvió

### 6.1 Race condition en el kardex (descubierto por exploración)

Los 7 SPs `crear_*_kardex` hacían read-modify-write del saldo sin lock.
Empíricamente reproducible: dos ventas concurrentes del mismo artículo →
saldo queda más alto que el real → faltante. **Resolución**: V19 (solo
`crear_venta_kardex` con SIGNAL para sobreventa según
`config_user_facturacion`) + V20 (el resto). Patrón header lock + FOR UPDATE
saldo + UPSERT.

**Trampa importante**: el lock solo funciona dentro de una transacción del
caller (`autocommit=1 + CALL directo` no sirve). Esta invariante quedó
documentada en `docs/inventario-api-design.md §9.4.1`.

### 6.2 Trigger faltante en cajas viejas (V8)

`admin_tools_caja_1` local no tenía `detalle_factura_b_insert` — descubierto
al planear INV-8. Se importó de un dump anterior a que las migraciones de
caja existieran. **Resolución**: V8 caja idempotente, sin placeholders Flyway
(resuelve la bodega en runtime via `admin_tools.cajas WHERE nombre_db =
DATABASE()`). Aplicable a clientes viejos y nuevos por igual.

### 6.3 Drifts de tipo entre BD viejas y nuevas (V21–V24, V26)

Floats que el Swing nunca tocó, columnas `varchar(9)` donde la entidad
esperaba `varchar(255)`, `bit(1)` vs `int`. Aparecen al apuntar el API con
`ddl-auto=validate`. **Resolución**: helper `v21_alter_si_tipo_difiere`
reusado en cada V*. Sin pérdida de datos.

### 6.4 Spring Security 6 — beans `static` para method security

`@EnableMethodSecurity` con `hasRole('CASHIER')` no respetaba la jerarquía
hasta que `RoleHierarchy` y `MethodSecurityExpressionHandler` se declararon
`static`. Sin eso, los `AuthorizationManager` ya estaban inicializados
cuando llegaba el bean y la jerarquía no se aplicaba. **Trampa silenciosa**
porque no hay error: simplemente `hasRole` queda match literal.

### 6.5 `@ConfigurationProperties` no traduce `url` → `jdbcUrl`

En `MultiTenantConfig` la primera versión usaba
`@ConfigurationProperties(prefix="spring.datasource")` sobre un
`HikariDataSource`. Funcionaba en el inferred bean de Spring Boot pero NO
funcionaba para beans declarados a mano (Hikari requiere `jdbcUrl`,
no `url`). **Resolución**: `@Value("${spring.datasource.url}")` explícito y
construcción manual del DataSource.

### 6.6 `@Valid` corre antes de `@PreAuthorize`

Probar RBAC con body vacío en endpoints `@Valid @RequestBody` no funciona
— el body falla validación primero (400) sin importar el rol. **Workaround**:
probar el comportamiento de RBAC en endpoints sin `@Valid` o con body
mínimo válido. Lo dejamos documentado en el commit de US-021.

### 6.7 Rename `Factura → Orden` requerido para INV-8

El entity `Factura.java` apuntaba a `encabezado_factura_temp` (órdenes). Si
se introducía la `EncabezadoFactura` definitiva de INV-8 sin renombrar, el
código tendría dos entidades con nombres confundidos. **Resolución**:
rename quirúrgico (clase + relación interna `factura → orden`); atributos
internos `idFactura, tipoFactura` mantuvieron nombre por coherencia con
columnas BD reales (`numero_factura`, `tipo_factura`).

### 6.8 INV-CC sin cubrir todos los SPs

V19/V20 actualizó 7 SPs pero dejó `crear_ajuste_inventario_kardex` sin el
fix. Hubiera requerido V25 nueva si INV-9 hubiera ido por la ruta original.
**Resolución**: INV-9 se refinó al patrón "compra ficticia", que usa
`crear_compa_kardex` (ya fixed). El SP histórico de ajuste queda
**descontinuado** — no se le aplica el fix porque ya no es ruta de nuevo
desarrollo.

### 6.9 V19 default invertido + query JOIN cross-schema (descubierto post-deploy)

Después del deploy de V14–V26 a Cliente A (192.168.1.23), el Swing empezó
a tirar `SIGNAL '45000' Stock insuficiente; usuario bloqueado para
sobrevender (V19)` para cada venta con stock <= cantidad. El
comportamiento histórico del Swing era permitir sobreventa.

**Causa raíz doble:**

1. La query del SP V19 hacía
   `JOIN encabezado_factura ef ON cu.usuario = ef.usuario WHERE ef.numero_factura = p_no_factura`.
   El SP vive en `admin_tools`; sin prefijo de schema, `encabezado_factura`
   resuelve a `admin_tools.encabezado_factura`. Pero todo el flujo real
   (Swing y INV-8) escribe facturas en `admin_tools_caja_N.encabezado_factura`
   (per-caja). `admin_tools.encabezado_factura` está vacía.

2. El JOIN nunca encuentra el row → `COALESCE(NULL, 0) = 0` → `v_permite=0`.
   Combinado con `newExistencia < 0` → SIGNAL.

**Lección:** la validación V19 (sobreventa por usuario) se diseñó asumiendo
que la factura estaba en la misma BD que el SP. Funciona en aislamiento
pero NUNCA pudo aplicarse en el flujo real cross-schema. Eso convirtió un
"default conservador" en una **regresión silenciosa** que sólo apareció
con stock bajo.

**Resolución V27**: cambiar el default a 1 (permitir). Sin cambio de
firma, sin tocar triggers, cero blast radius. La validación V19 queda
**pasiva** para el flujo real (cross-schema desde cajas). Si en el futuro
se necesita validación real, hay que pasar el `usuario` como parámetro al
SP (V28 + V9 caja: trigger resuelve usuario del encabezado local y lo
pasa). Diseño documentado pero no implementado.

**Patrón a evitar**: cualquier SP en `admin_tools` que asuma encontrar
datos en `admin_tools` cuando puede ser llamado desde triggers de cajas.
Si el dato vive cross-schema, hay que pasarlo como parámetro, no hacer
JOIN al vacío.

**Hallazgo secundario al investigar el reporte**: `FacturaDao.registrar`
del Swing **no es transaccional** — cada INSERT (encabezado + N detalles
+ cuenta_factura) usa una conexión distinta del pool con `autoCommit=true`.
Cualquier falla intermedia produce factura cobrada sin líneas (huérfana).
V27 desactivó el único punto de falla conocido (SIGNAL de V19) y el bug
estructural NO se manifiesta hoy, pero queda latente para cualquier otra
causa de error. Refactor diferido voluntariamente para no atrasar el
proyecto principal de US. Plan completo documentado en repo Swing:
`docs/deferred-facturadao-transaccional.md` con pasos, estimación
(~2h+buffer), gatillos para activarlo (otro huérfano en prod, reactivación
de V19/V28, cliente con auditoría estricta) y mitigaciones.

### 6.10 Frontend orders app cableado a Docker para el proxy

`package.json` traía `"proxy": "http://host.docker.internal:8082"` para el
deploy productivo. Para correrlo en mi laptop contra el API local, tuve que
cambiarlo a `http://localhost:8080` (temporal, no commiteado). **Mejora
futura**: usar `setupProxy.js` con env var `API_URL`. No hecho — bajo
impacto, alta visibilidad cuando se necesita.

## 7. Patrones reusables establecidos

| Patrón | Donde se usa hoy | Plantilla |
|---|---|---|
| Endpoint CRUD con DTOs | INV-3, INV-4, INV-5, INV-6, INV-7, INV-8, Sale Returns | `WarehouseCtl` + `WarehouseService` |
| Inserción de documento → trigger kardex | Todas las operaciones de stock | `PurchaseService.create` |
| Service cross-tenant (common + tenant transactions) | INV-8, Sale Returns | `SaleReturnService` |
| Helper SP idempotente para drift fix | V21, V22, V23, V24, V26 | `v21_alter_si_tipo_difiere` |
| Trigger cross-schema (caja → admin_tools) | V8 caja | `detalle_factura_b_insert` |
| Validación de cantidad disponible | Sale Returns | `SaleReturnService.create paso 2` |
| RBAC derivado de columna del Swing | US-021 | `CustomUserDetailsService.mapTipoPermiso` |
| Compensación si falla insert tenant | INV-8 | `InvoiceService.create paso 2` |
| Validación E2E con curl + asserts BD | Todas las US del epic | Scripts inline en los commits |

## 8. Deuda técnica aceptada

| Item | Riesgo | Mitigación |
|---|---|---|
| `crear_ajuste_inventario_kardex` sin fix V19/V20 | Lost update si dos ajustes manuales concurrentes del mismo artículo | El SP queda descontinuado; no es ruta del API. El Swing legacy lo sigue llamando — su uso es esporádico y de bajo volumen. |
| Frontend `package.json` con proxy hardcoded a Docker | Confusión al levantar dev local | Documentado en este doc; mejora a `setupProxy.js` queda pendiente |
| `application-dev.properties` gitignored | Setup local no reproducible vía repo | Los valores están documentados en commits anteriores y son comunes para todos los devs |
| Sin tests unitarios JUnit | Bug regresivo no detectado en CI | Validación E2E con curl + asserts BD cubre el happy path y 5-6 escenarios de error por endpoint |
| Cajas con `agrega_kardex=0` históricas (110 en mi laptop) | Stock sistémico desfasado de stock real en cajas antiguas | Script de reconciliación opcional: re-procesar filas con `agrega_kardex=0` post-deploy de V8 |
| El SP `agregar_salida_fact_kardex` no está integrado | Era el "catch-up" histórico — ya no se usa | Queda como referencia para reconciliación manual si se necesita |

## 9. Recomendaciones para futuras evoluciones

### 9.1 Nuevos canales de inventario

Cualquier integración nueva (POS mobile, importador masivo, API de socios)
debe respetar D1: insertar en la tabla de detalle correcta y dejar que el
trigger haga el kardex. **No llamar SPs directo**. El día que alguien lo
haga, hereda el bug de concurrencia o de coherencia kardex/balance.

### 9.2 UI nueva del POS

Cuando se construya, el selector de proveedores de "Compra normal" debe
filtrar `WHERE es_ajuste = 0`; el de "Ajuste de inventario" filtra
`WHERE es_ajuste = 1`. Sin esto, el cajero verá "Donación recibida" mezclado
con proveedores reales en el flujo de compra normal.

### 9.3 Reembolso al cliente / nota de crédito

`Sale Returns` solo maneja kardex. Si el negocio quiere emitir nota de
crédito o registrar reembolso, es US aparte. Diseño sugerido: tabla
`encabezado_nota_credito` referenciando `numero_factura`, con
`encabezado_factura.saldo` o tabla `cuentas_por_cobrar`. **No mezclar con
sale-returns** — son conceptos distintos (mercadería vs. dinero).

### 9.4 Anulación de factura

Hoy no existe. Si se necesita, el patrón propuesto: cambiar
`estado_factura = 'NULA'` + insertar líneas en `detalle_devoluciones` por
cada línea original (reversa automática del kardex). RBAC: ADMIN.

### 9.5 Reportes ejecutivos sobre kardex

`GET /reports/movements?from=&to=&warehouse=&product=` con joins entre
`movimiento_kardex`, `detalle_movimiento_kardex`, `articulo_kardex`. El
modelo de datos lo soporta hoy; falta el endpoint.

### 9.6 Aprobación multi-paso de ajustes / devoluciones

Si el negocio quiere "el cajero solicita devolución, el supervisor aprueba",
hay que cambiar el modelo: nueva columna `estado` en `detalle_devoluciones` +
endpoint `POST /sale-returns/{id}/approve`. **No es trivial** porque el
trigger del kardex dispara al insertar, no al aprobar. Habría que mover
el INSERT a "después de aprobado" — implica refactor del Service.

### 9.7 Producción al primer cliente piloto

Ver `~/.claude/projects/.../memory/project_inventario_kardex_arquitectura.md`
para el TODO de deploy. Los puntos críticos:

1. Cutover del frontend orders app (cambios de US-022 ya pusheados).
2. Verificar `tipo_permiso` por usuario en el cliente — el RBAC usa eso.
3. Aplicar V8 caja a las BDs `admin_tools_caja_N` reales del cliente (lo hace
   Flyway al arrancar Swing). Aplicar V26 también (drift de detalle_devoluciones).
4. Smoke en piloto: POST /invoices/from-order, /sale-returns, /purchases,
   /requisitions; verificar el cliente puede facturar y los movimientos
   aparecen en kardex.
5. Mantener Swing y API corriendo en paralelo (cutover gradual).

## 10. Referencias

- **Diseño**: `docs/inventario-api-design.md`
- **Sprint plan**: `docs/sprint-3-plan.md`
- **Memoria del proyecto**: `~/.claude/projects/-Users-jdmayorga-IdeaProjects-adminTools/memory/project_inventario_kardex_arquitectura.md`
- **Repo API**: `https://github.com/datatecsolution/admintools-api` — branch `main`
- **Repo Swing**: `https://github.com/datatecsolution/adminTools` — branch `master`
- **Repo Frontend**: `https://github.com/datatecsolution/at-ordenes-ventas` — branch `main`

Commits clave (todos en `main` / `master`):

```
API
  442a622  Merge US-018 (INV-1/3/4) + INV-5/6/7 + docs diseño
  5992fff  Merge US-017 multi-tenant
  e4f6dfb  Merge rename Factura → Orden
  7b0d185  Merge INV-8 / US-020 facturación definitiva
  6ccb17f  Merge US-021 RBAC con tipo_permiso
  e3e8b9f  Merge US-022 cleanup typos
  6b96346  Doc INV-9 refinado (D3)
  8a76431  Merge Sale Returns — cierra epic

Swing
  45c2653  Merge V17-V24 (inventario + concurrencia + drift fixes)
  962c9fc  Merge V8 caja (trigger detalle_factura_b_insert)
  33c42c4  Merge V25 (INV-9 proveedores ficticios)
  43bb955  Merge V26 (drift fix detalle_devoluciones)
  (next)   V27 (fix default facturar_sin_inventario — bug post-deploy)
```

---

Documento cerrado 2026-05-28. Si lo estás leyendo y algo de lo que dice ya no
es cierto, actualizalo: este doc es la memoria institucional del epic, no un
artefacto histórico inmutable.
