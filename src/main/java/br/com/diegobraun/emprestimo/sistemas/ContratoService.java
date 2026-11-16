package br.com.diegobraun.emprestimo.sistemas;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Component;

@Component
public class ContratoService {

    public enum Status { GERADO, CANCELADO, ATIVO }

    public record Contrato(String numero, String propostaId, String cliente, BigDecimal valor, int parcelas,
                           BigDecimal taxaMensal, Status status) {

        Contrato com(Status novo) {
            return new Contrato(numero, propostaId, cliente, valor, parcelas, taxaMensal, novo);
        }
    }

    public static class RestricaoJuridicaException extends RuntimeException {
        public RestricaoJuridicaException(String cliente) {
            super("cliente " + cliente + " tem restrição jurídica, contrato não pode ser emitido");
        }
    }

    private final Map<String, Contrato> contratos = new ConcurrentHashMap<>();
    private final AtomicInteger sequencia = new AtomicInteger(1000);
    private final ChaosService chaos;

    public ContratoService(ChaosService chaos) {
        this.chaos = chaos;
    }

    public Contrato gerar(String propostaId, String cliente, BigDecimal valor, int parcelas, BigDecimal taxaMensal) {
        if (cliente.toLowerCase().contains("restricao")) {
            throw new RestricaoJuridicaException(cliente);
        }
        return contratos.computeIfAbsent(propostaId, id -> new Contrato(
                "CTR-" + sequencia.incrementAndGet(), id, cliente, valor, parcelas, taxaMensal, Status.GERADO));
    }

    public Optional<Contrato> cancelar(String propostaId) {
        return Optional.ofNullable(contratos.computeIfPresent(propostaId, (id, c) ->
                c.status() == Status.GERADO ? c.com(Status.CANCELADO) : c));
    }

    public Contrato ativar(String propostaId) {
        if (chaos.atual().ativacaoFalhando()) {
            throw new IllegalStateException("sistema de contratos indisponível");
        }
        Contrato contrato = contratos.computeIfPresent(propostaId, (id, c) -> c.com(Status.ATIVO));
        if (contrato == null) {
            throw new IllegalStateException("contrato da proposta " + propostaId + " não existe");
        }
        return contrato;
    }

    public Optional<Contrato> contrato(String propostaId) {
        return Optional.ofNullable(contratos.get(propostaId));
    }
}
