package br.com.diegobraun.emprestimo.sistemas;

import java.util.concurrent.atomic.AtomicReference;

import org.springframework.stereotype.Component;

@Component
public class ChaosService {

    private final AtomicReference<Chaos> atual = new AtomicReference<>(Chaos.desligado());

    public Chaos atual() {
        return atual.get();
    }

    public Chaos definir(Chaos chaos) {
        atual.set(chaos);
        return chaos;
    }
}
