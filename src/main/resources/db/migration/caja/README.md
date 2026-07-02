# Espejo de las migraciones de caja del Swing (US-101)

Estos archivos (y las Java migrations en `src/main/java/db/migration/caja/`) son una
**copia exacta** de los del repo Swing `adminTools`:

- SQL:  `adminTools/src/main/resources/db/migration/caja/`
- Java: `adminTools/src/main/java/db/migration/caja/`

La API los usa **únicamente** al provisionar la BD de una caja nueva
(`POST /cajas` → `CajaProvisioner`, réplica de `SchemaMigrator.migrateNewCajaDatabase`).
El dueño de las migraciones en runtime normal sigue siendo el Swing
(`repair()+migrate()` en cada arranque), por eso cualquier diferencia de checksum
que se introdujera aquí la repara el Swing.

## REGLA de sincronización

Toda migración de caja NUEVA que se agregue en el Swing (V9+ — p. ej. los lotes
float→DECIMAL de US-070..073) **debe copiarse a este espejo** en el mismo PR/sprint.
Si el espejo queda atrás, las cajas provisionadas por la API nacen en la última
versión que la API conozca y el Swing les aplica el resto en su siguiente arranque
— funciona, pero con los placeholders por defecto de `migrateAll()` (bodega=1), no
con los de la caja. Mantener el espejo al día evita esa asimetría.
