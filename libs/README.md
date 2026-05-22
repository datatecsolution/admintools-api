# libs/

Artifacts publicados localmente que la API necesita compilar, pero que
no están en Maven Central / repos públicos. Se incluyen aquí en el repo
para que el build Docker pueda resolverlos sin depender de un Maven
externo o de la mavenLocal del desarrollador.

## admintools-core-0.1.0-SNAPSHOT.jar

Módulo de lógica compartida con el Swing legacy. Hoy contiene
`FacturacionCalculadora` (cálculo de descuento porcentual con redondeo
bit-idéntico al Swing). Más componentes se agregarán a medida que más
lógica se comparta entre Swing y API.

**Origen**: repo `https://github.com/datatecsolution/adminTools` (Swing),
sub-módulo `admintools-core` (Gradle), versión `0.1.0-SNAPSHOT`.

## Cómo regenerar (si el módulo cambia)

En la Mac del desarrollador, con ambos repos clonados:

```bash
# 1. Build + publish admintools-core a ~/.m2/repository
cd /path/to/adminTools                          # repo del Swing
./gradlew :admintools-core:publishToMavenLocal

# 2. Copiar el JAR + POM al repo del API
cp ~/.m2/repository/net/datatecsolution/admintools-core/0.1.0-SNAPSHOT/admintools-core-0.1.0-SNAPSHOT.jar \
   /path/to/admintools-api/libs/
cp ~/.m2/repository/net/datatecsolution/admintools-core/0.1.0-SNAPSHOT/admintools-core-0.1.0-SNAPSHOT.pom \
   /path/to/admintools-api/libs/

# 3. Commit + push del repo del API
cd /path/to/admintools-api
git add libs/
git commit -m "Regenerar admintools-core desde Swing master"
git push
```

## Cómo lo consume el build

- **Local (`./gradlew build` en Mac del dev)**: gradle resuelve la
  dependencia desde `mavenLocal()` (ver `build.gradle:repositories`).
- **Docker build**: el `Dockerfile` copia `libs/*.jar` y `libs/*.pom`
  a `/root/.m2/repository/...` ANTES de ejecutar `gradle build`. Así
  el build del API encuentra el artefacto en mavenLocal del container.

## Roadmap

Cuando publiquemos `admintools-core` a un Maven registry real (Maven
Central, GitHub Packages, Sonatype), borramos esta carpeta y volvemos
a depender de la URL del repo en `build.gradle`.
