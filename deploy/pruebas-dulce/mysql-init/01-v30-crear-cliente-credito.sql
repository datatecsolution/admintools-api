-- V30 del Swing (la BD dulce quedó en V29 al momento del dump).
-- La API nueva consulta crear_cliente_credito; sin la columna el
-- endpoint /sellers y el alta de clientes crédito fallan en runtime.
USE admin_tools;
ALTER TABLE config_user_facturacion
    ADD COLUMN crear_cliente_credito TINYINT NOT NULL DEFAULT 0;
INSERT INTO schema_version (installed_rank, version, description, type, script, checksum, installed_by, execution_time, success)
SELECT max(installed_rank)+1, '30', 'config crear cliente credito', 'SQL',
       'V30__config_crear_cliente_credito.sql', NULL, 'stack-pruebas', 0, 1
FROM schema_version;
