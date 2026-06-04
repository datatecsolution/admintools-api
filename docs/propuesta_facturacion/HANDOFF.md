# HANDOFF — Módulo de Facturación Táctil → `admintools-pos`

Guía para integrar el prototipo de **facturación táctil** (este proyecto) dentro
del frontend real `admintools-pos` (Vite + React 19 + TS + Tailwind v4 + React
Router 7), conectado a `admintools-api` (Spring Boot, multi-tenant por caja vía JWT).

> El prototipo es la **referencia de diseño y comportamiento**. NO se copia
> tal cual: se reimplementa en `.tsx` usando los componentes y tokens reales
> del repo, y se conecta a los endpoints reales en vez de los datos mock.

---

## 1. Archivos de referencia (este proyecto)
- `pos-app.jsx` — toda la lógica/UI (React + Babel en navegador). Léelo como spec.
- `pos.css` — tokens y estilos (replican slate + verde, claro/oscuro del repo).
- `pos-data.js` — **datos mock** (a reemplazar por llamadas al API).
- `pos-icons.jsx` — iconos (Heroicons outline) y `money()` (formato Lempiras).
- `Facturación Táctil.html` — ensambla todo.
- `src/...` (en este proyecto) — copias de los `.java` reales del API usados para
  mapear contratos (controllers + DTOs + domain de Order/OrderDetails).

## 2. Dónde vive en `admintools-pos`
- Ruta nueva sugerida: **`/facturacion`** (pantalla full-screen, sin el sidebar
  normal — es una superficie de caja táctil). Añadir en `src/App.tsx`.
- Feature: `src/features/facturacion/` (api.ts, schemas.ts, hooks).
- Componentes de pantalla: `src/pages/FacturacionPage.tsx` + subcomponentes.
- Reutilizar el cliente HTTP existente: **`@/lib/api`** (mismo patrón que
  `features/products/api.ts` y `features/cajas/api.ts`).

## 3. Sistema de diseño a reutilizar (NO recrear)
- Tokens: slate neutros + **verde primario** (green-600 claro / green-500 oscuro),
  `rounded-md`, system-ui, claro/oscuro por clase `dark` (ya en `src/index.css`).
- Componentes UI existentes: `Button`, `Input`, `Dialog`, `Table`, `PageHeader`,
  `Spinner`, `ThemeToggle`. Los modales del prototipo → usar `Dialog`.
- Iconos: Heroicons (ya en uso). Moneda: HNL, locale `es-HN`.

---

## 4. Modelo de flujo (CORREGIDO con el negocio real)

> Importante: el flujo `Order → /invoices/from-order` que expone `InvoiceCtl`
> es de **otra app** (órdenes de campo). En facturación táctil **la factura se
> cobra DIRECTO**. La única acción que crea una orden aquí es **Guardar**.

| Acción (prototipo) | Qué hace | Backend |
|---|---|---|
| **Cobrar** | Crea la **factura directa** (cobro inmediato) | ⚠️ **Endpoint a confirmar** — `InvoiceCtl` solo tiene `POST /invoices/from-order/{orderId}`. Falta/confirmar el de factura directa. |
| **Guardar** | Persiste el ticket como **Order** recuperable | `POST /orders/save` (rol SELLER) → tabla de órdenes |
| **Lista de órdenes** | Recupera una orden guardada | `GET /orders/today` (o listado de órdenes activas) + `GET /orders/{id}?user=` |
| **Crear cotización** | Persiste como **cotización** (tabla distinta) | ⚠️ Endpoint de cotizaciones — confirmar nombre |
| **Lista de cotizaciones** | Recupera una cotización | ⚠️ Endpoint de cotizaciones — confirmar |

## 5. Pantalla → endpoint → DTO

### Catálogo de productos (izquierda)
- Buscar por nombre: `GET /products/description/{description}` (trae precio del usuario).
- Por categoría: `GET /products/category/{categoryId}`. Categorías: `GET /categories`.
- Código de barras: el `Product` trae `altCode` → buscar por código exacto.
- DTO: `ProductResponse { id, name, price, categoryId, taxId, altCode, type, active }`.
- **Imagen de producto**: en el prototipo es mock (`images/cat-N.png`). En real
  vendría del producto (URL de imagen) — confirmar si el master la expone; si no,
  es un gap (campo nuevo) o se queda con monograma (tweak `Miniatura`).

### Ticket / líneas
El POS construye un **Order** + N **OrderDetails** (mismo shape que `Guardar` y que
el cobro directo deben enviar):

