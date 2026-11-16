package br.com.diegobraun.emprestimo.proposta;

import java.math.BigDecimal;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record NovaProposta(
        @NotBlank String cliente,
        @NotNull @Positive BigDecimal valor,
        @Min(1) @Max(120) int parcelas,
        @NotBlank String contaDestino,
        String prazoAssinatura) {}
