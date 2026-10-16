# Fase 0: base del agente

> **Para el agente:** REQUIRED SUB-SKILL: `superpowers:subagent-driven-development` (recomendado) o `superpowers:executing-plans`. Pasos con casillas `- [ ]`.

**Goal:** tener scripts para compilar y correr los tests con código de salida correcto, y un `AGENTS.md` con las reglas del trabajo.

**Architecture:** solo archivos de apoyo. No se toca el código de producción.

**Tech Stack:** bash, Java.

---

### Task 0.1: scripts de compilación y de tests

**Files:**
- Create: `scripts/build.sh`
- Create: `scripts/test.sh`
- Modify: `src/ChessCoreTests.java` (el final de `main`)

- [ ] **Step 1: Crear `scripts/build.sh`**

```bash
#!/usr/bin/env bash
# Compila todo src/ en out/. Se corre desde cualquier carpeta.
set -e
cd "$(dirname "$0")/.."
rm -rf out
find src -name "*.java" -print0 | xargs -0 javac -d out -encoding UTF-8
echo "compilado en out/"
```

- [ ] **Step 2: Crear `scripts/test.sh`**

```bash
#!/usr/bin/env bash
# Compila y corre los tests del núcleo. Sale con código distinto de cero si falla alguno.
set -e
cd "$(dirname "$0")/.."
./scripts/build.sh
java -cp out ChessCoreTests
```

- [ ] **Step 3: Darles permiso de ejecución**

Run: `chmod +x scripts/build.sh scripts/test.sh`

- [ ] **Step 4: Hacer que `ChessCoreTests` termine con código de salida**

En `src/ChessCoreTests.java`, la última línea de `main` es:

```java
        System.out.println("\n" + pasados + " pasados, " + fallados + " fallados.");
```

Dejala así:

```java
        System.out.println("\n" + pasados + " pasados, " + fallados + " fallados.");
        System.exit(fallados > 0 ? 1 : 0);
```

- [ ] **Step 5: Verificar que pasa y devuelve 0**

Run: `./scripts/test.sh; echo "codigo=$?"`
Expected: 20 líneas `[OK]`, luego `20 pasados, 0 fallados.` y `codigo=0`.

- [ ] **Step 6: Verificar que un fallo devuelve distinto de cero**

Cambiá temporalmente en algún test la condición de un `assertTrue` por `false`, corré `./scripts/test.sh; echo "codigo=$?"` y comprobá `codigo=1`. Deshacé el cambio con `git checkout src/ChessCoreTests.java` y volvé a aplicar el Step 4.

- [ ] **Step 7: Commit**

```bash
git add scripts/build.sh scripts/test.sh src/ChessCoreTests.java
git commit -m "scripts para compilar y correr los tests"
```

---

### Task 0.2: AGENTS.md

**Files:**
- Create: `AGENTS.md`

- [ ] **Step 1: Crear `AGENTS.md` en la raíz del repo con este contenido exacto**

```markdown
# Reglas para quien trabaje en este repo

Este es el TPO de Ingeniería de Software (UADE): un ajedrez en Java con núcleo y adaptadores. Franco es el dueño del trabajo y lo tiene que defender oralmente, así que todo tiene que ser simple y explicable con lo que se vio en la materia.

## Antes de empezar

1. Si no tenés instalado el plugin superpowers, instalalo (fuente: https://github.com/obra/superpowers):
   - Claude Code: `/plugin install superpowers@claude-plugins-official`
   - Codex: "Fetch and follow instructions from https://raw.githubusercontent.com/obra/superpowers/refs/heads/main/.codex/INSTALL.md"
   - Cursor: desde el marketplace de plugins.
   - OpenCode: instrucciones en `.opencode/INSTALL.md` del repo de superpowers.
   - Gemini CLI: `gemini extensions install https://github.com/obra/superpowers`
   Verificá la instalación con la sección "Verify Installation" de su README.
2. Leé `README.md`, `docs/superpowers/specs/2026-10-15-cierre-del-tp-design.md` y `docs/superpowers/plans/00-indice.md`, y seguí el plan fase por fase.

## Cómo se trabaja

- Compilar: `./scripts/build.sh`. Tests: `./scripts/test.sh` (sale con código distinto de cero si algo falla). Los programas se corren desde la raíz del repo.
- Cada cambio deja todo compilando y con los tests en verde antes de commitear.
- Los tests nuevos se escriben antes que el código (TDD) y se marcan con un comentario `// TDD:` en la línea de arriba del método.
- Skills de superpowers a usar: `subagent-driven-development` o `executing-plans`, `test-driven-development`, `systematic-debugging`, `verification-before-completion` y `requesting-code-review` al cerrar cada fase.

## Reglas de estilo (no negociables)

- Commits cortos, en español, escritos como los escribiría una persona. Sin `Co-Authored-By`, sin "generated with", sin emojis, sin prefijos como `feat:`.
- Nada de guiones largos (el carácter raya) en código, comentarios, README ni documentos.
- Comentarios en español, identificadores en inglés, textos para el usuario en español. Los paquetes `nucleo`, `puertos`, `adaptadores`, `especiales` y `estrategias` se quedan con esos nombres (vocabulario de la cátedra).
- En `nucleo/` solo se usa lo visto en la materia: sin streams, `Optional`, threads, reflection ni librerías externas. Patrones permitidos: Strategy, Observer, Factory, State y Command.
- `nucleo/` no importa nada de `adaptadores/` ni de `ai/`. Las dependencias apuntan hacia adentro.
- Si algo no está claro o contradice la spec, preguntale a Franco antes de inventar.
- Los documentos de la entrega (justificación y presentación) llevan formato académico sobrio: Times New Roman o Calibri en negro, sin colores ni adornos.

## Lo que NO hay que hacer

- No agregar JUnit, Maven ni Gradle.
- No dibujar el diagrama UML (lo hace Franco con la lista de `docs/uml-checklist.md`).
- No pushear a otra rama que no sea `main` ni reescribir el historial.
```

- [ ] **Step 2: Verificar que no tiene el carácter raya**

Run: `grep -c $'\xe2\x80\x94' AGENTS.md`
Expected: `0`

- [ ] **Step 3: Commit**

```bash
git add AGENTS.md
git commit -m "reglas de trabajo para el agente"
```

---

### Cierre de la fase 0

- [ ] Correr `./scripts/test.sh` y confirmar `20 pasados, 0 fallados.`
- [ ] `git push`
