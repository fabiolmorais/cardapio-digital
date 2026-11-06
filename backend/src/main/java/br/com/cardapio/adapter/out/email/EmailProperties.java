package br.com.cardapio.adapter.out.email;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;

/**
 * O e-mail pelo Resend (app.email).
 *
 * @param resendApiKey a chave da API; vazia, o e-mail só é registrado no log (desenvolvimento)
 * @param from         o remetente, de um domínio verificado no Resend (ex.: "Cardápio Digital
 *                     &lt;avisos@cardapio.seudominio.com.br&gt;")
 * @param resendUrl    o endereço da API (nos testes, um servidor falso)
 */
@ConfigurationProperties("app.email")
record EmailProperties(String resendApiKey, String from, URI resendUrl) {

    boolean configured() {
        return resendApiKey != null && !resendApiKey.isBlank();
    }
}
