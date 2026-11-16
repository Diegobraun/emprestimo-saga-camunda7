package br.com.diegobraun.emprestimo.delegates;

import br.com.diegobraun.emprestimo.proposta.Acompanhamento;
import br.com.diegobraun.emprestimo.sistemas.BureauCredito;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component("analisarCredito")
public class AnalisarCredito implements JavaDelegate {

    private final BureauCredito bureau;
    private final Acompanhamento acompanhamento;

    public AnalisarCredito(BureauCredito bureau, Acompanhamento acompanhamento) {
        this.bureau = bureau;
        this.acompanhamento = acompanhamento;
    }

    @Override
    public void execute(DelegateExecution execution) {
        String propostaId = execution.getProcessBusinessKey();
        BureauCredito.Analise analise = bureau.analisar(Variaveis.texto(execution, "cliente"), Variaveis.decimal(execution, "valor"));
        acompanhamento.registrar(propostaId, "crédito analisado: score " + analise.score() + " -> " + analise.decisao());
        execution.setVariable("score", analise.score());
        execution.setVariable("decisaoCredito", analise.decisao().name());
        execution.setVariable("taxaMensal", analise.taxaMensal());
    }
}
