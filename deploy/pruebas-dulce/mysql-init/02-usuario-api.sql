-- Usuario MySQL para la API de pruebas (el del .env-pruebas).
-- Acceso solo a los esquemas admin_tools* del stack.
CREATE USER IF NOT EXISTS 'apipruebas'@'%' IDENTIFIED BY 'CAMBIAR_EN_EL_SERVER';
GRANT ALL PRIVILEGES ON `admin_tools`.* TO 'apipruebas'@'%';
GRANT ALL PRIVILEGES ON `admin_tools_caja_1`.* TO 'apipruebas'@'%';
GRANT ALL PRIVILEGES ON `admin_tools_caja_2`.* TO 'apipruebas'@'%';
GRANT ALL PRIVILEGES ON `admin_tools_caja_3`.* TO 'apipruebas'@'%';
GRANT ALL PRIVILEGES ON `admin_tools_caja_4`.* TO 'apipruebas'@'%';
FLUSH PRIVILEGES;
