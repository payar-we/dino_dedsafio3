# Quiu (Dedsafio) – mod Fabric 1.21.1

Agrega al mod de dinosaurios (`dinodedsafio`) el mundo y la entrada del **Centro de Quiu** de customgear.

## Compilar en GitHub (sin instalar nada)
1. Crea un repositorio nuevo y sube **todo** el contenido de esta carpeta (incluida la carpeta oculta `.github`).
2. Pestaña **Actions** → workflow **build** → espera el check verde.
3. Entra a la ejecución y baja el artefacto **quiu-jar** (dentro está `quiu-1.0.0.jar`).

## Instalar
En la carpeta `mods`: **Fabric API** + `dinodedsafio-1_0_0.jar` (el original) + `quiu-1.0.0.jar`.
Usa un mundo nuevo (o chunks nuevos en la dimensión de dinos): los chunks ya generados conservan el terreno viejo.

## Qué trae
- **Bloques de Quiu** con sus texturas: Tronco de Quiu, Tronco Primitivo, Pizarra / Pizarra Labrada / Toba / Basalto / Losas Agrietadas del Hoyo, Grieta Cerrada (inrompible), Oscuridad de Quiu (portal), Cuerda de Quiu (se trepa) y Mineral de Bioxita (+ ítem Bioxita).
- **El mundo**: la dimensión de los dinos (`dinodedsafio:dino_world`) ahora usa el terreno, los biomas, el césped normal con colores de Quiu, los árboles, los ruidos y el techo de customgear. Los dinos siguen apareciendo igual.
- **Minas gigantes** con el mineral dentro (ámbar y bioxita), no arriba: cuevas enormes entre y -56 y 70, sin agua.
- **Se pica todo con herramientas normales**: pico (minerales y piedras del hoyo), hacha (troncos), pala (tierra/césped), azadón (hojas). Ámbar: pico de piedra o mejor. Bioxita: pico de diamante o mejor.

## Cómo llegar (igual que customgear)
- A **2750 bloques del spawn** (en dirección aleatoria según la semilla) se construye solo el **Hoyo de Quiu**: pozo con rampa en espiral y la Grieta sellada arriba.
- `/quiu info` dice las coordenadas. `/quiu ir` te lleva ahí. `/quiu abrir` quita el sello (en customgear lo abría un Creeper Nuclear).
- Caes al fondo (y -58): la **Oscuridad de Quiu** te manda al Centro de Quiu.
- Allá, a 3 bloques del punto de llegada hay una **cuerda gigante** que sube al techo; la Oscuridad del techo te devuelve a la superficie, junto al hoyo.
- `/quiu entrar` hace el mismo viaje por comando.

## Ajustes rápidos
- Tamaño de las minas: `data/quiu/worldgen/configured_carver/*.json` (`horizontal_radius_multiplier`, `probability`).
- Cantidad de mineral: `data/quiu/worldgen/placed_feature/ore_*.json` (`count`, `size`, rango de `y`).
- Distancia / profundidad del hoyo: constantes arriba de `HoyoQuiu.java`.

## Nota
Si el build de GitHub falla, copia el error del log (paso "Run gradle build") y se corrige.
