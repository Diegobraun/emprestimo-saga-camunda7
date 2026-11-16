package br.com.diegobraun.emprestimo.delegates;

import br.com.diegobraun.emprestimo.proposta.Acompanhamento;
import br.com.diegobraun.emprestimo.sistemas.ContratoService;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component("gerarContrato")
public class GerarContrato implements JavaDelegate {

    private final ContratoService contratos;
    private final Acompanhamento acompanhamento;

    public GerarContrato(ContratoService contratos, Acompanhamento acompanhamento) {
        this.contratos = contratos;
        this.acompanhamento = acompanhamento;
    }

    @Override
    public void execute(DelegateExecution execution) {
        String propostaId = execution.getProcessBusinessKey();
        try {
            ContratoService.Contrato contrato = contratos.gerar(propostaId,
                    Variaveis.texto(execution, "cliente"),
                    Variaveis.decimal(execution, "valor"),
                    Variaveis.inteiro(execution, "parcelas"),
                    Variaveis.decimal(execution, "taxaMensal"));
            acompanhamento.registrar(propostaId, "contrato " + contrato.numero() + " gerado, aguardando assinatura");
            execution.setVariable("numeroContrato", contrato.numero());
        } catch (ContratoService.RestricaoJuridicaException e) {
            acompanhamento.registrar(propostaId, "contrato recusado: " + e.getMessage());
            throw new BpmnError("CONTRATO_RECUSADO", e.getMessage());
        }
    }
}
