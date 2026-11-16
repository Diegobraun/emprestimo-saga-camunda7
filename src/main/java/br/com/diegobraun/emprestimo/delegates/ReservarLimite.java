package br.com.diegobraun.emprestimo.delegates;

import br.com.diegobraun.emprestimo.proposta.Acompanhamento;
import br.com.diegobraun.emprestimo.sistemas.LimiteService;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component("reservarLimite")
public class ReservarLimite implements JavaDelegate {

    private final LimiteService limites;
    private final Acompanhamento acompanhamento;

    public ReservarLimite(LimiteService limites, Acompanhamento acompanhamento) {
        this.limites = limites;
        this.acompanhamento = acompanhamento;
    }

    @Override
    public void execute(DelegateExecution execution) {
        String propostaId = execution.getProcessBusinessKey();
        try {
            limites.reservar(propostaId, Variaveis.texto(execution, "cliente"), Variaveis.decimal(execution, "valor"));
            acompanhamento.registrar(propostaId, "limite reservado: " + Variaveis.decimal(execution, "valor").toPlainString());
        } catch (LimiteService.LimiteInsuficienteException e) {
            acompanhamento.registrar(propostaId, "sem limite: " + e.getMessage());
            throw new BpmnError("LIMITE_INSUFICIENTE", e.getMessage());
        }
    }
}
