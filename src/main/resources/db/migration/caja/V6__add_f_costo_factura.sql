-- Agrega f_costo_factura en bases de caja que no la tenían
-- (clientes existentes donde V1 fue baselineada sin ejecutar).
SET @ADMIN_TOOLS_ORIG_LBT = @@GLOBAL.log_bin_trust_function_creators;
SET GLOBAL log_bin_trust_function_creators = 1;

DROP FUNCTION IF EXISTS `f_costo_factura`;

DELIMITER $$
CREATE FUNCTION `f_costo_factura`(p_numero_factura int(11)) RETURNS double(11,2)
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

SET GLOBAL log_bin_trust_function_creators = @ADMIN_TOOLS_ORIG_LBT;
