#!/usr/bin/env bash
set -euo pipefail

APP="${APP:-http://localhost:8086}"

titulo() {
  printf '\n\033[1;36m== %s ==\033[0m\n' "$*"
}

passo() {
  printf '\033[1;33m-> %s\033[0m\n' "$*"
}

abrir() {
  local cliente="$1" valor="$2" conta="${3:-conta-1}" prazo="${4:-PT2M}"
  curl -s -X POST "$APP/api/propostas" -H 'Content-Type: application/json' \
    -d "{\"cliente\":\"$cliente\",\"valor\":$valor,\"parcelas\":12,\"contaDestino\":\"$conta\",\"prazoAssinatura\":\"$prazo\"}" | jq -r .propostaId
}

assinar() {
  for _ in $(seq 20); do
    codigo=$(curl -s -o /dev/null -w '%{http_code}' -X POST "$APP/api/propostas/$1/assinatura")
    [ "$codigo" = "202" ] && echo "assinatura correlacionada" && return
    sleep 0.5
  done
  echo "ninguém esperando a assinatura de $1"
}

revisar() {
  local id="$1" aprovado="$2"
  for _ in $(seq 30); do
    codigo=$(curl -s -o /dev/null -w '%{http_code}' -X POST "$APP/api/propostas/$id/revisao" \
      -H 'Content-Type: application/json' -d "{\"aprovado\":$aprovado,\"parecer\":\"revisado pela demo\"}")
    [ "$codigo" = "200" ] && return
    sleep 0.5
  done
  echo "revisão não disponível" >&2
}

esperar_desfecho() {
  for _ in $(seq "${2:-60}"); do
    d=$(curl -s "$APP/api/propostas/$1" | jq -r .desfecho)
    [ "$d" != "EM_ANDAMENTO" ] && break
    sleep 0.5
  done
}

mostrar() {
  curl -s "$APP/api/propostas/$1" | jq '{desfecho, reserva: .reservaDeLimite.status, contrato: .contrato.status, desembolso: (.desembolso != null), eventos: [.eventos[].descricao]}'
}

chaos() {
  curl -s -X PUT "$APP/chaos" -H 'Content-Type: application/json' -d "$1" > /dev/null
}

chaos_off() {
  curl -s -X DELETE "$APP/chaos" > /dev/null
}
