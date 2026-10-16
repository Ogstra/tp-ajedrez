#!/usr/bin/env bash
# Compila todo src/ en out/. Se corre desde cualquier carpeta.
set -e
cd "$(dirname "$0")/.."
rm -rf out
find src -name "*.java" -print0 | xargs -0 javac -d out -encoding UTF-8
echo "compilado en out/"
