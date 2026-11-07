#!/usr/bin/env bash
# Roda o teste de carga inteiro: prepara os restaurantes, liga a sonda do tempo real e o k6.
# Pré-requisitos: o back no ar em localhost:8080, com RATE_LIMIT_ENABLED=false (toda a carga sai
# de um IP só), o Docker e o Node 22. Ver load/README.md.
set -euo pipefail
cd "$(dirname "$0")"

node preparar.mjs
# A sonda dura o teste inteiro (2 min + pausa + 2 min) e um pouco mais.
node sonda.mjs 285 > resultado-sonda.txt 2>&1 &
SONDA=$!
sleep 8
docker run --rm --network host -v "$PWD":/load -w /load grafana/k6:1.0.0 run carga.js | tee resultado-k6.txt
# Espera o resumo da sonda (ela conta o tempo dela a partir de quando abriu todas as conexões).
until grep -q "avisos que não chegaram" resultado-sonda.txt 2>/dev/null; do sleep 2; done
cat resultado-sonda.txt
