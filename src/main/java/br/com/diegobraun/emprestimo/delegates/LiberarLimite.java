package br.com.diegobraun.emprestimo.delegates;

import br.com.diegobraun.emprestimo.proposta.Acompanhamento;
import br.com.diegobraun.emprestimo.sistemas.LimiteService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component("liberarLimite")
public class LiberarLimite implements JavaDelegate {

    private final LimiteService limites;
    private final Acompanhamento acompanhamento;

    public LiberarLimite(LimiteService limites, Acompanhamento acompanhamento) {
        this.limites = limites;
        this.acompanhamento = acompanhamento;
    }

    @Override
    public void execute(DelegateExecution execution) {
        limites.liberar(execution.getProcessBusinessKey());
        acompanhamento.registrar(execution.getProcessBusinessKey(), "COMPENSAÇÃO: limite liberado");
    }
}
