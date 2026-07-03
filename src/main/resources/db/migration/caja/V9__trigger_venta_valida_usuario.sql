-- =====================================================================
-- V9 (caja) — trigger de venta pasa el USUARIO al kardex (par de V33 común)
--
-- Recrea detalle_factura_b_insert (patrón V8: DROP+CREATE idempotente,
-- resolución de bodega en runtime, sin placeholders) con UN cambio:
-- resuelve el usuario de la factura desde el encabezado_factura de ESTA
-- caja (la fila del encabezado ya existe cuando se insertan los detalles)
-- y llama a admin_tools.crear_venta_kardex_v2(..., v_usuario), que valida
-- config_user_facturacion.facturar_sin_inventario DE VERDAD (la validación
-- de V19 quedó pasiva desde V27 porque el SP no podía resolver la factura
-- cross-schema — ver V33 común y retrospectiva epic INV §6).
--
-- Semántica: usuario sin config o con facturar_sin_inventario=1 → permite
-- (histórico). Solo bloquea (SIGNAL) al usuario con flag=0 explícito.
--
-- REQUIERE: común en V33 (SchemaMigrator migra común antes que cajas en el
-- mismo boot). El SP viejo crear_venta_kardex queda para cajas sin migrar.
--
-- NOTA API (regla US-101): esta migración debe copiarse al espejo de
-- admintools-api (src/main/resources/db/migration/caja) para que las cajas
-- provisionadas por POST /cajas nazcan con el trigger correcto.
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
    DECLARE v_usuario        VARCHAR(255);

    -- Resolver bodega de ESTA caja en runtime (DATABASE() = nombre BD actual).
    -- Fallback bodega=1 si la caja no esta registrada — comportamiento
    -- conservador identico al baseline historico V7.
    SET v_caja_db = DATABASE();
    SET v_codigo_bodega = COALESCE(
        (SELECT codigo_bodega FROM admin_tools.cajas WHERE nombre_db = v_caja_db LIMIT 1),
        1
    );

    -- Usuario que factura: del encabezado de ESTA caja (sin calificar =
    -- schema de la caja). NULL si no existe → v2 defaultea a permitir.
    SET v_usuario = (
        SELECT usuario FROM encabezado_factura
        WHERE numero_factura = NEW.numero_factura
        LIMIT 1
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
            CALL admin_tools.crear_venta_kardex_v2(
                v_cod_kardex,
                NEW.numero_factura,
                NEW.cantidad,
                v_usuario
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
            CALL admin_tools.crear_venta_kardex_v2(
                v_cod_kardex,
                NEW.numero_factura,
                NEW.cantidad,
                v_usuario
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
