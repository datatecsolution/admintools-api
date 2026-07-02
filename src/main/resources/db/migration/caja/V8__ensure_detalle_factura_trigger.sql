-- =====================================================================
-- V8 — Asegurar trigger detalle_factura_b_insert en CADA caja
--
-- PROBLEMA: el trigger esta definido en caja/V1__baseline.sql, pero esa
-- baseline solo corre cuando se CREA una caja nueva via
-- SchemaMigrator.migrateNewCajaDatabase(). Las cajas viejas (importadas
-- desde dumps anteriores a Flyway, o bases creadas a mano antes de que
-- existieran las migraciones de caja) quedaron sin trigger y por lo tanto
-- sus ventas NO descuentan inventario — bug silencioso de inventario.
--
-- SOLUCION: esta V8 recrea el trigger idempotentemente (DROP IF EXISTS +
-- CREATE) en cada caja, asegurando que TODOS los clientes (viejos y nuevos)
-- queden con el comportamiento correcto. En cajas que ya lo tenian, el
-- DROP+CREATE produce un trigger identico — no rompe nada.
--
-- DIFERENCIAS contra el trigger de V1__baseline:
--
-- 1) SIN placeholders Flyway ${codigo_bodega} / ${caja_db}.
--    En V1, esos valores se sustituyen al CREATE de la caja
--    (SchemaMigrator.migrateNewCajaDatabase recibe codigoBodega de
--    CajaDao.registrar). En migrateAll() — el path normal del arranque
--    de Swing — el placeholder se pasaria con un valor por defecto (1)
--    que seria INCORRECTO para cajas atadas a bodegas distintas. Este
--    trigger resuelve la bodega EN RUNTIME: lee admin_tools.cajas
--    WHERE nombre_db = DATABASE(). Eso lo hace seguro de aplicar via
--    migrateAll() y se autoadapta si la bodega de la caja cambia.
--
-- 2) Fallback bodega=1 si la caja NO esta en admin_tools.cajas. Es
--    comportamiento conservador identico al baseline historico V7
--    (codigo_bodega hardcoded a 1). Si una caja existe en el cluster
--    pero no esta registrada, sus ventas siguen contabilizando — solo
--    que contra Tienda Principal en lugar de su bodega real. Esto
--    preserva el comportamiento que tenian los clientes hasta ahora.
--
-- 3) Llama admin_tools.crear_venta_kardex que YA esta corregido por la
--    V19 common (header lock + saldo FOR UPDATE + SIGNAL para sobreventa
--    + UPSERT a existencia_articulo_bodega). El trigger se beneficia
--    automaticamente de toda la correctness fixed en V19.
--
-- LOGICA DEL TRIGGER (igual a V1 baseline):
--   - Resuelve codigo_kardex de (articulo, bodega).
--   - Si no existe: crea articulo_kardex inicial, hace ajuste (entrada)
--     y luego la venta. Aplica solo a tipo_articulo=1 (bienes).
--   - Si existe: solo la venta (bienes) o crear_venta_insumo_kardex
--     (tipo_articulo=2, insumos).
--   - Marca NEW.agrega_kardex=1 al completar el procesamiento.
--
-- VALIDACION post-aplicacion (cualquier caja):
--   SHOW TRIGGERS LIKE 'detalle_factura';
--   -- debe mostrar detalle_factura_b_insert + validacion1
--
-- ROLLBACK: ninguno necesario. Si por alguna razon se quisiera revertir,
-- DROP TRIGGER detalle_factura_b_insert deja la caja en el estado bug
-- (ventas sin descuento de kardex) — pero eso es justo lo que esta V8
-- corrige.
-- =====================================================================

DROP TRIGGER IF EXISTS detalle_factura_b_insert;

DELIMITER $$

CREATE TRIGGER detalle_factura_b_insert
BEFORE INSERT ON detalle_factura
FOR EACH ROW
BEGIN
    DECLARE v_caja_db        VARCHAR(64);
    DECLARE v_codigo_bodega  INT;
    DECLARE v_cod_kardex     INT;
    DECLARE v_tipo_articulo  INT;

    -- Resolver bodega de ESTA caja en runtime (DATABASE() = nombre BD actual).
    -- Fallback bodega=1 si la caja no esta registrada — comportamiento
    -- conservador identico al baseline historico V7.
    SET v_caja_db = DATABASE();
    SET v_codigo_bodega = COALESCE(
        (SELECT codigo_bodega FROM admin_tools.cajas WHERE nombre_db = v_caja_db LIMIT 1),
        1
    );

    SET v_cod_kardex = (
        SELECT codigo_kardex
        FROM   admin_tools.articulo_kardex
        WHERE  codigo_articulo = NEW.codigo_articulo
          AND  codigo_bodega = v_codigo_bodega
        LIMIT 1
    );

    SET v_tipo_articulo = (
        SELECT tipo_articulo
        FROM   admin_tools.articulo
        WHERE  codigo_articulo = NEW.codigo_articulo
        LIMIT 1
    );

    IF v_cod_kardex IS NULL THEN
        -- Bienes: crear kardex + ajuste inicial + venta.
        IF v_tipo_articulo = 1 THEN
            INSERT INTO admin_tools.articulo_kardex(codigo_articulo, codigo_bodega)
            VALUES (NEW.codigo_articulo, v_codigo_bodega);
            SET v_cod_kardex = LAST_INSERT_ID();
            CALL admin_tools.crear_ajuste_inventario_kardex(
                v_cod_kardex,
                NEW.cantidad,
                NEW.precio,
                CONCAT('facturado en ', v_caja_db)
            );
            CALL admin_tools.crear_venta_kardex(
                v_cod_kardex,
                NEW.numero_factura,
                NEW.cantidad
            );
            SET NEW.agrega_kardex = 1;
        END IF;
        -- Insumos: solo venta (los insumos no llevan ajuste inicial).
        IF v_tipo_articulo = 2 THEN
            CALL admin_tools.crear_venta_insumo_kardex(
                v_codigo_bodega,
                NEW.codigo_articulo,
                NEW.cantidad,
                CONCAT('facturado en ', v_caja_db),
                NEW.numero_factura
            );
            SET NEW.agrega_kardex = 1;
        END IF;
    ELSE
        -- Kardex ya existe.
        IF v_tipo_articulo = 1 THEN
            CALL admin_tools.crear_venta_kardex(
                v_cod_kardex,
                NEW.numero_factura,
                NEW.cantidad
            );
            SET NEW.agrega_kardex = 1;
        END IF;
        IF v_tipo_articulo = 2 THEN
            CALL admin_tools.crear_venta_insumo_kardex(
                v_codigo_bodega,
                NEW.codigo_articulo,
                NEW.cantidad,
                CONCAT('facturado en ', v_caja_db),
                NEW.numero_factura
            );
            SET NEW.agrega_kardex = 1;
        END IF;
    END IF;
END$$

DELIMITER ;
