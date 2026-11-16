package br.com.diegobraun.emprestimo.camera;

import java.time.Duration;
import java.util.Map;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CameraLentaController {

    private final CameraLenta camera;

    public CameraLentaController(CameraLenta camera) {
        this.camera = camera;
    }

    @GetMapping("/camera-lenta")
    public Map<String, Long> atual() {
        return Map.of("segundosPorPasso", camera.atual().toSeconds());
    }

    @PutMapping("/camera-lenta")
    public Map<String, Long> ligar(@RequestParam(defaultValue = "5") long segundos) {
        camera.definir(Duration.ofSeconds(segundos));
        return atual();
    }

    @DeleteMapping("/camera-lenta")
    public Map<String, Long> desligar() {
        camera.definir(Duration.ZERO);
        return atual();
    }
}
