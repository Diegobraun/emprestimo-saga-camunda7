package br.com.diegobraun.emprestimo.delegates;

import br.com.diegobraun.emprestimo.proposta.Acompanhamento;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component("registrarDesfecho")
public class RegistrarDesfecho implements JavaDelegate {

    private final Acompanhamento acompanhamento;

    public RegistrarDesfecho(Acompanhamento acompanhamento) {
        this.acompanhamento = acompanhamento;
    }

    @Override
    public void execute(DelegateExecution execution) {
        acompanhamento.desfecho(execution.getProcessBusinessKey(), (String) execution.getVariableLocal("desfecho"));
    }
}
