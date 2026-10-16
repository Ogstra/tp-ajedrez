#!/usr/bin/env python3
"""Uso: python3 scripts/rename_identifier.py VIEJO NUEVO

Reemplaza VIEJO por NUEVO como palabra completa en todos los .java de src/,
excepto en las líneas package e import. No toca textos entre comillas ni
comentarios: usa el reemplazo solo en código.
"""
import pathlib
import re
import sys

STRING = re.compile(r'"(?:\\.|[^"\\])*"')

if len(sys.argv) != 3:
    sys.exit("uso: rename_identifier.py VIEJO NUEVO")
old, new = sys.argv[1], sys.argv[2]
pattern = re.compile(r"\b" + re.escape(old) + r"\b")
total = 0

for path in pathlib.Path("src").rglob("*.java"):
    lines = path.read_text(encoding="utf-8").split("\n")
    changed = False
    for index, line in enumerate(lines):
        stripped = line.strip()
        if stripped.startswith("package ") or stripped.startswith("import "):
            continue
        code, _, comment = line.partition("//")
        # separa los textos entre comillas para no tocarlos
        parts = STRING.split(code)
        strings = STRING.findall(code)
        replaced = [pattern.sub(new, part) for part in parts]
        rebuilt = replaced[0]
        for i, s in enumerate(strings):
            rebuilt += s + replaced[i + 1]
        if rebuilt != code:
            lines[index] = rebuilt + (("//" + comment) if comment or "//" in line else "")
            total += 1
            changed = True
    if changed:
        path.write_text("\n".join(lines), encoding="utf-8")
print(f"{total} líneas cambiadas")
