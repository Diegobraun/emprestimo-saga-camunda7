package br.com.diegobraun.emprestimo.proposta;

import jakarta.validation.constraints.NotBlank;

public record Revisao(boolean aprovado, @NotBlank String parecer) {}
