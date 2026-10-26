#!/usr/bin/env bash
# Compila y corre los tests del núcleo. Sale con código distinto de cero si falla alguno.
set -e
cd "$(dirname "$0")/.."
./scripts/build.sh
java -cp out ChessCoreTests
java -cp out AccountTests
java -cp out SkinTests
./scripts/test-console.sh
