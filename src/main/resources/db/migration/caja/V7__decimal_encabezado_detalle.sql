-- =====================================================================
-- V7 (caja) — float a DECIMAL(15,2) en columnas monetarias de
-- encabezado_factura y detalle_factura LOCALES de cada caja
--
-- Espejo de V13 del schema common (US-014). Cada base de caja
-- (admin_tools_caja_1, _2, _3, _4) tiene sus PROPIAS tablas fisicas
-- de encabezado_factura y detalle_factura (no son federated ni views;
-- se confirmo viendo COUNT distinto por caja). V13 solo migro
-- admin_tools (central), las cajas quedaron en float y siguen
-- generando descuadres por redondeo.
--
-- Precision = 15, scale = 2: rango [-9.999.999.999.999,99 a
-- +9.999.999.999.999,99]. Auditoria previa confirmo MAX < 100K en
-- todas las cajas observadas — sobra capacidad.
--
-- Idempotencia: ALTER TABLE ... MODIFY es self-idempotent.
--
-- Riesgo: ALTER bloquea las tablas durante el rebuild. Cajas
-- observadas tienen 1K-50K filas → segundos cada una.
-- =====================================================================

-- encabezado_factura — 12 columnas monetarias
ALTER TABLE `encabezado_factura`
    MODIFY `subtotal_excento` DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    MODIFY `subtotal15`       DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    MODIFY `subtotal18`       DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    MODIFY `subtotal`         DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    MODIFY `impuesto`         DECIMAL(15,2) NOT NULL,
    MODIFY `total`            DECIMAL(15,2) NOT NULL,
    MODIFY `isvOtros`         DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    MODIFY `isv18`            DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    MODIFY `pago`             DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    MODIFY `descuento`        DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    MODIFY `cobro_tarjeta`    DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    MODIFY `cobro_efectivo`   DECIMAL(15,2) NOT NULL DEFAULT 0.00;

-- detalle_factura — 5 columnas monetarias (cantidad se queda float, no es monetaria)
ALTER TABLE `detalle_factura`
    MODIFY `precio`    DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    MODIFY `impuesto`  DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    MODIFY `subtotal`  DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    MODIFY `descuento` DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    MODIFY `total`     DECIMAL(15,2) NOT NULL DEFAULT 0.00;
