package br.com.diegobraun.emprestimo.camera;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.stereotype.Component;

@Component
public class CameraLenta {

    private final AtomicReference<Duration> pausa = new AtomicReference<>(Duration.ZERO);

    public Duration atual() {
        return pausa.get();
    }

    public Duration definir(Duration nova) {
        pausa.set(nova.isNegative() ? Duration.ZERO : nova);
        return pausa.get();
    }

    void pausar() {
        Duration d = pausa.get();
        if (d.isZero()) {
            return;
        }
        try {
            Thread.sleep(d);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
