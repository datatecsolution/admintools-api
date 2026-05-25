# Sprint 3 — Plan ajustado tras exploración del API

**Fecha**: 2026-05-24
**Repo**: `/Users/jdmayorga/Desktop/admintools/` (admin-tools-api, Spring Boot)
**Repo Swing relacionado**: `/Users/jdmayorga/IdeaProjects/adminTools/`
**Contexto**: Sprint 3 del Hito 1. US-016 ya cerrado. API en producción en Ronal (`pedidos.distribuidorasharon.com` → `admin-tools-api-v2`).

---

## ⚠️ LEER PRIMERO — Decisiones pendientes que BLOQUEAN el arranque

Antes de escribir cualquier línea de código del Sprint 3, hay 3 decisiones que el usuario debe tomar. Sin ellas no se puede empezar US-019 (la primera del Sprint).

### Decisión 1 — ¿Qué hacemos con los typos heredados?

El API actual tiene typos profundamente arraigados:

- `costomer` / `Costomer` (debería ser `customer` / `Customer`)
- `despriciouser` (probablemente `descriptionByUser` o similar)
- `getByNome` (debería ser `getByName`)

Están en: Domain POJOs, Mappers MapStruct, Repositories, Services, **paths HTTP públicos** (`/costomers/name/{q}`, `/products/despriciouser/{q}`), y **el frontend React en producción** los consume así (visto en logs de Ronal).

**Opciones**:
- **A** (recomendada): Mantener typos en lo existente. Los Controllers nuevos del Sprint 3 nacen con paths limpios (`/customers`, `/products`). Frontend sigue usando los viejos. Deuda técnica = deprecar los viejos en sprint futuro.
- **B**: Romper y migrar. 1 PR coordinado API + React. Riesgo: si algo se queda atrás, prod rota.
- **C**: Alias de Spring `@RequestMapping({"/customers", "/costomers"})` — ambos paths apuntan al mismo método. Mantiene compat sin duplicar código.

### Decisión 2 — ¿Introducimos DTOs ahora o seguimos con Domain POJOs?

Hoy el API **no usa DTOs**. Controllers devuelven Domain POJOs crudos (`Costomer`, `Product`, `Order`) directamente del MapStruct. No hay `@Valid`, no hay `CustomerCreateRequest`, no hay `CustomerResponse`. La carpeta `/domain/dto/` existe pero está vacía.

**Recomendación de Claude**: introducir DTOs en US-019 (primer Controller nuevo). Es el momento — si seguimos sin DTOs, el API queda atrapado en el patrón actual y los endpoints nuevos heredan las deficiencias.

**Implica**: US-019 establece convenciones (DTOs + `@Valid` + paginación + `@ControllerAdvice` global + `@PreAuthorize` skeleton + `@Operation` OpenAPI). Por eso US-019 son **7 SP reales** (no 5 como dice el backlog).

### Decisión 3 — ¿Cómo arrancamos US-019?

Como US-019 arrastra setup transversal (advice global, patrón DTO, paginación, OpenAPI), puede convenir partirlo:

- **3a** — un commit primero **solo del setup transversal** (advice, base DTO, error response estándar) + un endpoint pequeño (`GET /customers/{id}`). Después seguir con el CRUD completo en commits incrementales.
- **3b** — un solo PR con todo el CRUD + setup. Más grande, más riesgo de regresión.

---

## Plan ajustado del Sprint 3

| Orden | US | Título | SP backlog | SP real | Comentario |
|---|---|---|---|---|---|
| 1 | **US-019** | ClienteController completo | 5 | **7** | Más de lo estimado porque establece patrón |
| 2 | US-018 | ProductoController completo | 8 | **5** | Está 60% hecho; solo completar gaps |
| 3 | US-021 | Usuarios + roles + @PreAuthorize | 5 | **8** | Incluye refactor de seguridad retroactivo |
| 4 | US-020 | FacturaController | 8 | **9** | Depende de US-019, US-018, US-021 |
| 5 | US-022 | Flyway V17+ FKs/ENUMs/typos | 5 | **3** | **NO va en este repo** — va en repo Swing |
| 6 | US-017 | Multi-tenant DataSource | 8 | 8 | **Mover a Sprint 4** — no bloqueante para Hito 1 |

