# emprestimo-saga-camunda7

A mesma saga de contratação de empréstimo do projeto `emprestimo-saga-camunda` (Camunda 8), refeita em **Camunda 7.24** para comparar os dois motores. O processo, os sistemas simulados, a API e os cenários são os mesmos. Muda o motor, o jeito de escrever cada passo e a ferramenta de operação.

## Rodando

Pré-requisitos: Java 21+, Maven e `jq`. **Não precisa de Docker**: o motor roda dentro da aplicação.

```bash
mvn spring-boot:run
bash scripts/demo.sh
mvn test
```

| O quê | Endereço |
|---|---|
| API | http://localhost:8086 |
| Cockpit (instâncias, incidentes) | http://localhost:8086/camunda/app/cockpit (demo/demo) |
| Tasklist | http://localhost:8086/camunda/app/tasklist (demo/demo) |
| REST do motor | http://localhost:8086/engine-rest |

O banco é H2 em arquivo (`target/camunda-h2`). Instâncias e histórico sobrevivem a um restart; os sistemas simulados (limites, contratos, core) não.

## O processo

O diagrama é o mesmo do Camunda 8: mesmos ids, mesmos caminhos, mesmas compensações. Só mudam as extensões do XML:

| No Camunda 8 (`zeebe:`) | No Camunda 7 (`camunda:`) |
|---|---|
| `<zeebe:taskDefinition type="desembolsar" retries="3"/>` | `camunda:delegateExpression="${desembolsar}"` + `camunda:asyncBefore="true"` + `<camunda:failedJobRetryTimeCycle>R3/PT3S` |
| `<zeebe:taskHeaders>` com `desfecho` | `<camunda:inputParameter name="desfecho">` |
| condição FEEL: `=decisaoCredito = "APROVADA"` | condição JUEL: `${decisaoCredito == 'APROVADA'}` |
| timer `=prazoAssinatura` | timer `${prazoAssinatura}` |
| user task + formulário `.form` | user task com `camunda:formData` (formulário gerado) |
| mensagem com `zeebe:subscription correlationKey` | mensagem correlacionada por **business key** (`propostaId`) |

Os ⏪ (compensation boundary event, tarefas `isForCompensation` e throw de compensação) são idênticos. Fazem parte do padrão BPMN 2.0, e o Camunda 7 suporta desde sempre.

## As diferenças que importam

### 1. Onde o código roda
- **Camunda 8:** o motor (Zeebe) é um servidor separado. O código Java é um *job worker* que busca trabalho pela rede (`@JobWorker`).
- **Camunda 7:** o motor é uma biblioteca **dentro** da aplicação. Cada passo é um `JavaDelegate` que o motor chama direto, na mesma JVM e na mesma transação de banco.

```java
@Component("desembolsar")
public class Desembolsar implements JavaDelegate {
    public void execute(DelegateExecution execution) { ... }
}
```

Isso deixa o 7 mais simples de rodar e testar: os testes daqui levam ~3s, contra ~45s no 8, que sobe um container. Em compensação, o motor escala junto com a aplicação, e não separado dela.

### 2. Transação e `asyncBefore`
No 7, o motor executa os passos **na thread de quem chamou**, até chegar num *wait state* (tarefa humana, receive task, timer). Sem `asyncBefore`:
- `POST /api/propostas` rodaria crédito, reserva e contrato dentro da requisição HTTP;
- uma exceção no meio desfaria tudo (rollback) e voltaria como erro 500 para o cliente.

Com `camunda:asyncBefore="true"`, o motor grava um *job* e devolve na hora. O *job executor* pega o job em background. É isso que permite retry com intervalo e incidente.

No 8 não existe essa escolha: todo passo já é assíncrono, porque o worker está do outro lado da rede.

### 3. Retry e incidente
- **8:** `retries="3"` na tarefa, e o worker decide o backoff (`jobError(..., Duration.ofSeconds(3))`).
- **7:** `<camunda:failedJobRetryTimeCycle>R3/PT3S</camunda:failedJobRetryTimeCycle>` no BPMN, e qualquer exceção do delegate conta como falha.

Nos dois, acabadas as tentativas, o passo vira incidente, e no Cockpit ou no Operate aparece o botão **Retry**. No 7 há um detalhe: com `asyncBefore`, o processo fica parado **antes** da atividade (um *transition instance*), não dentro dela. Isso aparece na API e foi o motivo de o `compensar.sh` precisar tratar os dois casos.

### 4. Mensagem que chega antes da hora
- **8:** a mensagem tem `timeToLive`. Se chegar antes de o processo estar esperando, o motor guarda e entrega depois. Mensagem repetida com o mesmo `messageId` é ignorada.
- **7:** **não existe buffer**. Se ninguém estiver esperando, `correlate()` lança `MismatchingMessageCorrelationException` e a mensagem se perde. Aqui a API devolve 409 e o `scripts/lib.sh` tenta de novo por alguns segundos.

