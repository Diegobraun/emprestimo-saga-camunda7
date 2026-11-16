package br.com.diegobraun.emprestimo.delegates;

import br.com.diegobraun.emprestimo.proposta.Acompanhamento;
import br.com.diegobraun.emprestimo.sistemas.ContratoService;
import br.com.diegobraun.emprestimo.sistemas.LimiteService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component("ativarContrato")
public class AtivarContrato implements JavaDelegate {

    private final ContratoService contratos;
    private final LimiteService limites;
    private final Acompanhamento acompanhamento;

    public AtivarContrato(ContratoService contratos, LimiteService limites, Acompanhamento acompanhamento) {
        this.contratos = contratos;
        this.limites = limites;
        this.acompanhamento = acompanhamento;
    }

    @Override
    public void execute(DelegateExecution execution) {
        String propostaId = execution.getProcessBusinessKey();
        contratos.ativar(propostaId);
        limites.utilizar(propostaId);
        acompanhamento.registrar(propostaId, "contrato ativado");
    }
}