**Total Sprint 3 (sin US-017): 32 SP**

### Por qué este orden

- **US-019 primero (no US-022)**: US-022 NO aplica a este repo (hallazgo de exploración). Las migraciones Flyway viven en `/Users/jdmayorga/IdeaProjects/adminTools/src/main/resources/db/migration/` (repo Swing). El API solo hace `ddl-auto=validate`. Cuando US-022 esté lista, se aplica en Swing y el API la valida en el siguiente arranque.
- **US-019 antes que US-018**: US-019 es 20% hecho vs US-018 60% hecho. Si arrancamos por US-018, vamos a heredar el patrón viejo (sin DTOs). US-019 fresco establece patrón nuevo, después US-018 se completa siguiendo ese patrón.
- **US-021 antes que US-020**: facturación requiere autorización por rol para anular. Necesitamos `@PreAuthorize` funcionando antes.
- **US-017 a Sprint 4**: Multi-tenant es invasivo y los clientes actuales (Urbina, Ronal) son 1 tenant cada uno. No bloquea Hito 1.

---

## Hallazgos clave de la exploración (referencia)

### Hallazgo 1: US-022 no aplica a este repo

El backlog dice "admintools-api: migraciones Flyway V6-V8 FKs/ENUMs/typos" pero el API tiene:
- `spring.jpa.hibernate.ddl-auto=validate`
- Sin Flyway configurado
- Comentario en código: "Las migraciones las maneja Flyway desde el repo adminTools (Swing)"

La nomenclatura "V6-V8" del backlog está desfasada — la realidad es V13/V14/V15/V16 ya aplicadas (créditos, drift fixes). US-022 sería V17+ en el repo Swing.

### Hallazgo 2: Typos están en todo el stack

Tabla de typos detectados:

| Actual | Debería ser | Dónde |
|---|---|---|
| `costomer` / `Costomer` | `customer` / `Customer` | Controller, Service, Repository, Mapper, Entity Domain, frontend React |
| `/costomers` | `/customers` | Path raíz `CostomerCtl` |
| `/products/despriciouser/{q}` | `/products/description/{q}` | Path en `ProductCtl` |
| `getByNome` | `getByName` | Método en `CostomerCtl` (portugués?) |
| `getProduct` en `OrderCtl` | `getOrder` | Método mal nombrado |
| `categoryID` | `categoryId` | Parámetro en `ProductCtl` |

### Hallazgo 3: Arquitectura del API actual

```
Estructura de paquetes:
net.datatecsolution.admintools
├── web/controller/         (7 controllers REST)
├── domain/
│   ├── (POJOs: Costomer, Product, Order, User, ...)
│   ├── dto/               ← VACÍO
│   ├── repository/        (interfaces)
│   └── service/           (8 services)
├── persistence/
│   ├── entity/            (JPA: Cliente, Articulo, Factura, ...)
│   ├── crud/              (JpaRepository)
│   └── mapper/            (MapStruct: 11 mappers)
├── config/
└── security/
```

**Patrón actual**: Controller → Service (con Domain POJOs) → Repository → JpaRepository (con JPA Entity) + Mapper MapStruct entre Entity↔Domain.

**Lo que falta transversalmente**:
- DTOs dedicados (carpeta vacía)
- `@Valid` + `jakarta.validation` en requests
- Paginación con `Pageable`
- `@PreAuthorize` o checks por rol
- `@ControllerAdvice` global (cada controller maneja sus excepciones)
- `@Operation` / `@ApiResponse` (Swagger UI funciona pero sin documentación de endpoints)

### Hallazgo 4: Estado de cada Controller

#### CostomerCtl (`/costomers`) — 20% completo
- ✅ `GET /name/{name}` — usado por React
- ❌ GET paginado, GET by ID, POST, PUT, DELETE, búsqueda por RTN

