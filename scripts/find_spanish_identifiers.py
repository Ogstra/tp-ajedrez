#!/usr/bin/env python3
"""Lista los identificadores en español de los .java de src/.

Ignora comentarios, textos entre comillas y las líneas package/import.
Un identificador se marca si alguna de sus palabras (separadas por camelCase
o guion bajo) está en la lista SPANISH. Sale con código 1 si encuentra alguno.
"""
import pathlib
import re
import sys

SPANISH = {
    "fila", "filas", "columna", "columnas", "casilla", "casillas", "pieza", "piezas",
    "tablero", "jugada", "jugadas", "turno", "destino", "destinos", "origen", "enroque",
    "coronacion", "peon", "caballo", "torre", "alfil", "reina", "rey", "paso", "pasos",
    "salto", "saltos", "estado", "seleccion", "tamano", "clara", "oscura", "claro",
    "oscuro", "nombre", "nombres", "simbolo", "simbolos", "letra", "letras", "inicial",
    "captura", "entrada", "movimiento", "movimientos", "parsear", "armar", "aplicar",
    "como", "hay", "deshacer", "blanca", "blancas", "blanco", "negra", "negras", "negro",
    "jaque", "mate", "ahogado", "oponente", "tiene", "legales", "candidatos", "simulado",
    "limpio", "corte", "boton", "imagen", "imagenes", "clave", "mapa", "carpeta",
    "opciones", "eleccion", "abajo", "cargar", "definir", "direcciones", "deslizante",
    "dos", "interfaz", "casilleros", "elegida", "vacia", "vacio", "propia",
    "atacada", "atacado", "movido", "tocada", "tocado", "derecho", "algebraico",
    "texto", "linea", "lineas", "ventana", "partida", "juego", "regla", "reglas",
    "resultado", "intento", "hacia", "comando", "ultima", "ultimo", "antes", "despues",
    "jugador", "pasar", "saltar", "estaba", "fueron", "cada",
}
# Mate es un termino ingles de ajedrez en estos nombres de test.
ALLOWED = {"MATE", "testMinimaxOpponentFindsMateInOne", "testCheckIsDetectedAndIsNotMate", "testFoolsMate"}

STRING = re.compile(r'"(?:\\.|[^"\\])*"')
CHAR = re.compile(r"'(?:\\.|[^'\\])'")
BLOCK_COMMENT = re.compile(r"/\*.*?\*/", re.DOTALL)
LINE_COMMENT = re.compile(r"//.*")
IDENT = re.compile(r"[A-Za-z_][A-Za-z0-9_]*")
WORDS = re.compile(r"[A-Z]?[a-z0-9]+|[A-Z]+(?![a-z])")


def clean(text):
    text = STRING.sub('""', text)
    text = CHAR.sub("''", text)
    text = BLOCK_COMMENT.sub(lambda m: "\n" * m.group(0).count("\n"), text)
    return LINE_COMMENT.sub("", text)


def main():
    found = 0
    for path in sorted(pathlib.Path("src").rglob("*.java")):
        text = clean(path.read_text(encoding="utf-8"))
        for number, line in enumerate(text.splitlines(), 1):
            stripped = line.strip()
            if stripped.startswith("package ") or stripped.startswith("import "):
                continue
            for identifier in IDENT.findall(line):
                if identifier in ALLOWED:
                    continue
                for word in WORDS.findall(identifier):
                    if word.lower() in SPANISH:
                        print(f"{path}:{number}: {identifier} (palabra: {word.lower()})")
                        found += 1
                        break
    print(f"{found} identificadores en español")
    sys.exit(1 if found else 0)


main()
