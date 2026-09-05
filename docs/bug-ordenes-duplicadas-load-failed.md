# Bug · App de pedidos — Órdenes duplicadas al guardar desde el celular ("Load failed")

> **Estado:** ✅ **CORREGIDO (US-150, 2026-09-05)** — Opción A (`clientRef`) + timeout y aviso en la app.
> Validado E2E en local (doble POST con el mismo ref → una sola orden). **Pendiente: deploy a producción.**
> **Fecha de diagnóstico:** 2026-06-08 · **Fecha del fix:** 2026-09-05
> **Etiquetas:** Bug · Producción · App de pedidos · Prioridad media-alta
> **Componentes:** `at-ordenes-ventas` (app de pedidos) · `admintools-api` (`/orders/save`) · `adminTools` (V48)

## Actualización 2026-09-04 — impacto real medido en producción

La investigación del caso "facturas 91 y 332 de caja_yisell" (Sharon) confirmó que
los duplicados **sí llegan a doble factura**: cada copia de la orden se factura por
separado, a veces en **cajas distintas** (indetectable para el cajero). Medido con
huella exacta de líneas (mismo cliente + mismos artículos/cantidades, <24 h):

- **jun: 30 pares · jul: 6 · ago: 11 (6 con ambas copias facturadas) · sep (2 días): 2**
- **6 pares de facturas duplicadas AMBAS ACTIVAS** al 2026-09-04 (~L 5,423):
  clientes 984 (caja_4 37704+37752), 4273 (caja_7 390+428), 4290 (caja_7 182+219),
  626 (caja_2 262803+262817), 856 (caja_4 35888 + caja_2 261342), 1619 (caja_4
  36653 + caja_2 262254). Otros 4 pares fueron anulados a mano por el personal.
- 8 vendedores distintos afectados → sistémico, no error de digitación.
- El patrón "re-envío 1-2 h después" NO es una cola offline (no existe tal código);
  es el carrito que sobrevive en pantalla tras el fallo y se re-guarda.
- Vector secundario detectado: el retry automático post-401 de `apiClient.js`
  re-enviaba el POST completo (quedó inocuo con el `clientRef`).

## Fix aplicado (US-150)

- **BD (adminTools, V48):** `encabezado_factura_temp.client_ref VARCHAR(36) NULL`
  + UNIQUE. Filas históricas y Swing/POS no afectados.
- **API:** si el `clientRef` ya existe, `/orders/save` devuelve la orden existente
  sin crear otra (y sin re-validar stock); la carrera de dos POST simultáneos la
  cierra el UNIQUE (el perdedor relee y devuelve la ganadora). Suite 213/213.
- **App:** `clientRef` (UUID) generado al primer intento y conservado entre
  reintentos; timeout de 20 s con AbortController; aviso claro tras fallo de red
  ("tocá Guardar de nuevo: no se va a duplicar").
- **Orden de deploy:** V48 debe estar aplicada ANTES de desplegar la API nueva
  (ddl-auto=validate). La app vieja sin `clientRef` sigue funcionando igual.
- **Remediación pendiente:** confirmar con el cliente los 6 pares activos y anular
  la copia por el flujo normal de anulación (backup previo).

---

## Síntoma reportado

Un usuario (iPhone) reporta el error en pantalla:

```
Error al crear la orden: TypeError: Load failed
```

Además, **las órdenes se están duplicando** al guardarlas.

## Diagnóstico (confirmado 2026-06-08)

- El servidor está **sano**: container `admin-tools-api-v2` con 6 días de uptime,
  **0 reinicios**, sin OOM, y **sin errores** de `/orders/save` en los logs.
- La app y el API están en el **mismo dominio** (`pedidos.distribuidorasharon.com`)
  → **no es problema de CORS**.
- `TypeError: Load failed` es el mensaje de **Safari/iOS cuando un request falla a
  nivel de red** (la respuesta no vuelve al teléfono).
- **Mecanismo del duplicado:** el `POST /orders/save` **sí llega y crea la orden** en
  el servidor, pero la respuesta se pierde en el celular → la app muestra "error" y
  **no limpia la pantalla** → el usuario **vuelve a tocar Guardar** → se crea una
  **segunda orden** (la app no tiene cómo saber que la primera ya se guardó).
