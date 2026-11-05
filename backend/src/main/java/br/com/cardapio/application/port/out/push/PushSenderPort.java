package br.com.cardapio.application.port.out.push;

import br.com.cardapio.domain.push.model.PushMessage;
import br.com.cardapio.domain.push.model.PushSubscription;

import java.net.URI;

public interface PushSenderPort {

    /** O resultado de um envio. GONE: o serviço de push diz que a inscrição acabou (404 ou 410). */
    enum Delivery {
        SENT,
        GONE,
        FAILED
    }

    /** As chaves VAPID estão configuradas? Sem elas, o push fica desligado (no computador, é o normal). */
    boolean enabled();

    /** A chave pública VAPID, em base64url: o navegador precisa dela para se inscrever. */
    String publicKey();

    /** O endpoint é de um serviço de push conhecido? Mandar para qualquer endereço seria um SSRF. */
    boolean accepts(URI endpoint);

    Delivery send(PushSubscription subscription, PushMessage message);
}
