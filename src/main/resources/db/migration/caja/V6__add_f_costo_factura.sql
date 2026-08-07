-- Agrega f_costo_factura en bases de caja que no la tenían
-- (clientes existentes donde V1 fue baselineada sin ejecutar).
-- US-138: sin `SET GLOBAL` (exige SUPER); la funcion declara su
-- caracteristica SQL, que es lo que MySQL necesita con el binlog activo.

DROP FUNCTION IF EXISTS `f_costo_factura`;

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
