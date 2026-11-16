package br.com.diegobraun.emprestimo.proposta;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import br.com.diegobraun.emprestimo.AppProperties;
import org.camunda.bpm.engine.MismatchingMessageCorrelationException;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.springframework.stereotype.Service;

@Service
public class PropostaService {

    public static final String PROCESSO = "contratacao-emprestimo";
    public static final String MENSAGEM_ASSINATURA = "contrato-assinado";

    private final RuntimeService runtime;
    private final TaskService tarefas;
    private final Acompanhamento acompanhamento;
    private final AppProperties props;

    public PropostaService(RuntimeService runtime, TaskService tarefas, Acompanhamento acompanhamento, AppProperties props) {
        this.runtime = runtime;
        this.tarefas = tarefas;
        this.acompanhamento = acompanhamento;
        this.props = props;
    }

    public PropostaAberta abrir(NovaProposta proposta) {
        String propostaId = UUID.randomUUID().toString();
        Map<String, Object> variaveis = new LinkedHashMap<>();
        variaveis.put("propostaId", propostaId);
        variaveis.put("cliente", proposta.cliente());
        variaveis.put("valor", proposta.valor());
        variaveis.put("parcelas", proposta.parcelas());
        variaveis.put("contaDestino", proposta.contaDestino());
        variaveis.put("prazoAssinatura", proposta.prazoAssinatura() != null
                ? proposta.prazoAssinatura() : props.prazoAssinaturaPadrao());
        acompanhamento.registrar(propostaId, "proposta recebida");
        ProcessInstance instancia = runtime.startProcessInstanceByKey(PROCESSO, propostaId, variaveis);
        acompanhamento.iniciada(propostaId, instancia.getId());
        return new PropostaAberta(propostaId, instancia.getId());
    }

    public boolean assinar(String propostaId) {
        try {
            runtime.createMessageCorrelation(MENSAGEM_ASSINATURA)
                    .processInstanceBusinessKey(propostaId)
                    .correlate();
            acompanhamento.registrar(propostaId, "assinatura recebida");
            return true;
        } catch (MismatchingMessageCorrelationException e) {
            acompanhamento.registrar(propostaId, "assinatura descartada: ninguém esperando por ela");
            return false;
        }
    }

    public Optional<String> revisar(String propostaId, Revisao revisao) {
        Task tarefa = tarefas.createTaskQuery()
                .processInstanceBusinessKey(propostaId)
                .taskDefinitionKey("RevisarProposta")
                .singleResult();
        if (tarefa == null) {
            return Optional.empty();
        }
        tarefas.complete(tarefa.getId(), Map.of("aprovadoManualmente", revisao.aprovado(), "parecer", revisao.parecer()));
        acompanhamento.registrar(propostaId, "analista " + (revisao.aprovado() ? "aprovou" : "recusou") + ": " + revisao.parecer());
        return Optional.of(tarefa.getId());
    }
}
