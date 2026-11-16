package br.com.diegobraun.emprestimo;

import java.math.BigDecimal;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app")
public record AppProperties(String prazoAssinaturaPadrao, BigDecimal limitePorCliente) {}
