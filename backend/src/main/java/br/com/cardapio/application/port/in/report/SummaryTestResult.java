package br.com.cardapio.application.port.in.report;

/**
 * O que o "Enviar um teste para mim" conseguiu.
 *
 * @param email       o e-mail saiu (quem só entra pelo PIN não tem e-mail)
 * @param pushDevices em quantos aparelhos o aviso chegou
 */
public record SummaryTestResult(boolean email, int pushDevices) {
}
