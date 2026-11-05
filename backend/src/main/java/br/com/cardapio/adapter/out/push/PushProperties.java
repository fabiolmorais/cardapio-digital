package br.com.cardapio.adapter.out.push;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * As notificações no celular (web push).
 *
 * @param vapidPublicKey  a chave pública VAPID (ponto P-256 sem compressão, em base64url)
 * @param vapidPrivateKey a chave privada VAPID (o número de 32 bytes, em base64url); vazia desliga o push
 * @param subject         quem manda, para o serviço de push avisar se algo der errado (mailto: ou https:)
 * @param allowedOrigins  os serviços de push aceitos, como "https://fcm.googleapis.com" ou
 *                        "https://*.notify.windows.com"
 */
@ConfigurationProperties(prefix = "app.push")
public record PushProperties(String vapidPublicKey, String vapidPrivateKey, String subject,
                             List<String> allowedOrigins) {

    public PushProperties {
        allowedOrigins = allowedOrigins == null ? List.of() : List.copyOf(allowedOrigins);
    }

    public boolean configured() {
        return vapidPublicKey != null && !vapidPublicKey.isBlank()
                && vapidPrivateKey != null && !vapidPrivateKey.isBlank();
    }
}
