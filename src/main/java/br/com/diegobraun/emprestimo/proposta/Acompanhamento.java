package br.com.diegobraun.emprestimo.proposta;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

@Component
public class Acompanhamento {

    public record Evento(Instant em, String descricao) {}

    public record Registro(String propostaId, String processInstanceId, String desfecho, List<Evento> eventos) {}

    private final Map<String, String> instancias = new ConcurrentHashMap<>();
    private final Map<String, String> desfechos = new ConcurrentHashMap<>();
    private final Map<String, List<Evento>> eventos = new ConcurrentHashMap<>();

    public void iniciada(String propostaId, String processInstanceId) {
        instancias.put(propostaId, processInstanceId);
        registrar(propostaId, "processo iniciado (instância " + processInstanceId + ")");
    }

    public void registrar(String propostaId, String descricao) {
        eventos.computeIfAbsent(propostaId, id -> new ArrayList<>());
        synchronized (eventos.get(propostaId)) {
            eventos.get(propostaId).add(new Evento(Instant.now(), descricao));
        }
    }

    public void desfecho(String propostaId, String desfecho) {
        desfechos.put(propostaId, desfecho);
        registrar(propostaId, "desfecho: " + desfecho);
    }

    public String instancia(String propostaId) {
        return instancias.get(propostaId);
    }

    public Registro registro(String propostaId) {
        List<Evento> lista = eventos.getOrDefault(propostaId, List.of());
        synchronized (lista) {
            return new Registro(propostaId, instancias.get(propostaId),
                    desfechos.getOrDefault(propostaId, "EM_ANDAMENTO"), List.copyOf(lista));
        }
    }
}
