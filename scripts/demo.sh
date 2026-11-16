#!/usr/bin/env bash
source "$(dirname "$0")/lib.sh"
chaos_off

titulo "1. Caminho feliz: aprovada, assinada, desembolsada"
ID=$(abrir cliente-bom 10000)
sleep 2
assinar "$ID"
esperar_desfecho "$ID"
mostrar "$ID"

titulo "2. Score baixo: recusada antes de reservar qualquer coisa"
ID=$(abrir cliente-ruim 10000)
esperar_desfecho "$ID"
mostrar "$ID"

titulo "3. Score médio: vai para o analista (tarefa humana)"
ID=$(abrir cliente-medio 8000)
passo "veja a tarefa em http://localhost:8086/camunda/app/tasklist (demo/demo); aqui a demo aprova pela API"
revisar "$ID" true
sleep 1
assinar "$ID"
esperar_desfecho "$ID"
mostrar "$ID"

titulo "4. Assinatura não chega: timer estoura e compensa reserva e contrato"
ID=$(abrir cliente-bom 5000 conta-1 PT10S)
passo "esperando o prazo de 10s..."
esperar_desfecho "$ID" 40
mostrar "$ID"

titulo "5. Conta de destino não existe: erro de negócio no desembolso, compensa"
ID=$(abrir cliente-bom 3000 conta-fantasma)
sleep 2
assinar "$ID"
esperar_desfecho "$ID"
mostrar "$ID"

titulo "6. Cliente com restrição jurídica: contrato recusado, desfaz só a reserva"
ID=$(abrir cliente-bom-restricao 4000)
esperar_desfecho "$ID"
mostrar "$ID"

titulo "7. Core fora do ar: retries, depois incidente no Cockpit"
chaos '{"coreForaDoAr":true,"ativacaoFalhando":false}'
ID=$(abrir cliente-bom 2000 conta-2)
sleep 2
assinar "$ID"
passo "job executor: 3 tentativas com 3s de intervalo..."
sleep 15
mostrar "$ID"
chaos_off
passo "core voltou. Abra o Cockpit, encontre o incidente e clique em Retry:"
curl -s "$APP/api/propostas/$ID" | jq -r '"   http://localhost:8086/camunda/app/cockpit/default/#/process-instance/" + .processInstanceId + "/runtime"'