O teste `assinaturaSemNinguemEsperandoEhDescartada` mostra isso: a assinatura enviada antes da hora é recusada, a enviada na hora certa passa, e a segunda vez é recusada porque o processo já seguiu.

### 5. Ordem das compensações
Na demo, o 7 executou as compensações **sempre na ordem inversa** da execução (cancela o contrato, depois libera o limite). No 8 a ordem variou entre execuções. Mesmo assim, não dependa disso: se a ordem importar, desenhe a sequência no diagrama.

### 6. Consistência das telas
- **7:** o Cockpit lê direto o banco do motor. O que você vê é o estado atual.
- **8:** o Operate lê de um índice no Elasticsearch, atualizado com atraso. A busca de tarefas e incidentes pode não achar algo criado há meio segundo.

### 7. Operação manual
| Ação | Cockpit 7 Community | Operate 8 |
|---|---|---|
| Ver incidente e dar Retry | sim | sim |
| Cancelar instância | sim | sim |
| Migrar instâncias de versão | sim | sim |
| **Modificar instância** (mover o processo para outro ponto) | **não** (só na edição Enterprise) | sim |

Mas dá para compensar pelo Cockpit Community usando o que ele tem: **editar variável** e **Retry**. O `Desembolsar` olha a variável `cancelarDesembolso` antes de chamar o core. Se ela for `true`, ele lança o mesmo `BpmnError("DESEMBOLSO_RECUSADO")` da conta inexistente, e o processo segue o caminho de compensação já desenhado. Num incidente:

1. aba **Variables** → **Add variable**: `cancelarDesembolso`, tipo Boolean, valor `true`;
2. aba **Incidents** → **Retry**;
3. a instância termina em `CANCELADA_DESEMBOLSO`, com contrato cancelado e limite liberado.

A decisão do operador vira dado do processo e fica no histórico, e o caminho percorrido é o do modelo. É o jeito comum de dar ao operador do 7 um "botão de cancelar" sem a edição Enterprise. O teste `operadorCancelaPorVariavelERetryECompensa` cobre esse fluxo.

A outra saída é a modificação de instância por REST:

```bash
bash scripts/compensar.sh <propostaId>
```

O script cancela o que estiver ativo e inicia o throw `CompensarCancelada`, igual ao Move que você fez no Operate.

Lembrete que vale para os dois motores: **cancelar a instância não compensa nada.** O processo morre, e limite e contrato ficam pendurados nos sistemas.

## Cenários

Os mesmos sete do projeto do Camunda 8 (`scripts/demo.sh`):

| # | Entrada | Desfecho |
|---|---|---|
| 1 | `cliente-bom` | CONTRATADO |
| 2 | `cliente-ruim` | RECUSADA_CREDITO |
| 3 | `cliente-medio` | revisão no Tasklist, depois CONTRATADO |
| 4 | prazo de 10s sem assinar | EXPIRADA, compensa reserva e contrato |
| 5 | `conta-fantasma` | CANCELADA_DESEMBOLSO, compensa reserva e contrato |
| 6 | `cliente-bom-restricao` | RECUSADA_CONTRATO, compensa só a reserva |
| 7 | core fora do ar | incidente no Cockpit |

O grupo de analistas se chama `analistas`, e o usuário `demo` é colocado nele na subida (`GrupoAnalistas`), para a tarefa aparecer no Tasklist. Id com hífen não é aceito: o Camunda 7 só aceita ids alfanuméricos por padrão.

## Testes

`ContratacaoEmprestimoProcessTest` usa `camunda-bpm-assert` no estilo clássico do 7:
- o job executor fica desligado (`camunda.bpm.job-execution.enabled=false`) e o teste executa cada job com `managementService.executeJob`, controlando exatamente quando cada passo assíncrono roda;
- o timer é testado avançando o relógio do motor (`ClockUtil.setCurrentTime`);
- o incidente é provocado com o core fora do ar, executando o job até as tentativas acabarem, e resolvido com `setJobRetries` (o equivalente do botão Retry).

## Qual escolher

| | Camunda 7 | Camunda 8 |
|---|---|---|
| Infra | só a aplicação + banco relacional | cluster Zeebe + Elasticsearch/OpenSearch |
| Escala | junto com a aplicação, limitada pelo banco | horizontal, feita para alto volume |
| Operação | Cockpit: direto, denso e em tempo real | Operate + Tasklist + Optimize: mais telas, com atraso de indexação |
| Teste | rápido, em memória | precisa de container |
| Futuro | Community encerrada na 7.24; o fork comunitário é o Operaton | produto atual, onde está o investimento |

Para aprender BPMN e saga, o 7 deixa tudo mais visível. Para mercado e sistemas novos, o caminho é o 8.
