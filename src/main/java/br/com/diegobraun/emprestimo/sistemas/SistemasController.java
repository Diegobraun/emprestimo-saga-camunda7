package br.com.diegobraun.emprestimo.sistemas;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SistemasController {

    private final ChaosService chaos;
    private final LimiteService limites;
    private final CoreBancario core;

    public SistemasController(ChaosService chaos, LimiteService limites, CoreBancario core) {
        this.chaos = chaos;
        this.limites = limites;
        this.core = core;
    }

    @GetMapping("/api/sistemas")
    public Map<String, Object> sistemas() {
        Map<String, Object> estado = new LinkedHashMap<>();
        estado.put("saldosNoCore", core.saldos());
        estado.put("reservasDeLimite", limites.todas());
        estado.put("chaos", chaos.atual());
        return estado;
    }

    @PutMapping("/chaos")
    public Chaos definir(@RequestBody Chaos novo) {
        return chaos.definir(novo);
    }

    @DeleteMapping("/chaos")
    public Chaos desligar() {
        return chaos.definir(Chaos.desligado());
    }
}
