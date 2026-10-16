#!/usr/bin/env bash
# Verifica que la consola permite deshacer y repetir un mate.
set -e
cd "$(dirname "$0")/.."
output=$(printf 'f2 f3\ne7 e5\ng2 g4\nd8 h4\nundo\nd8 h4\nsalir\n' | java -cp out Main)
if [ "$(printf '%s' "$output" | grep -c 'Jaque mate!')" -ne 2 ] || ! printf '%s' "$output" | grep -q 'Se deshizo el'; then
    echo '[FAIL] La consola debe permitir deshacer y repetir el mate.'
    exit 1
fi
echo '[OK]   Consola: deshacer y repetir mate'
