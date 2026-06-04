# Notas del proyecto — Facturación Táctil (Datatec POS)

## Propuesta pendiente (guardada para más tarde)
**Unificar "Lista de órdenes" + "Lista de cotizaciones" en un botón "Recuperar".**
- Un solo botón en la barra que abre un selector con pestañas: **Órdenes | Cotizaciones**
  (reutilizando el patrón del selector de clientes: buscador + lista paginada).
- Respeta que son **tablas distintas** en la BD: cada pestaña pega a su endpoint
  (`/orders/...` vs cotizaciones). Tocar una fila la carga al ticket.
- Barra resultante: Descuentos · Precios · Cantidad · Guardar · **Recuperar** · Más
- Estado: NO implementado aún (el usuario quiere dejar la barra como está por ahora).

## Estado actual de la barra de funciones
- **Barra:** Descuentos · Precios · Cantidad · Guardar · Lista de órdenes · Más
- **Más → Documento:** Crear cotización · Lista de cotizaciones
- **Más → Caja:** Pago cliente · Pago proveedores · Salida de efectivo · Cierre de caja

## Funciones ya implementadas (interactivas)
- **Cantidad** (teclado numérico sobre la línea seleccionada).
- **Cobrar** (asistente guiado: Vendedor → Observaciones → Pago → Venta confirmada).
Las demás funciones de la barra/Más son placeholders (modal descriptivo).
