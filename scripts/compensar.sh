#!/usr/bin/env bash
source "$(dirname "$0")/lib.sh"

ID="${1:?uso: compensar.sh <propostaId> [CompensarCancelada|CompensarExpirada]}"
DESTINO="${2:-CompensarCancelada}"

INSTANCIA=$(curl -s "$APP/api/propostas/$ID" | jq -r .processInstanceId)
ATIVIDADES=$(curl -s "$APP/engine-rest/process-instance/$INSTANCIA/activity-instances" \
  | jq -c '[(.childActivityInstances[] | {type: "cancel", activityInstanceId: .id}), (.childTransitionInstances[] | {type: "cancel", transitionInstanceId: .id})]')

if [ "$ATIVIDADES" = "[]" ] || [ "$ATIVIDADES" = "null" ]; then
  echo "instância $INSTANCIA não tem nada ativo"
  exit 1
fi

INSTRUCOES=$(jq -c ". + [{type: \"startBeforeActivity\", activityId: \"$DESTINO\"}]" <<< "$ATIVIDADES")
passo "instância $INSTANCIA: $INSTRUCOES"
curl -s -o /dev/null -w '   modificação: HTTP %{http_code}\n' -X POST "$APP/engine-rest/process-instance/$INSTANCIA/modification" \
  -H 'Content-Type: application/json' -d "{\"instructions\":$INSTRUCOES}"

esperar_desfecho "$ID" 20
mostrar "$ID"
