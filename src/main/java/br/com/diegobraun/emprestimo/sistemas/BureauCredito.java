package br.com.diegobraun.emprestimo.sistemas;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

@Component
public class BureauCredito {

    public enum Decisao { APROVADA, REVISAO, RECUSADA }

    public record Analise(int score, Decisao decisao, BigDecimal taxaMensal) {}

    private static final BigDecimal VALOR_QUE_EXIGE_REVISAO = new BigDecimal("50000");

    public Analise analisar(String cliente, BigDecimal valor) {
        int score = score(cliente);
        Decisao decisao;
        if (score < 500) {
            decisao = Decisao.RECUSADA;
        } else if (score < 700 || valor.compareTo(VALOR_QUE_EXIGE_REVISAO) > 0) {
            decisao = Decisao.REVISAO;
        } else {
            decisao = Decisao.APROVADA;
        }
        BigDecimal taxa = score >= 750 ? new BigDecimal("1.49") : new BigDecimal("2.39");
        return new Analise(score, decisao, taxa);
    }

    private int score(String cliente) {
        String c = cliente.toLowerCase();
        if (c.contains("ruim")) {
            return 420;
        }
        if (c.contains("medio")) {
            return 640;
        }
        if (c.contains("bom")) {
            return 820;
        }
        return 500 + Math.floorMod(cliente.hashCode(), 400);
    }
}