```
Order {
  customerId, sellerId, user, obser (observaciones),
  subTotalExcento, subTotal15, subTotal18, subTotal,
  totalTax (ISV 15%), totalTaxs18 / isvOther (ISV 18%), total,
  discount, active, details: OrderDetails[]
}
OrderDetails {
  productId, amount (cantidad), priceItem, priceItemId (tipo de precio),
  discountItem / discount, subTotal, tax, total
}
```

### Impuestos (ISV 15% y 18%) — YA alineado
- `subTotal15` / `totalTax` = base y ISV 15%.
- `subTotal18` / `totalTaxs18` (o `isvOther`) = base y ISV 18%.
- `subTotalExcento` = exento.
- Catálogo de impuestos: `GET /taxes` → `TaxResponse { id, description, percent }`.
- El producto trae `taxId` → resolver % desde el catálogo (no hardcodear 0.15/0.18).

### Cliente
- Buscar: `GET /customers?name=&page=&size=` → `Page<CustomerResponse>`.
- Por id: `GET /customers/{id}`.
- ⚠️ Crear: `POST /customers` requiere **rol ADMIN**. En el prototipo el cajero
  crea libremente — reconciliar: ocultar "Nuevo cliente" si no es ADMIN, o
  habilitar un flujo/permiso. (`CustomerCreateRequest`: name, rtn, address, phone…)

### Vendedor (paso 1 del asistente de cobro)
- Vendedores = usuarios con rol SELLER. Confirmar endpoint de listado de
  vendedores/sellers (no apareció uno dedicado; revisar `UserCtl`/`Seller`).
- `Seller { id, name, lastName, email, phone, address }`; el código de empleado
  sale de `User.codigoEmpleado`.

### Tipos de precio ("Precios")
- `GET /price-catalog` → `PriceCatalogResponse { id, name }`
  (1 Público General, 2 Clientes Especiales, 3 Mayoristas, 4 Costos).
- Cambiar tipo afecta `OrderDetails.priceItemId` por línea.

### Caja / sesión
- `GET /cajas` → `CajaResponse`. La caja activa se resuelve por el **JWT**
  (TenantContext) — no se manda en el body.

### Config "pedir vendedor / pedir observaciones"
- ⚠️ **No hay endpoint de configuración** en el API revisado. El prototipo lo lee
  de `window.POS_CONFIG.facturacion`. Opciones:
  (a) crear `GET /config/facturacion` en el backend, o
  (b) resolverlo como regla de negocio/constante por cliente.

---

## 6. Gaps de backend (no existen endpoints aún)
Estas funciones del prototipo son **placeholders**; requieren trabajo de API antes
de funcionalizar:
- **Cobro directo de factura** (confirmar/crear endpoint).
- **Crear/Listar cotizaciones** (confirmar endpoints + tabla).
- **Cierre de caja** (arqueo/corte).
- **Pago cliente** (abono a cuenta por cobrar).
- **Pago proveedores** (egreso).
- **Salida de efectivo** (retiro/gasto).
- **Config** pedir vendedor/observaciones.

## 7. Comportamientos a preservar (del prototipo)
- Búsqueda limpia el campo al agregar al ticket; Enter agrega match de código.
- Último producto agregado queda **seleccionado**; **Cantidad** abre teclado
  numérico sobre esa línea (cantidades grandes).
- **Cobrar** = asistente guiado de pasos dinámicos según config
  (Vendedor → Observaciones → Pago); pasos se omiten si la config los apaga.
- Pago efectivo: campo "Recibido" editable + teclado + montos rápidos + cambio.
- "Venta confirmada" persiste hasta que el usuario salga; muestra ISV 15/18,
  total, recibido y cambio. NO muestra factura/CAI/vendedor (decisión del usuario).
- Vaciar resetea cliente + observaciones + selección.

## 8. Orden sugerido de implementación
1. Scaffold ruta `/facturacion` + layout full-screen + tema claro/oscuro.
2. Catálogo (productos/categorías/búsqueda) con datos reales.
3. Ticket + cálculo de ISV 15/18/exento desde `taxId` + catálogo de impuestos.
4. Cliente (buscar/seleccionar; crear según permiso).
5. Cobro directo (asistente) — **cuando se confirme el endpoint de factura**.
6. Guardar/recuperar órdenes.
7. Resto (cotizaciones, caja) conforme existan endpoints.

---
_Prototipo de referencia generado iterativamente. Ver `NOTAS.md` para la propuesta
pendiente de unificar "Recuperar" (órdenes + cotizaciones)._
