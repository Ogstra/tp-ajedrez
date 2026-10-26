#!/usr/bin/env bash
# Pruebas de la consola con entrada simulada. Primero va el login y después el modo: deshacer y
# repetir un mate en 1 contra 1 (sin puntos) y las cuentas contra la IA (registro, login por
# nombre y ranking guardado entre ejecuciones, en una carpeta temporal).
set -e
# La salida de Java se fuerza a UTF-8 para que grep encuentre los acentos (en Windows sale en Cp1252).
cd "$(dirname "$0")/.."

data=$(mktemp -d)
trap 'rm -rf "$data"' EXIT

# 1. Invitado en 1 contra 1: se puede deshacer y repetir el mate y no se crea el archivo de cuentas.
output=$(printf '3\n0\nf2 f3\ne7 e5\ng2 g4\nd8 h4\nundo\nd8 h4\nsalir\n' | java -Dstdout.encoding=UTF-8 -Dajedrez.data="$data/vacio" -cp out Main)
if [ "$(printf '%s' "$output" | grep -c 'Jaque mate!')" -ne 2 ] || ! printf '%s' "$output" | grep -q 'Se deshizo el'; then
    echo '[FAIL] La consola debe permitir deshacer y repetir el mate.'
    exit 1
fi
if ! printf '%s' "$output" | grep -q '1 contra 1' || [ -e "$data/vacio/usuarios.csv" ]; then
    echo '[FAIL] Como invitado en 1 contra 1 no se deben guardar cuentas.'
    exit 1
fi
echo '[OK]   Consola: invitado en 1 contra 1, deshacer y repetir mate'

# 2. Ana se registra en el login y juega contra la IA fácil: queda en el ranking sin partidas.
output=$(printf '2\nAna\n1\nranking\nsalir\n' | java -Dstdout.encoding=UTF-8 -Dajedrez.data="$data/cuentas" -cp out Main)
if [ "$(printf '%s' "$output" | grep -c 'Ya estás registrado')" -ne 1 ] || [ "$(printf '%s' "$output" | grep -c 'iniciar sesión')" -ne 1 ]; then
    echo '[FAIL] El login se debe pedir una sola vez y registrar a Ana.'
    exit 1
fi
if ! printf '%s' "$output" | grep -q 'Blancas: Ana | Negras: la IA' || ! printf '%s' "$output" | grep -Eq '^ *1  Ana +0 +0 +0 +0 +0$'; then
    echo '[FAIL] Ana debe jugar con blancas contra la IA y figurar en el ranking.'
    exit 1
fi
echo '[OK]   Consola: registro y partida contra la IA'

# 3. Ana entra y juega 1 contra 1: el mate no suma nada al ranking.
output=$(printf '1\nAna\n0\nf2 f3\ne7 e5\ng2 g4\nd8 h4\nranking\nsalir\n' | java -Dstdout.encoding=UTF-8 -Dajedrez.data="$data/cuentas" -cp out Main)
if ! printf '%s' "$output" | grep -q 'Jaque mate!'; then
    echo '[FAIL] El 1 contra 1 debe llegar al mate.'
    exit 1
fi
if ! printf '%s' "$output" | grep -Eq '^ *1  Ana +0 +0 +0 +0 +0$'; then
    echo '[FAIL] El 1 contra 1 no debe sumar partidas al ranking.'
    exit 1
fi
echo '[OK]   Consola: el 1 contra 1 no suma al ranking'

# 4. Otra ejecución contra la IA: el login es solo por nombre, un usuario inexistente se rechaza y el ranking persiste.
output=$(printf '1\nNadie\n1\nana\n2\nranking\nsalir\n' | java -Dstdout.encoding=UTF-8 -Dajedrez.data="$data/cuentas" -cp out Main)
if ! printf '%s' "$output" | grep -q 'No existe ese usuario' || ! printf '%s' "$output" | grep -q 'Hola, Ana'; then
    echo '[FAIL] El login debe rechazar un usuario inexistente y aceptar los registrados.'
    exit 1
fi
if ! printf '%s' "$output" | grep -Eq '^ *1  Ana +0 +0 +0 +0 +0$'; then
    echo '[FAIL] El ranking guardado debe seguir ahí en la siguiente ejecución.'
    exit 1
fi
echo '[OK]   Consola: login por nombre y ranking persistente'
