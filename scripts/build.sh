#!/usr/bin/env bash
# Compila todo src/ en out/ y copia las imágenes (src/assets) al lado de las clases, porque el juego
# las lee del classpath. Se corre desde cualquier carpeta. --release 21 compila contra la API de
# Java 21 aunque el JDK instalado sea más nuevo: así nadie del grupo usa sin querer algo que no
# exista en el JDK de otro.
set -e
cd "$(dirname "$0")/.."
rm -rf out
find src -name "*.java" -print0 | xargs -0 javac --release 21 -d out -encoding UTF-8
cp -R src/assets out/assets
echo "compilado en out/"
