package br.com.diegobraun.emprestimo.proposta;

import java.util.LinkedHashMap;
import java.util.Map;

import br.com.diegobraun.emprestimo.sistemas.ContratoService;
import br.com.diegobraun.emprestimo.sistemas.CoreBancario;
import br.com.diegobraun.emprestimo.sistemas.LimiteService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/propostas")
public class PropostaController {

    private final PropostaService service;
    private final Acompanhamento acompanhamento;
    private final LimiteService limites;
    private final ContratoService contratos;
    private final CoreBancario core;

    public PropostaController(PropostaService service, Acompanhamento acompanhamento, LimiteService limites,
                              ContratoService contratos, CoreBancario core) {
        this.service = service;
        this.acompanhamento = acompanhamento;
        this.limites = limites;
        this.contratos = contratos;
        this.core = core;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> abrir(@Valid @RequestBody NovaProposta proposta) {
        PropostaAberta aberta = service.abrir(proposta);
        return ResponseEntity.status(201).body(Map.of(
                "propostaId", aberta.propostaId(),
                "processInstanceId", aberta.processInstanceId(),
                "acompanhar", "/api/propostas/" + aberta.propostaId(),
                "cockpit", "http://localhost:8086/camunda/app/cockpit/default/#/process-instance/" + aberta.processInstanceId()));
    }

    @PostMapping("/{id}/assinatura")
    public ResponseEntity<Map<String, Object>> assinar(@PathVariable String id) {
        boolean correlacionada = service.assinar(id);
        return ResponseEntity.status(correlacionada ? 202 : 409)
                .body(Map.of("propostaId", id, "assinaturaCorrelacionada", correlacionada));
    }

    @PostMapping("/{id}/revisao")
    public ResponseEntity<Map<String, Object>> revisar(@PathVariable String id, @Valid @RequestBody Revisao revisao) {
        return service.revisar(id, revisao)
                .<ResponseEntity<Map<String, Object>>>map(tarefa -> ResponseEntity.ok(Map.of("propostaId", id, "taskId", tarefa)))
                .orElseGet(() -> ResponseEntity.status(409).body(Map.of("erro", "nenhuma revisão pendente para a proposta")));
    }

    @GetMapping("/{id}")
    public Map<String, Object> acompanhar(@PathVariable String id) {
        Map<String, Object> resposta = new LinkedHashMap<>();
        Acompanhamento.Registro registro = acompanhamento.registro(id);
        resposta.put("propostaId", id);
        resposta.put("desfecho", registro.desfecho());
        resposta.put("processInstanceId", registro.processInstanceId());
        resposta.put("reservaDeLimite", limites.reserva(id).orElse(null));
        resposta.put("contrato", contratos.contrato(id).orElse(null));
        resposta.put("desembolso", core.lancamento(id + ":desembolso").orElse(null));
        resposta.put("eventos", registro.eventos());
        return resposta;
    }
}