- **Causa raíz:** el endpoint de guardado **no es idempotente** (no protege contra
  reintentos). Es un patrón conocido en apps móviles con red inestable.

### Flujo del problema

```
Usuario toca Guardar
  → POST /orders/save ──(el server la crea)──► ✅ orden creada
  → pero la respuesta no vuelve al iPhone  ✗  "Load failed"
  → la app cae en .catch → muestra "Error al crear la orden"
  → NO limpia la pantalla (ticket/cliente/items siguen)
  → el usuario piensa "falló" y toca Guardar otra vez
  → POST /orders/save de nuevo ──► ❌ SEGUNDA orden (duplicado)
```

## Evidencia en base de datos (producción)

Órdenes idénticas (mismo cliente y mismo total) creadas con segundos de diferencia:

| Usuario     | Cliente | Total         | # órdenes              | Detalle                              |
|-------------|---------|---------------|------------------------|--------------------------------------|
| GABRIEL     | 1972    | L. 57,127.22  | **5** (#91807–91811)   | en ~3 min; 4 anuladas + 1 facturada  |
| LORENZOFER  | 838     | L. 12,320.00  | 2                      | 1 anulada + 1 facturada              |
| DORIANM     | 1712    | L. 10,598.00  | 2                      | 1 anulada + 1 facturada              |
| SERRANOALE  | 1465    | L. 1,588.00   | 2                      | 1 anulada + 1 facturada              |

El caso de **GABRIEL es el más claro**: 5 órdenes idénticas (mismo cliente, mismo
total exacto `57127.22`) creadas con segundos de diferencia = reintento tras
"Load failed". El operador después anula las copias a mano y deja una.

> Nota: los pares con **total distinto y ambas facturadas** son probablemente
> órdenes legítimas separadas del mismo cliente, **no** duplicados. Los duplicados
> son los de **total idéntico con una anulada**.

## Impacto

- El operador debe **anular los duplicados a mano** cada vez (trabajo extra,
  propenso a error).
- **Riesgo de doble facturación** si se factura un duplicado por equivocación.
- Mala experiencia para el vendedor en campo.

## Estado actual

- Producción corre `main @ 39b795d` (~2026-05-22). El bug **no es por una mala
  actualización**; es de diseño (falta idempotencia).
- **Diagnosticado, sin cambios aplicados.**

---

## Opciones para decidir (a definir con el cliente)

### Opción A — Idempotencia (`clientRef`) · *Solución de fondo, recomendada*
La app manda un identificador único por orden (`clientRef`); el servidor, si lo
recibe repetido, **devuelve la misma orden en vez de crear otra**. Aunque el celular
reintente 5 veces, **se crea una sola**.
- **Toca:** API + app + 1 columna nueva en BD (migración Flyway aditiva, segura).
- **Deploy:** rebuild del API y del front.
- **Resultado:** elimina el duplicado de raíz.

### Opción B — Aviso en la app ("guard") · *Mitigación rápida, solo front*
Tras un "Load failed", la app **verifica con el servidor si la orden ya se guardó**
(refresca la lista y busca un match) y avisa, en vez de dejar que el usuario
re-guarde a ciegas.
- **Toca:** solo `at-ordenes-ventas` (el `.catch` del guardado).
- **Deploy:** solo build del front (sin API ni BD).
- **Resultado:** reduce duplicados, **no** los elimina del todo (match heurístico;
  depende de que el refresco no falle también).

### Opción C — Dedup heurístico (solo API) · *Parche puente*
El servidor rechaza una orden idéntica (mismo usuario/cliente/total/líneas) creada
en los últimos ~60s.
- **Toca:** solo API (sin BD ni app).
- **Riesgo:** falsos positivos en órdenes idénticas legítimas seguidas.

### Recomendación técnica
**A (fondo) + B (experiencia).** Si se quiere algo en producción ya, empezar por **B**
(menor riesgo, no toca backend) y luego migrar a **A**.

---

## Notas técnicas de referencia

- Endpoint: `POST /admin_tools/api/orders/save` → tabla `encabezado_factura_temp`.
- App: `at-ordenes-ventas`, `src/componets/PruebaMenu.jsx` (función de guardado;
  el `.catch` actual solo hace `toast.error(...)`, no resetea ni verifica).
- "Load failed" = error de red de WebKit/Safari (equivalente a "Failed to fetch" en
  Chrome). Mismo origen → descartado CORS.
