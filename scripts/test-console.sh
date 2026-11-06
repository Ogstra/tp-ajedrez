#!/usr/bin/env bash
# Prueba de la consola con entrada simulada: dos jugadores (0), se juega el mate del loco, se deshace y se repite.
set -e
cd "$(dirname "$0")/.."

# La salida de Java se fuerza a UTF-8 para que grep encuentre los acentos (en Windows sale en Cp1252).
output=$(printf '0\nf2 f3\ne7 e5\ng2 g4\nd8 h4\nundo\nd8 h4\nsalir\n' | java -Dstdout.encoding=UTF-8 -cp out Main)
if [ "$(printf '%s' "$output" | grep -c 'Jaque mate!')" -ne 2 ] || ! printf '%s' "$output" | grep -q 'Se deshizo el'; then
    echo '[FAIL] La consola debe permitir deshacer y repetir el mate.'
    exit 1
fi
echo '[OK]   Consola: deshacer y repetir mate'
