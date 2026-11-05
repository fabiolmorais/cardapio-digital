package br.com.cardapio.application.port.in.push;

/** @param publicKey a chave pública VAPID (base64url); null quando o push está desligado */
public record PushSettings(boolean enabled, String publicKey) {
}
