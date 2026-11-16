package br.com.diegobraun.emprestimo.sistemas;

public record Chaos(boolean coreForaDoAr, boolean ativacaoFalhando) {

    public static Chaos desligado() {
        return new Chaos(false, false);
    }
}
