-- Baseline de `admin_tools_caja_1`
-- Generado 2026-04-14 08:38:03 por SchemaDumper
-- Fuente: conexion activa de ConexionStatic

SET FOREIGN_KEY_CHECKS=0;
SET SQL_MODE='NO_AUTO_VALUE_ON_ZERO';
-- US-138: antes aqui se hacia `SET GLOBAL log_bin_trust_function_creators=1`
-- para poder crear funciones con el binlog activo. Ese SET exige SUPER /
-- SYSTEM_VARIABLES_ADMIN, privilegios que el usuario de la API no tiene en
-- los clientes, y rompia el provisioning de cajas nuevas (error 1227).
-- La alternativa que no necesita privilegios globales: declarar la
-- caracteristica SQL de la funcion (READS SQL DATA), que es justo lo que
-- MySQL pide cuando el flag esta en 0.

-- ============================================
-- TABLAS (3)
-- ============================================

CREATE TABLE `datos_factura` (
  `codigo_rango` int NOT NULL AUTO_INCREMENT,
  `CAI` varchar(300) NOT NULL DEFAULT 'NA',
  `factura_inicial` varchar(11) NOT NULL DEFAULT 'NA',
  `factura_final` varchar(11) NOT NULL DEFAULT 'NA',
  `codigo_tipo_facturacion` varchar(50) NOT NULL DEFAULT 'NA',
  `cantida_solicitada` int NOT NULL DEFAULT '0',
  `fecha_limite_emision` date NOT NULL DEFAULT '1990-01-01',
  `observacion` varchar(255) DEFAULT '',
  PRIMARY KEY (`codigo_rango`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC;

CREATE TABLE `detalle_factura` (
  `numero_factura` int NOT NULL,
  `codigo_articulo` int NOT NULL,
  `precio` float(11,2) NOT NULL DEFAULT '0.00',
  `cantidad` float(11,2) NOT NULL DEFAULT '0.00',
  `impuesto` float(11,2) NOT NULL DEFAULT '0.00',
  `subtotal` float(11,2) NOT NULL DEFAULT '0.00',
  `descuento` float(11,2) NOT NULL DEFAULT '0.00',
  `total` float(11,2) NOT NULL DEFAULT '0.00',
  `id` int NOT NULL AUTO_INCREMENT,
  `codigo_barra` varchar(255) NOT NULL DEFAULT 'NA',
  `agrega_kardex` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `codigo_articulo` (`codigo_articulo`) USING BTREE,
  KEY `numero_factura` (`numero_factura`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;

CREATE TABLE `encabezado_factura` (
  `numero_factura` int NOT NULL AUTO_INCREMENT,
  `fecha` datetime NOT NULL,
  `subtotal_excento` float(8,2) NOT NULL DEFAULT '0.00',
  `subtotal15` float(8,2) NOT NULL DEFAULT '0.00',
  `subtotal18` float(8,2) NOT NULL DEFAULT '0.00',
  `subtotal` float(8,2) NOT NULL DEFAULT '0.00',
  `impuesto` float(8,2) NOT NULL,
  `total` float(10,2) NOT NULL,
  `codigo_cliente` int NOT NULL,
  `codigo` varchar(11) NOT NULL DEFAULT '-1',
  `estado_factura` varchar(25) NOT NULL DEFAULT 'NA',
  `isvOtros` float(8,2) NOT NULL DEFAULT '0.00',
  `isv18` float(11,2) NOT NULL DEFAULT '0.00',
  `usuario` varchar(255) NOT NULL DEFAULT 'SYSTEM',
  `pago` float(11,2) NOT NULL DEFAULT '0.00',
  `descuento` float(11,2) NOT NULL DEFAULT '0.00',
  `tipo_factura` int NOT NULL DEFAULT '1',
  `agrega_kardex` int NOT NULL DEFAULT '0',
  `tipo_pago` int NOT NULL,
  `observacion` varchar(255) NOT NULL DEFAULT 'NA',
  `total_letras` varchar(500) NOT NULL DEFAULT 'NA',
  `codigo_vendedor` int NOT NULL DEFAULT '1',
  `estado_pago` int NOT NULL DEFAULT '0',
  `cod_rango` int NOT NULL DEFAULT '1',
  `cobro_tarjeta` float(11,2) NOT NULL DEFAULT '0.00',
  `cobro_efectivo` float(11,2) NOT NULL DEFAULT '0.00',
  `fecha_vencimiento` date NOT NULL DEFAULT '1990-01-01',
  PRIMARY KEY (`numero_factura`),
  UNIQUE KEY `numero_factura` (`numero_factura`) USING BTREE,
  KEY `codigo_cliente` (`codigo_cliente`) USING BTREE,
  KEY `tipo_factura` (`tipo_factura`) USING BTREE,
  KEY `usuario` (`usuario`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;

-- ============================================
-- FUNCTIONS (1)
-- ============================================

DELIMITER $$
CREATE FUNCTION `f_costo_factura`(p_numero_factura int(11)) RETURNS double(11,2)
    READS SQL DATA
BEGIN
	return (SELECT

	SUM(
		cantidad * precios_articulos.precio_articulo
	) AS total_costo
FROM
	detalle_factura
INNER JOIN admin_tools.precios_articulos ON (
	detalle_factura.codigo_articulo = admin_tools.precios_articulos.codigo_articulo
	AND admin_tools.precios_articulos.codigo_precio = 4
)
WHERE
	numero_factura= p_numero_factura) ;
end$$
DELIMITER ;

-- ============================================
-- TRIGGERS (2)
-- ============================================

DELIMITER $$
CREATE TRIGGER `detalle_factura_b_insert` BEFORE INSERT ON `detalle_factura` FOR EACH ROW BEGIN  declare cod_kardex int;  declare tipo_articulo int;  set cod_kardex =(SELECT codigo_kardex FROM admin_tools.articulo_kardex WHERE (codigo_articulo = NEW.codigo_articulo AND	codigo_bodega = ${codigo_bodega}) limit 1); set tipo_articulo=(SELECT t1.tipo_articulo from admin_tools.articulo t1 where t1.codigo_articulo=NEW.codigo_articulo LIMIT 1); if(cod_kardex is null) then   if( tipo_articulo =1) then INSERT INTO admin_tools.articulo_kardex(codigo_articulo,codigo_bodega) VALUES (NEW.codigo_articulo,${codigo_bodega}); set cod_kardex=(select last_insert_id()); CALL admin_tools.crear_ajuste_inventario_kardex(cod_kardex,NEW.cantidad,NEW.precio,'facturado en ${caja_db}'); call admin_tools.crear_venta_kardex(cod_kardex,NEW.numero_factura,NEW.cantidad); set NEW.agrega_kardex=1;  end if;  if( tipo_articulo =2) then  CALL admin_tools.crear_venta_insumo_kardex(${codigo_bodega},NEW.codigo_articulo,NEW.cantidad,'facturado en ${caja_db}',NEW.numero_factura);  set NEW.agrega_kardex=1;  end if; ELSE  if( tipo_articulo =1) then call admin_tools.crear_venta_kardex(cod_kardex,NEW.numero_factura,NEW.cantidad); set NEW.agrega_kardex=1;  end if;  if( tipo_articulo =2) then  CALL admin_tools.crear_venta_insumo_kardex(${codigo_bodega},NEW.codigo_articulo,NEW.cantidad,'facturado en ${caja_db}',NEW.numero_factura);  set NEW.agrega_kardex=1;  end if;  end if;  end$$
DELIMITER ;

DELIMITER $$
CREATE TRIGGER `validacion1` BEFORE INSERT ON `encabezado_factura` FOR EACH ROW BEGIN
	
	IF NEW.total= 0 THEN
               SIGNAL SQLSTATE '45000'
                    SET MESSAGE_TEXT = 'ERROR 2303: Ocurrio un problema con la factura.';
	
  END IF;
end$$
DELIMITER ;
SET FOREIGN_KEY_CHECKS=1;
