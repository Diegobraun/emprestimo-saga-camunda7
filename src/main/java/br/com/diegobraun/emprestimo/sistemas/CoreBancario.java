package br.com.diegobraun.emprestimo.sistemas;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

@Component
public class CoreBancario {

    public record Lancamento(String operacaoId, String conta, BigDecimal valor) {}

    public static class ContaInexistenteException extends RuntimeException {
        public ContaInexistenteException(String conta) {
            super("conta " + conta + " não existe");
        }
    }

    public static class CoreIndisponivelException extends RuntimeException {
        public CoreIndisponivelException() {
            super("core bancário fora do ar");
        }
    }

    private final Map<String, BigDecimal> saldos = new ConcurrentHashMap<>();
    private final Map<String, Lancamento> lancamentos = new ConcurrentHashMap<>();
    private final ChaosService chaos;

    public CoreBancario(ChaosService chaos) {
        this.chaos = chaos;
        for (int i = 1; i <= 3; i++) {
            saldos.put("conta-" + i, BigDecimal.ZERO);
        }
    }

    public synchronized Lancamento creditar(String operacaoId, String conta, BigDecimal valor) {
        if (chaos.atual().coreForaDoAr()) {
            throw new CoreIndisponivelException();
        }
        Lancamento existente = lancamentos.get(operacaoId);
        if (existente != null) {
            return existente;
        }
        BigDecimal saldo = saldos.get(conta);
        if (saldo == null) {
            throw new ContaInexistenteException(conta);
        }
        saldos.put(conta, saldo.add(valor));
        Lancamento lancamento = new Lancamento(operacaoId, conta, valor);
        lancamentos.put(operacaoId, lancamento);
        return lancamento;
    }

    public Optional<Lancamento> lancamento(String operacaoId) {
        return Optional.ofNullable(lancamentos.get(operacaoId));
    }

    public Map<String, BigDecimal> saldos() {
        return Map.copyOf(saldos);
    }
}
