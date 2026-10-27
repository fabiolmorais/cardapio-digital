package br.com.cardapio.application.port.out.security;

/** Gera códigos numéricos aleatórios para pessoas digitarem (como o código do aparelho). */
public interface RandomCodeGenerator {

    /** Só dígitos de 0 a 9, com zeros à esquerda ("004815" é válido). */
    String digits(int length);
}
