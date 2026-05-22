# Deploy a producción — admin-tools-api

Levanta el container del API con perfil `pdn`, `ddl-auto=validate` y
credenciales externalizadas en `.env-admintools`.

## Layout en el server (ejemplo Ronal)

```
/home/ronal/admintools-api/           # este repo clonado
├── Dockerfile
├── docker-compose.yml                 # producción, en git
├── .env-admintools.example            # template, en git
├── .env-admintools                    # credenciales reales, NO en git (chmod 600)
├── libs/                              # admintools-core JAR (en git)
├── src/...                            # código del API
└── ...
```

## Pasos para deployar (en el server)

```bash
ssh ronal@10.10.0.1
cd /home/ronal/admintools-api

# 1. Asegurar última versión del código
git checkout main
git pull origin main

# 2. Crear .env-admintools desde el template (solo la primera vez)
cp .env-admintools.example .env-admintools
chmod 600 .env-admintools

# 3. Editar con los valores reales del cliente
nano .env-admintools
#    Mínimo a reemplazar:
#    - MYSQL_PASSWORD: password real del usuario `admin` de MySQL
#    - CORS_ALLOWED_ORIGINS: dominios/IPs desde donde se va a consumir
#    Mantener (probado en cliente Ronal):
#    - APP_JWT_SECRET: el default histórico, así tokens existentes
#      siguen valiendo. Rotar en otra ventana si querés mayor seguridad.

# 4. Build + levantar
docker compose up -d --build

# 5. Verificar arranque limpio
docker logs admin-tools-api-v2 --tail 60
```

## Salida esperada en los logs

```
The following 1 profile is active: "pdn"
HikariPool-1 - Start completed
Initialized JPA EntityManagerFactory for persistence unit 'default'
Tomcat started on port(s): 8080 (http) with context path '/admin_tools/api'
Started AdmintoolsApplication in X.X seconds
```

**SIN** ninguna línea `SchemaManagementException`. Si aparece → hay drift
en el schema; ver memoria del proyecto sobre cómo detectarlo y crear
una migración Flyway que lo resuelva.

## Probar end-to-end (ej. desde otra máquina en 10.10.0.2)

```bash
# Swagger
curl -I http://10.10.0.1:8083/admin_tools/api/swagger-ui/index.html

# Login (devuelve JWT)
curl -X POST http://10.10.0.1:8083/admin_tools/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"usuario":"admin","clave":"PASSWORD"}'

# Endpoint protegido con el token recibido
curl http://10.10.0.1:8083/admin_tools/api/orders/today \
  -H "Authorization: Bearer eyJ..."
```

## Operación diaria

```bash
# Ver estado
docker ps | grep admin-tools-api
docker logs admin-tools-api-v2 --tail 50 -f    # en vivo

# Restart del proceso (NO relee .env-admintools)
docker compose restart admin-tools-api-v2

# Recrear container (SÍ relee .env-admintools y cambios al yml)
docker compose up -d --force-recreate admin-tools-api-v2

# Recrear con código nuevo (después de git pull)
docker compose up -d --build --force-recreate admin-tools-api-v2

# Apagar
docker compose down
```

## Persistencia automática

El yml tiene `restart: unless-stopped`. Eso significa:
- Si el container crashea → Docker lo levanta solo
- Si el server reboota → Docker arranca al boot, y el container con él
- Si vos hacés `docker stop` o `docker compose down` → NO se reinicia
  automáticamente (intencional, para rollback manual)

Para confirmar que Docker arranca al boot del server:
```bash
sudo systemctl is-enabled docker     # debe decir: enabled
```

## Backup del `.env-admintools`

Como NO está en git, conviene tenerlo backupeado fuera del repo:

```bash
sudo cp /home/ronal/admintools-api/.env-admintools \
        /root/.env-admintools-backup
sudo chmod 600 /root/.env-admintools-backup
```

Restaurar si hace falta:
```bash
sudo cp /root/.env-admintools-backup \
        /home/ronal/admintools-api/.env-admintools
sudo chown ronal:ronal /home/ronal/admintools-api/.env-admintools
sudo chmod 600 /home/ronal/admintools-api/.env-admintools
```

## Rollback

Si el deploy nuevo rompe algo y querés volver al estado anterior:

```bash
# 1. Apagar el v2
docker compose down

# 2. Si el admin-tools-api viejo todavía existe en otro compose
#    (ej. /home/ronal/n8n-docker/docker-compose.yml), sigue corriendo
#    en su port (8082) y atiende a usuarios.
```

## Desarrollo local (en tu Mac)

NO uses este `docker-compose.yml` directamente para dev — apunta a
producción y requiere `.env-admintools` con credenciales reales.

En cambio, creá un `docker-compose.override.yml` (ignorado por git) en
la raíz del repo con tu config local. Compose lo carga automáticamente
junto al `docker-compose.yml` y los valores del override ganan. Ejemplo:

```yaml
# docker-compose.override.yml (dev local, NO se commitea)
services:
  admin-tools-api-v2:
    env_file: []                  # ignorar .env-admintools de prod
    environment:
      SPRING_PROFILES_ACTIVE: dev
      SPRING_DATASOURCE_URL: jdbc:mysql://host.docker.internal:3306/PacRoc_backup?serverTimezone=GMT-6
      SPRING_DATASOURCE_USERNAME: root
      SPRING_DATASOURCE_PASSWORD: tu_password_local
    networks: !reset []           # no usar la red de prod
    ports:
      - "8082:8080"                # port distinto al de prod
```

Después `docker compose up -d --build` desde la raíz funciona contra
tu MySQL local sin tocar producción.
