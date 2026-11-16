package br.com.diegobraun.emprestimo.sistemas;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import br.com.diegobraun.emprestimo.AppProperties;
import org.springframework.stereotype.Component;

@Component
public class LimiteService {

    public enum Status { RESERVADA, LIBERADA, UTILIZADA }

    public record Reserva(String propostaId, String cliente, BigDecimal valor, Status status) {}

    public static class LimiteInsuficienteException extends RuntimeException {
        public LimiteInsuficienteException(BigDecimal disponivel, BigDecimal pedido) {
            super("limite disponível " + disponivel.toPlainString() + " menor que " + pedido.toPlainString());
        }
    }

    private final BigDecimal limitePorCliente;
    private final Map<String, Reserva> reservas = new ConcurrentHashMap<>();

    public LimiteService(AppProperties props) {
        this.limitePorCliente = props.limitePorCliente();
    }

    public synchronized Reserva reservar(String propostaId, String cliente, BigDecimal valor) {
        Reserva existente = reservas.get(propostaId);
        if (existente != null) {
            return existente;
        }
        BigDecimal disponivel = disponivel(cliente);
        if (disponivel.compareTo(valor) < 0) {
            throw new LimiteInsuficienteException(disponivel, valor);
        }
        Reserva reserva = new Reserva(propostaId, cliente, valor, Status.RESERVADA);
        reservas.put(propostaId, reserva);
        return reserva;
    }

    public synchronized Optional<Reserva> liberar(String propostaId) {
        return Optional.ofNullable(reservas.computeIfPresent(propostaId, (id, r) ->
                r.status() == Status.RESERVADA ? new Reserva(id, r.cliente(), r.valor(), Status.LIBERADA) : r));
    }

    public synchronized void utilizar(String propostaId) {
        reservas.computeIfPresent(propostaId, (id, r) -> new Reserva(id, r.cliente(), r.valor(), Status.UTILIZADA));
    }

    public synchronized BigDecimal disponivel(String cliente) {
        BigDecimal comprometido = reservas.values().stream()
                .filter(r -> r.cliente().equals(cliente) && r.status() != Status.LIBERADA)
                .map(Reserva::valor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return limitePorCliente.subtract(comprometido);
    }

    public Optional<Reserva> reserva(String propostaId) {
        return Optional.ofNullable(reservas.get(propostaId));
    }

    public List<Reserva> todas() {
        return List.copyOf(reservas.values());
    }
}
