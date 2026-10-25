#!/usr/bin/env bash
# Pruebas de la consola con entrada simulada: deshacer y repetir un mate, y las cuentas
# (registro, login por nombre y ranking guardado entre ejecuciones, en una carpeta temporal).
set -e
# La salida de Java se fuerza a UTF-8 para que grep encuentre los acentos (en Windows sale en Cp1252).
cd "$(dirname "$0")/.."

data=$(mktemp -d)
trap 'rm -rf "$data"' EXIT

# 1. Sin usuarios (3 = sin usuario, dos veces): se puede deshacer y repetir el mate.
output=$(printf '0\n3\n3\nf2 f3\ne7 e5\ng2 g4\nd8 h4\nundo\nd8 h4\nsalir\n' | java -Dstdout.encoding=UTF-8 -Dajedrez.data="$data/vacio" -cp out Main)
if [ "$(printf '%s' "$output" | grep -c 'Jaque mate!')" -ne 2 ] || ! printf '%s' "$output" | grep -q 'Se deshizo el'; then
    echo '[FAIL] La consola debe permitir deshacer y repetir el mate.'
    exit 1
fi
echo '[OK]   Consola: deshacer y repetir mate'

# 2. Dos usuarios nuevos juegan el mate del loco: ganan las negras (Beto).
output=$(printf '0\n2\nAna\n2\nBeto\nf2 f3\ne7 e5\ng2 g4\nd8 h4\nsalir\n' | java -Dstdout.encoding=UTF-8 -Dajedrez.data="$data/cuentas" -cp out Main)
if [ "$(printf '%s' "$output" | grep -c 'Ya estás registrado')" -ne 2 ]; then
    echo '[FAIL] Los dos jugadores deben poder registrarse solo con el nombre.'
    exit 1
fi
if ! printf '%s' "$output" | grep -Eq '^ *1  Beto +1 +1 +0 +0 +3$'; then
    echo '[FAIL] Después del mate, Beto debe liderar el ranking con 3 puntos.'
    exit 1
fi
echo '[OK]   Consola: registro y ranking al terminar la partida'

# 3. Otra ejecución: el login es solo por nombre, un usuario inexistente se rechaza y el ranking persiste.
output=$(printf '0\n1\nNadie\n1\nAna\n1\nbeto\nranking\nsalir\n' | java -Dstdout.encoding=UTF-8 -Dajedrez.data="$data/cuentas" -cp out Main)
if ! printf '%s' "$output" | grep -q 'No existe ese usuario' || ! printf '%s' "$output" | grep -q 'Hola, Ana' || ! printf '%s' "$output" | grep -q 'Hola, Beto'; then
    echo '[FAIL] El login debe rechazar un usuario inexistente y aceptar los registrados.'
    exit 1
fi
if ! printf '%s' "$output" | grep -Eq '^ *1  Beto +1 +1 +0 +0 +3$' || ! printf '%s' "$output" | grep -Eq '^ *2  Ana +1 +0 +0 +1 +0$'; then
    echo '[FAIL] El ranking guardado debe seguir ahí en la siguiente ejecución.'
    exit 1
fi
echo '[OK]   Consola: login por nombre y ranking persistente'
