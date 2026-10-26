# Gobernanza y calidad del TPO

Los ejercicios de las unidades 5 y 6 aplicados al propio trabajo: el registro de riesgos, la política de uso de IA y cómo medimos si el TP va bien. Proporcionalidad ante todo: es un TPO, no un sistema con datos de miles de personas, así que cada control tiene que costar menos que el riesgo que evita.

## 1. Registro de riesgos (Unidad 5, ISO 31000)

Probabilidad e impacto en tres niveles (B baja, M media, A alta). Rojo se atiende primero, amarillo se planifica, verde se acepta o se monitorea.

| # | Riesgo (activo, amenaza, vulnerabilidad) | P | I | Nivel | Tratamiento | Control | Dueño |
|---|---|---|---|---|---|---|---|
| R1 | Reglas del ajedrez mal implementadas (el núcleo) por un cambio que rompe otra regla sin que nadie lo note | M | A | Rojo | Mitigar | 65 tests del núcleo, `./scripts/test.sh` antes de cada push y revisión por PR | Todo el grupo |
| R2 | El juego no anda en la PC de un integrante (otro sistema operativo, otro JDK, puertos ocupados) el día de la exposición | M | A | Rojo | Mitigar | Recursos por classpath, `--release 21`, sin puertos fijos para la partida, puerto de avisos configurable, prueba en Windows y macOS antes del 20/11 | Leandro |
| R3 | Pérdida del código (una PC rota, un `push --force` sobre `main`) | B | A | Amarillo | Mitigar | Repositorio en GitHub, ramas por cambio y PR; no se fuerza `main` sin acuerdo del grupo | Franco |
| R4 | El UML deja de coincidir con el código (la cátedra lo evalúa) | A | M | Rojo | Mitigar | `docs/uml-checklist.md` se regenera del código con `java scripts/ListTypes.java` antes de cada entrega y el diagrama se revisa contra esa lista | Franco |
| R5 | Uso de recursos con derechos de terceros (imágenes, marcas) | M | M | Amarillo | Evitar | Solo recursos con licencia que lo permita, atribuidos en `CREDITS.md`; se sacaron las skins con marcas | Leandro |
| R6 | Un integrante no puede defender una parte del diseño en el final individual | M | A | Rojo | Mitigar | Justificación en formato qué/por qué/cuándo y `docs/normativa-catedra.md`; cada uno practica agregar una pieza y cambiar el tablero | Cada integrante |
| R7 | Código generado con IA que nadie del grupo entiende o que copia algo con licencia | M | M | Amarillo | Mitigar | Política de uso de IA (sección 2) | Todo el grupo |
| R8 | Alguien en la misma red manda datos falsos al puerto del juego por LAN | B | B | Verde | Aceptar, con controles baratos | Nombres limpios, líneas acotadas, jugadas validadas contra las legales; se acepta que por LAN no hay autenticación | Leandro |

Riesgo residual aceptado: por LAN cualquiera de la misma red puede anunciar una partida con cualquier nombre. Para un juego entre compañeros en una red de confianza no justifica agregar autenticación (sería un control que cuesta más que el riesgo).

## 2. Política de uso de IA en el TPO (Unidad 5)

**Objetivo.** Usar asistentes de IA para avanzar más rápido sin perder la autoría ni la comprensión del trabajo, que se defiende individualmente.

**Qué se permite.** Pedir explicaciones, revisar código, proponer pruebas, ayudar a redactar documentación y generar código que después se revisa.

**Qué datos no se comparten.** Datos personales de terceros, contraseñas o claves, y el contenido de `data/` (usuarios y puntajes). El código del TP y el material de la cátedra sí, porque son del grupo y del curso.

**Quién revisa.** Todo cambio entra por un PR que revisa otro integrante. Quien entrega código generado por IA responde por él: tiene que poder explicarlo, haberlo probado y saber por qué se eligió así.

**Cómo se declara.** Los commits hechos con ayuda de un asistente lo dicen (línea `Co-Authored-By`). La exposición y la justificación mencionan que se usó IA y para qué.

**Qué no se hace.** Tocar un test para que pase en vez de arreglar el código, entregar algo que nadie del grupo entiende, o incluir recursos con derechos de terceros que la IA haya sugerido.

**Revisión.** Se revisa antes de la entrega del 20/11. Responsable: el grupo.

## 3. Cómo medimos si el TP va bien (Unidad 6)

**Meta.** Entregar un ajedrez correcto y mantenible, que se pueda defender y extender en el momento.

**Preguntas.** ¿Las reglas están bien? ¿El diseño aguanta una extensión no programada? ¿El UML coincide con el código? ¿Anda en las PCs del grupo?

| KPI | Fórmula | Dato | Quién mide | Umbral |
|---|---|---|---|---|
| Tests en verde | pasados / total, en `./scripts/test.sh` y `SwingIntegrationTests` | Salida de los scripts | Quien abre el PR | 100 % para mergear |
| Costo de una extensión | clases existentes modificadas para agregar una pieza nueva o cambiar el tamaño del tablero | Diff del cambio de prueba | El grupo, antes de la entrega | 0 clases del núcleo |
| Fidelidad del UML | tipos y relaciones del diagrama que no están en `uml-checklist.md` (y al revés) | Comparación diagrama contra lista | Franco | 0 diferencias |

**KRI (alerta temprana).** Días con un PR abierto sin revisar o con un test fallando en `main`. Si pasa de 2 días, se frena lo nuevo y se arregla eso primero.

**Ley de Goodhart y contramedidas.**

- *Tests en verde* se puede "trampear" borrando o debilitando tests. Contramedida: el revisor del PR mira el diff de los tests y la cantidad no puede bajar sin una razón escrita.
- *Costo de una extensión* se puede trampear preparando justo la extensión esperada. Contramedida: el que prueba elige la extensión en el momento (otra pieza, otro tamaño, otra regla de tablas), como en la defensa.
- *Fidelidad del UML* se puede trampear dibujando menos cosas. Contramedida: la lista sale del código, así que lo que falta en el diagrama se ve.

## 4. Integridad del código (Unidad 6)

Proporcional a un TPO:

- Control de versiones con historial, ramas por cambio y revisión por PR.
- Sin dependencias externas: el juego usa solo la biblioteca estándar de Java, así que no hay librerías de terceros que fijar ni escanear.
- Compilación reproducible: `./scripts/build.sh` compila con `--release 21` desde cero (borra `out/` antes).
- Lo que viene de afuera (red, archivos de datos) se valida antes de usarlo.
- Las simplificaciones conocidas están declaradas en la sección 19 de `docs/justificacion.md`, no escondidas.
