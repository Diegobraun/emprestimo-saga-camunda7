package br.com.diegobraun.emprestimo.delegates;

import br.com.diegobraun.emprestimo.proposta.Acompanhamento;
import br.com.diegobraun.emprestimo.sistemas.CoreBancario;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component("desembolsar")
public class Desembolsar implements JavaDelegate {

    private final CoreBancario core;
    private final Acompanhamento acompanhamento;

    public Desembolsar(CoreBancario core, Acompanhamento acompanhamento) {
        this.core = core;
        this.acompanhamento = acompanhamento;
    }

    @Override
    public void execute(DelegateExecution execution) {
        String propostaId = execution.getProcessBusinessKey();
        String conta = Variaveis.texto(execution, "contaDestino");
        if (Variaveis.sim(execution, "cancelarDesembolso")) {
            acompanhamento.registrar(propostaId, "operador cancelou o desembolso pelo Cockpit");
            throw new BpmnError("DESEMBOLSO_RECUSADO", "cancelado pelo operador");
        }
        try {
            CoreBancario.Lancamento lancamento = core.creditar(propostaId + ":desembolso", conta, Variaveis.decimal(execution, "valor"));
            acompanhamento.registrar(propostaId, "desembolso de " + lancamento.valor().toPlainString() + " em " + conta);
            execution.setVariable("operacaoDesembolso", lancamento.operacaoId());
        } catch (CoreBancario.ContaInexistenteException e) {
            acompanhamento.registrar(propostaId, "desembolso recusado: " + e.getMessage());
            throw new BpmnError("DESEMBOLSO_RECUSADO", e.getMessage());
        } catch (CoreBancario.CoreIndisponivelException e) {
            acompanhamento.registrar(propostaId, "core indisponível, o job executor vai tentar de novo");
            throw e;
        }
    }
}
