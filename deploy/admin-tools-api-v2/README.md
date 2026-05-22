# admin-tools-api-v2 — Deploy paralelo

Levanta una nueva instancia del `admin-tools-api` en el server **sin
tocar** el container `admin-tools-api` actual. Cuando esté validado,
se corta tráfico al viejo.

## Layout en el server

```
/home/ronal/
├── admintools/                       # repo del API (ya clonado)
│   ├── Dockerfile
│   └── src/...
└── admin-tools-api-v2/                # NUEVA carpeta
    ├── docker-compose.yml
    ├── .env-admintools                # NO en git, chmod 600
    └── .env-admintools.example
```

## Pasos para levantar (en el server)

```bash
ssh ronal@ronalserver

# 1. Crear la carpeta
mkdir -p /home/ronal/admin-tools-api-v2
cd /home/ronal/admin-tools-api-v2

# 2. Copiar los archivos de este deploy (desde tu Mac vía scp o git pull
#    si ya tenés el repo admintools clonado y querés copiarlos manualmente)
scp /Users/jdmayorga/Desktop/admintools/deploy/admin-tools-api-v2/* \
    ronal@ronalserver:/home/ronal/admin-tools-api-v2/

# 3. Crear .env-admintools desde el template
cp .env-admintools.example .env-admintools
chmod 600 .env-admintools

# 4. Editar .env-admintools con las credenciales REALES del cliente
nano .env-admintools
#    - MYSQL_PASSWORD: la password real del usuario admin de MySQL
#    - APP_JWT_SECRET: generar con `openssl rand -base64 48` o usar el histórico
#    - CORS_ALLOWED_ORIGINS: dominios de los frontends (POS actual, nueva React)

# 5. Build + levantar
docker compose up -d --build

# 6. Verificar arranque limpio
docker logs admin-tools-api-v2 --tail 50

#    Esperás ver:
#      - The following 1 profile is active: "pdn"
#      - HikariPool-1 - Start completed
#      - Initialized JPA EntityManagerFactory (sin SchemaManagementException)
#      - Started AdmintoolsApplication
```

## Validar end-to-end

Con el v2 arriba en `127.0.0.1:8083`, podés:

```bash
# Health check directo (desde el server)
curl http://127.0.0.1:8083/admin_tools/api/swagger-ui/index.html
# Debe devolver el HTML del Swagger
```

Si querés probar desde tu Mac/cliente, expone temporalmente via
nginx-proxy-manager con un subdominio nuevo (ej. `api-v2.cliente.example`)
o tuneliza con SSH:

```bash
ssh -L 8083:127.0.0.1:8083 ronal@ronalserver
# después en tu Mac: http://localhost:8083/admin_tools/api/...
```

## Cuando esté validado

```bash
# 1. Cambiar el frontend para apuntar a v2 (cambio en nginx-proxy o env var React)
# 2. Verificar que el container viejo no tiene tráfico
docker logs admin-tools-api --tail 20

# 3. Apagar el viejo
docker stop admin-tools-api

# 4. (Opcional) Borrar el viejo
docker rm admin-tools-api
docker image prune    # libera capas viejas

# 5. (Opcional) Renombrar v2 al nombre canónico
# (requiere docker-compose con container_name: admin-tools-api)
```

## Rollback

```bash
docker stop admin-tools-api-v2
# El viejo admin-tools-api sigue corriendo en 8082 — usuarios no se enteran
```