#### ProductCtl (`/products`) — 60% completo
- ✅ `GET /{productId}`
- ✅ `GET /description/{description}`
- ✅ `GET /category/{categoryId}`
- ✅ `GET /despriciouser/{description}` ⚠️ typo, mismo método que `/description`
- ✅ `POST /save`
- ✅ `DELETE /delete/{id}`
- ❌ `PUT /products/{id}` update, GET paginado, DTOs, validación stock

#### OrderCtl (`/orders`) — 40% completo
- ✅ `GET /{orderId}` (método mal nombrado `getProduct`)
- ✅ `GET /today` (órdenes del día del usuario autenticado)
- ✅ `POST /save` (validación de vendedor, autenticación)
- ✅ `DELETE /delete/{id}` (valida propiedad)
- ❌ GET paginado todas, GET detalle líneas, PUT update parcial, búsqueda por rango fechas

#### UserCtl (`/users`) — 5% completo
- Existe pero **sin endpoints**. Todos los métodos comentados.
- Para US-021 = casi desde cero.

#### AuthCtl (`/auth`) — 100% completo
- ✅ `POST /login`
- ✅ `POST /refresh`
- JWT funcional, stateless, integrado con Spring Security.

#### PriceCtl (`/price`) — funcional básico
- ✅ `GET /all`, `POST /save`

#### PricesProducCtl (`/prices`) — solo lectura
- ✅ `GET /all`

### Hallazgo 5: SecurityConfig

- ✅ JWT + stateless
- ✅ CORS vía env `CORS_ALLOWED_ORIGINS`
- ✅ CSRF deshabilitado
- ✅ Rutas públicas: `/auth/**`, `/v3/api-docs/**`, `/swagger-ui/**`
- ❌ **SIN `@PreAuthorize`** en ningún controller — solo autenticación, no autorización por rol
- ❌ **SIN `@ControllerAdvice` global**

---

## Próximos pasos (orden exacto para próxima sesión)

1. **Mostrar este documento al usuario** (no asumir que se acuerda del contexto).
2. **Cobrar las 3 decisiones pendientes** (typos, DTOs, arranque US-019).
3. Una vez decididas las 3:
   - Si **Decisión 1 = A o C** y **Decisión 2 = DTOs sí** y **Decisión 3 = 3a**:
     - Crear rama `feature/sprint-3-us-019-cliente-controller`
     - Commit 1: setup transversal (`@ControllerAdvice`, `ApiErrorResponse` estándar, base `CustomerCreateRequest`/`CustomerResponse` en `/domain/dto/`, primer endpoint `GET /customers/{id}`)
     - Commit 2-N: CRUD completo + paginación + búsqueda + tests
   - Si otras combinaciones: ajustar approach según elecciones.
4. **Tener cuidado con**:
   - No tocar `CostomerCtl`/`/costomers` mientras el React de Ronal lo siga llamando.
   - Verificar que el patrón nuevo (DTO + advice + paginación) compila y arranca contra MySQL local antes de PR.
5. **Lo que NO se hace en Sprint 3**:
   - Arreglar typos en endpoints existentes (deuda técnica futura).
   - US-017 Multi-tenant (movido a Sprint 4).
   - US-022 acá (va al repo Swing como V17+).

---

## Estado del repo en este momento

- Branch: `main`
- Último commit: `39b795d` "Bajar logging de DEBUG a INFO en perfil pdn"
- API en prod (Ronal): corriendo el commit `39b795d` desplegado el 2026-05-24
- Frontend en prod: `at-ordenes-ventas-v2` desplegado 2026-05-23, estable

## Memoria relacionada

Ver `~/.claude/projects/-Users-jdmayorga-IdeaProjects-adminTools/memory/`:
- `project_deploy_pdn_paralelo.md` — contexto del deploy actual en Ronal
- `project_schema_drift_validation.md` — V14/V15/V16 aplicadas
- `project_admintools_api_phase0.md` — phase 0 del API (login, swagger)
