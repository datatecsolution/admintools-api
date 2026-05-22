# admin-tools-api-v2 — Deploy paralelo

Levanta una nueva instancia del `admin-tools-api` en el server **sin
tocar** el container `admin-tools-api` actual. Cuando esté validado,
se corta tráfico al viejo.

## Layout en el server

```
/home/ronal/admintools-api/                  # repo del API (clonado)
├── Dockerfile
├── src/...
└── deploy/
    └── admin-tools-api-v2/
        ├── docker-compose.yml                # template, en git
        ├── .env-admintools.example           # template, en git
        ├── .env-admintools                   # creado manualmente, NO en git
        └── README.md
```

## Pasos para levantar (en el server)

```bash
ssh ronal@10.10.0.1

# 1. Pull de la rama feature/deploy-pdn-paralelo
cd /home/ronal/admintools-api
git fetch origin
git checkout feature/deploy-pdn-paralelo
git pull

# 2. Entrar a la carpeta del deploy
cd deploy/admin-tools-api-v2

# 3. Crear .env-admintools desde el template
cp .env-admintools.example .env-admintools
chmod 600 .env-admintools

# 4. Editar .env-admintools con los valores reales del cliente
nano .env-admintools
#    Mínimo a reemplazar:
#    - MYSQL_PASSWORD: password real del usuario `admin` de MySQL
#    - CORS_ALLOWED_ORIGINS: dominios/IPs desde donde se va a probar
#    Mantener (probado en cliente Ronal):
#    - APP_JWT_SECRET = el default histórico (tokens existentes seguirán valiendo)

# 5. Build + levantar
docker compose up -d --build

# 6. Verificar arranque limpio
docker logs admin-tools-api-v2 --tail 50
```

## Salida esperada en los logs

```
The following 1 profile is active: "pdn"
HikariPool-1 - Start completed
Initialized JPA EntityManagerFactory for persistence unit 'default'
Tomcat started on port(s): 8080 (http) with context path '/admin_tools/api'
Started AdmintoolsApplication in X.X seconds
```

**SIN** ningún `SchemaManagementException`. Si aparece → drift no
detectado en validación previa, hay que crear V17+ migración.

## Probar desde otra máquina (ej. MacBook 10.10.0.2)

```bash
# Swagger
curl http://10.10.0.1:8083/admin_tools/api/swagger-ui/index.html

# Login (te debe devolver un JWT)
curl -X POST http://10.10.0.1:8083/admin_tools/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"usuario":"admin","clave":"PASSWORD"}'

# Endpoint protegido con el token recibido
curl http://10.10.0.1:8083/admin_tools/api/orders/today \
  -H "Authorization: Bearer eyJ..."
```

## Cutover (cuando el v2 esté validado)

```bash
# 1. Cambiar el frontend para apuntar al v2 (env var de la React)
# 2. Verificar que el container viejo no tiene tráfico
docker logs admin-tools-api --tail 20

# 3. Apagar el viejo
docker stop admin-tools-api

# 4. (Opcional) Borrar el viejo y limpiar imágenes
docker rm admin-tools-api
docker image prune -f

# 5. (Opcional) Renombrar v2 al nombre canónico
# (Requiere editar docker-compose, cambiar container_name a admin-tools-api,
# cambiar port a 127.0.0.1:8082:8080 si se usa nginx-proxy, y recrear)
```

## Rollback

```bash
docker stop admin-tools-api-v2
# El viejo admin-tools-api sigue corriendo en 8082 — usuarios no se enteran
```
