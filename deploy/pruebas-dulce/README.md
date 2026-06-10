# Pruebas con datos reales (cliente dulce) — server Ronal

Stack Docker **aislado** de la producción de Ronal: MySQL propio con el
backup de dulce + API + POS. No toca el MySQL del host ni el
`admin-tools-api-v2` que sirve pedidos.

| Servicio | Container | Puerto host |
|---|---|---|
| MySQL 8.0 (datos dulce) | `mysql-pruebas-dulce` | `127.0.0.1:3307` |
| API (perfil pdn) | `api-pruebas-dulce` | `8084` |
| POS (nginx) | `pos-pruebas-dulce` | `8090` |

## 1. Preparar en la máquina local

```bash
# Backup desde el server de dulce (192.168.1.23) — conexión en
# ~/Library/Application Support/AdminTools/dulce_connection.dat (Cifrado.java):
mysqldump -h192.168.1.23 -u<user> -p --single-transaction --routines --triggers --events \
  --databases admin_tools admin_tools_caja_1 admin_tools_caja_2 admin_tools_caja_3 admin_tools_caja_4 \
  > dulce_backup_$(date +%Y%m%d).sql

# OBLIGATORIO: quitar los DEFINER (usuarios de dulce no existen en el
# contenedor; sin esto la carga inicial aborta con ERROR 1449):
python3 - <<'PY'
import re
src = open('dulce_backup_YYYYMMDD.sql', encoding='utf-8', errors='replace').read()
out = re.sub(r'DEFINER=`[^`]+`@`[^`]+`', '', src).replace('SQL SECURITY DEFINER', 'SQL SECURITY INVOKER')
open('dulce_backup_sanitizado.sql', 'w', encoding='utf-8').write(out)
PY
```

## 2. Subir al server

```bash
ssh ronal@10.10.0.1 "mkdir -p /home/ronal/pruebas-dulce/stack"

# Repos (la primera vez):
ssh ronal@10.10.0.1 "cd /home/ronal/pruebas-dulce && \
  git clone https://github.com/datatecsolution/admintools-api.git && \
  git clone https://github.com/datatecsolution/admintools-pos.git"

# Stack + dump:
scp -r deploy/pruebas-dulce/* ronal@10.10.0.1:/home/ronal/pruebas-dulce/stack/
scp dulce_backup_sanitizado.sql \
    ronal@10.10.0.1:/home/ronal/pruebas-dulce/stack/mysql-init/00-dulce.sql
```

## 3. Configurar y levantar (en el server)

```bash
ssh ronal@10.10.0.1
cd /home/ronal/pruebas-dulce/stack

# Credenciales: editar .env Y el password de 02-usuario-api.sql
cp .env.example .env && chmod 600 .env
nano .env
nano mysql-init/02-usuario-api.sql   # mismo password que MYSQL_PASSWORD

docker compose up -d --build
docker logs -f mysql-pruebas-dulce   # esperar "ready for connections" (carga el dump, ~1 min)
docker logs api-pruebas-dulce --tail 50   # esperar "Started AdmintoolsApplication"
```

## 4. Verificar

```bash
# API arriba y validando el esquema de dulce:
curl -s -o /dev/null -w "%{http_code}\n" http://10.10.0.1:8084/admin_tools/api/cajas   # 401 = OK (pide token)

# Login con un usuario real de dulce:
curl -s -X POST http://10.10.0.1:8084/admin_tools/api/auth/login \
  -H "Content-Type: application/json" -d '{"username":"<usuario>","password":"<clave>"}'

# POS: abrir http://10.10.0.1:8090 en el navegador y hacer login.
```

## Notas

- **Claves de usuarios**: la API valida con BCrypt. En el dump de dulce,
  `tecnico`, `admin` y `caja1` ya tienen hash BCrypt (login OK con sus
  claves reales); `ventas`, `david` y `jdmayorga` tienen clave en texto
  plano (legacy del Swing) y **no podrán loguear** en la API/POS hasta
  re-encriptarlas. Si se necesita probar con ellos, generar un hash de
  prueba SOLO en el stack: `htpasswd -bnBC 10 "" <clave> | tr -d ':'`
  (cambiar `$2y` por `$2a`) y `UPDATE usuario SET clave='<hash>',
  enabled=1 WHERE usuario='...';` en el MySQL del stack (puerto 3307).


- **Apagar/resetear**: `docker compose down` conserva los datos
  (volumen `mysql-pruebas-data`); `docker compose down -v` borra el
  volumen y la próxima subida recarga el dump limpio — útil para
  repetir pruebas desde cero.
- El dump incluye rutinas/triggers (kardex); `log-bin-trust-function-creators=1`
  evita el error de creación de triggers sin SUPER.
- `mysql-init/01-v30...` aplica la V30 (dulce quedó en V29): columna
  `crear_cliente_credito` que la API nueva consulta.
- Si se quiere dominio + HTTPS en vez de IP:puerto, crear proxy hosts
  en nginx-proxy-manager (como pedidos.distribuidorasharon.com) hacia
  `api-pruebas-dulce:8080` y `pos-pruebas-dulce:80`, y ajustar
  `CORS_ALLOWED_ORIGINS` + `POS_API_URL` + rebuild del POS.
- La caja activa del JWT (TenantContext) usa `nombre_db` de la tabla
  cajas de dulce (`admin_tools_caja_1..4`) — el dump las incluye, no
  hay nada que ajustar.
