#!/usr/bin/env bash
# Pruebas de la consola con entrada simulada: deshacer y repetir un mate en 1 contra 1 (sin
# usuarios ni puntos) y las cuentas contra la IA (registro, login por nombre y ranking guardado
# entre ejecuciones, en una carpeta temporal).
set -e
# La salida de Java se fuerza a UTF-8 para que grep encuentre los acentos (en Windows sale en Cp1252).
cd "$(dirname "$0")/.."

data=$(mktemp -d)
trap 'rm -rf "$data"' EXIT

# 1. 1 contra 1: no se piden usuarios, se puede deshacer y repetir el mate y no se crea el archivo de cuentas.
output=$(printf '0\nf2 f3\ne7 e5\ng2 g4\nd8 h4\nundo\nd8 h4\nsalir\n' | java -Dstdout.encoding=UTF-8 -Dajedrez.data="$data/vacio" -cp out Main)
if [ "$(printf '%s' "$output" | grep -c 'Jaque mate!')" -ne 2 ] || ! printf '%s' "$output" | grep -q 'Se deshizo el'; then
    echo '[FAIL] La consola debe permitir deshacer y repetir el mate.'
    exit 1
fi
if printf '%s' "$output" | grep -q 'iniciar sesión' || ! printf '%s' "$output" | grep -q '1 contra 1: la partida no suma al ranking.' || [ -e "$data/vacio/usuarios.csv" ]; then
    echo '[FAIL] En 1 contra 1 no se deben pedir usuarios ni guardar cuentas.'
    exit 1
fi
echo '[OK]   Consola: 1 contra 1 sin usuarios, deshacer y repetir mate'

# 2. Contra la IA fácil: solo se pregunta por la persona (Ana), que queda en el ranking sin partidas.
output=$(printf '1\n2\nAna\nranking\nsalir\n' | java -Dstdout.encoding=UTF-8 -Dajedrez.data="$data/cuentas" -cp out Main)
if [ "$(printf '%s' "$output" | grep -c 'Ya estás registrado')" -ne 1 ] || [ "$(printf '%s' "$output" | grep -c 'iniciar sesión')" -ne 1 ]; then
    echo '[FAIL] Contra la IA solo se debe preguntar por el usuario de la persona.'
    exit 1
fi
if ! printf '%s' "$output" | grep -q 'Blancas: Ana | Negras: la IA' || ! printf '%s' "$output" | grep -Eq '^ *1  Ana +0 +0 +0 +0 +0$'; then
    echo '[FAIL] Ana debe jugar con blancas contra la IA y figurar en el ranking.'
    exit 1
fi
echo '[OK]   Consola: registro contra la IA'

# 3. 1 contra 1 con Ana ya registrada: el mate no suma nada al ranking.
output=$(printf '0\nf2 f3\ne7 e5\ng2 g4\nd8 h4\nranking\nsalir\n' | java -Dstdout.encoding=UTF-8 -Dajedrez.data="$data/cuentas" -cp out Main)
if ! printf '%s' "$output" | grep -q 'Jaque mate!' || printf '%s' "$output" | grep -q 'iniciar sesión'; then
    echo '[FAIL] El 1 contra 1 debe llegar al mate sin pedir usuarios.'
    exit 1
fi
if ! printf '%s' "$output" | grep -Eq '^ *1  Ana +0 +0 +0 +0 +0$'; then
    echo '[FAIL] El 1 contra 1 no debe sumar partidas al ranking.'
    exit 1
fi
echo '[OK]   Consola: el 1 contra 1 no suma al ranking'

# 4. Otra ejecución contra la IA: el login es solo por nombre, un usuario inexistente se rechaza y el ranking persiste.
output=$(printf '2\n1\nNadie\n1\nana\nranking\nsalir\n' | java -Dstdout.encoding=UTF-8 -Dajedrez.data="$data/cuentas" -cp out Main)
if ! printf '%s' "$output" | grep -q 'No existe ese usuario' || ! printf '%s' "$output" | grep -q 'Hola, Ana'; then
    echo '[FAIL] El login debe rechazar un usuario inexistente y aceptar los registrados.'
    exit 1
fi
if ! printf '%s' "$output" | grep -Eq '^ *1  Ana +0 +0 +0 +0 +0$'; then
    echo '[FAIL] El ranking guardado debe seguir ahí en la siguiente ejecución.'
    exit 1
fi
echo '[OK]   Consola: login por nombre y ranking persistente'
