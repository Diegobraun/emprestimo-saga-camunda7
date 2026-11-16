package br.com.diegobraun.emprestimo.delegates;

import br.com.diegobraun.emprestimo.proposta.Acompanhamento;
import br.com.diegobraun.emprestimo.sistemas.ContratoService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component("cancelarContrato")
public class CancelarContrato implements JavaDelegate {

    private final ContratoService contratos;
    private final Acompanhamento acompanhamento;

    public CancelarContrato(ContratoService contratos, Acompanhamento acompanhamento) {
        this.contratos = contratos;
        this.acompanhamento = acompanhamento;
    }

    @Override
    public void execute(DelegateExecution execution) {
        contratos.cancelar(execution.getProcessBusinessKey());
        acompanhamento.registrar(execution.getProcessBusinessKey(), "COMPENSAÇÃO: contrato cancelado");
    }
}
