package br.com.diegobraun.emprestimo.delegates;

import java.math.BigDecimal;

import org.camunda.bpm.engine.delegate.DelegateExecution;

final class Variaveis {

    private Variaveis() {
    }

    static String texto(DelegateExecution execution, String nome) {
        return (String) execution.getVariable(nome);
    }

    static BigDecimal decimal(DelegateExecution execution, String nome) {
        Object valor = execution.getVariable(nome);
        return valor instanceof BigDecimal b ? b : new BigDecimal(String.valueOf(valor));
    }

    static int inteiro(DelegateExecution execution, String nome) {
        return ((Number) execution.getVariable(nome)).intValue();
    }
}
