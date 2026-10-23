#!/usr/bin/env bash
# Pruebas de la consola con entrada simulada: deshacer y repetir un mate, y las cuentas
# (registro, login y ranking guardado entre ejecuciones, en una carpeta temporal).
set -e
cd "$(dirname "$0")/.."

data=$(mktemp -d)
trap 'rm -rf "$data"' EXIT

# 1. Sin usuarios (3 = sin usuario, dos veces): se puede deshacer y repetir el mate.
output=$(printf '0\n3\n3\nf2 f3\ne7 e5\ng2 g4\nd8 h4\nundo\nd8 h4\nsalir\n' | java -Dajedrez.data="$data/vacio" -cp out Main)
if [ "$(printf '%s' "$output" | grep -c 'Jaque mate!')" -ne 2 ] || ! printf '%s' "$output" | grep -q 'Se deshizo el'; then
    echo '[FAIL] La consola debe permitir deshacer y repetir el mate.'
    exit 1
fi
echo '[OK]   Consola: deshacer y repetir mate'

# 2. Dos usuarios nuevos juegan el mate del loco: ganan las negras (Beto).
output=$(printf '0\n2\nAna\n2\nBeto\nf2 f3\ne7 e5\ng2 g4\nd8 h4\nsalir\n' | java -Dajedrez.data="$data/cuentas" -cp out Main)
if ! printf '%s' "$output" | grep -q 'Tu id es 1' || ! printf '%s' "$output" | grep -q 'Tu id es 2'; then
    echo '[FAIL] El sistema debe asignar los ids 1 y 2 al registrarse.'
    exit 1
fi
if ! printf '%s' "$output" | grep -Eq '^ *1  Beto +2 +1 +1 +0 +0 +3$'; then
    echo '[FAIL] Después del mate, Beto debe liderar el ranking con 3 puntos.'
    exit 1
fi
echo '[OK]   Consola: registro con id y ranking al terminar la partida'

# 3. Otra ejecución: el login usa usuario e id, un id equivocado se rechaza y el ranking persiste.
output=$(printf '0\n1\nAna\n9\n1\nAna\n1\n1\nbeto\n2\nranking\nsalir\n' | java -Dajedrez.data="$data/cuentas" -cp out Main)
if ! printf '%s' "$output" | grep -q 'Usuario o id incorrectos' || ! printf '%s' "$output" | grep -q 'Hola, Ana' || ! printf '%s' "$output" | grep -q 'Hola, Beto'; then
    echo '[FAIL] El login debe rechazar un id equivocado y aceptar usuario e id correctos.'
    exit 1
fi
if ! printf '%s' "$output" | grep -Eq '^ *1  Beto +2 +1 +1 +0 +0 +3$' || ! printf '%s' "$output" | grep -Eq '^ *2  Ana +1 +1 +0 +0 +1 +0$'; then
    echo '[FAIL] El ranking guardado debe seguir ahí en la siguiente ejecución.'
    exit 1
fi
echo '[OK]   Consola: login con usuario e id y ranking persistente'
