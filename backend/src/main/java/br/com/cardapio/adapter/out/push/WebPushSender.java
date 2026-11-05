package br.com.cardapio.adapter.out.push;

import br.com.cardapio.application.port.out.push.PushSenderPort;
import br.com.cardapio.domain.push.model.PushMessage;
import br.com.cardapio.domain.push.model.PushSubscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * O web push de verdade: cifra a mensagem para o navegador e manda ao serviço de push dele (do
 * Google, da Apple, da Mozilla…), assinando com a chave VAPID do sistema. Sem as
 * chaves, fica desligado (e o painel nem oferece as notificações).
 */
@Component
class WebPushSender implements PushSenderPort {

    private static final Logger LOG = LoggerFactory.getLogger(WebPushSender.class);
    private static final Base64.Encoder B64 = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder B64D = Base64.getUrlDecoder();
    /** Um chamado de 1 hora atrás não serve mais: se o celular estiver sem rede, o serviço descarta. */
    private static final Duration TTL = Duration.ofMinutes(10);
    /** O JWT do VAPID vale por até 24 h; 12 h é o comum. */
    private static final Duration JWT_LIFETIME = Duration.ofHours(12);

    private final PushProperties properties;
    private final JsonMapper json;
    private final Clock clock;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    WebPushSender(PushProperties properties, JsonMapper json, Clock clock) {
        this.properties = properties;
        this.json = json;
        this.clock = clock;
    }

    @Override
    public boolean enabled() {
        return properties.configured();
    }

    @Override
    public String publicKey() {
        return properties.vapidPublicKey();
    }

    /** O esquema e o host batem com um dos serviços aceitos ("*." vale para os subdomínios). */
    @Override
    public boolean accepts(URI endpoint) {
        if (endpoint.getScheme() == null || endpoint.getHost() == null) {
            return false;
        }
        String host = endpoint.getHost().toLowerCase();
        for (String allowed : properties.allowedOrigins()) {
            URI origin = URI.create(allowed.replace("*.", "wildcard."));
            if (!origin.getScheme().equalsIgnoreCase(endpoint.getScheme())) {
                continue;
            }
            String allowedHost = origin.getHost().toLowerCase();
            if (allowedHost.startsWith("wildcard.")
                    ? host.endsWith(allowedHost.substring("wildcard".length()))
                    : host.equals(allowedHost)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Delivery send(PushSubscription subscription, PushMessage message) {
        if (!enabled() || !accepts(subscription.endpoint())) {
            return Delivery.FAILED;
        }
        try {
            Map<String, String> payload = new LinkedHashMap<>();
            payload.put("title", message.title());
            payload.put("body", message.body());
            payload.put("url", message.url());
            payload.put("tag", message.tag());
            byte[] body = WebPushCrypto.encrypt(json.writeValueAsString(payload).getBytes(StandardCharsets.UTF_8),
                    B64D.decode(subscription.p256dh()), B64D.decode(subscription.auth()));
            HttpRequest request = HttpRequest.newBuilder(subscription.endpoint())
                    .timeout(Duration.ofSeconds(10))
                    .header("Authorization", "vapid t=" + jwt(subscription.endpoint()) + ", k=" + publicKey())
                    .header("Content-Encoding", "aes128gcm")
                    .header("Content-Type", "application/octet-stream")
                    .header("TTL", String.valueOf(TTL.toSeconds()))
                    .header("Urgency", "high")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                    .build();
            int status = http.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
            if (status == 404 || status == 410) {
                return Delivery.GONE;
            }
            if (status >= 200 && status < 300) {
                return Delivery.SENT;
            }
            // Só o host e o status: o endpoint inteiro identifica o aparelho.
            LOG.warn("O serviço de push {} recusou a notificação: HTTP {}", subscription.endpoint().getHost(),
                    status);
            return Delivery.FAILED;
        } catch (GeneralSecurityException | IOException e) {
            LOG.warn("Falha ao mandar a notificação para {}: {}", subscription.endpoint().getHost(), e.getMessage());
            return Delivery.FAILED;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Delivery.FAILED;
        }
    }

    /** O JWT do VAPID (RFC 8292): o "aud" é a origem do serviço de push, assinado com ES256. */
    private String jwt(URI endpoint) throws GeneralSecurityException {
        String audience = endpoint.getScheme() + "://" + endpoint.getHost()
                + (endpoint.getPort() == -1 ? "" : ":" + endpoint.getPort());
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("aud", audience);
        claims.put("exp", clock.instant().plus(JWT_LIFETIME).getEpochSecond());
        claims.put("sub", properties.subject());
        String unsigned = B64.encodeToString("{\"typ\":\"JWT\",\"alg\":\"ES256\"}".getBytes(StandardCharsets.UTF_8))
                + "." + B64.encodeToString(json.writeValueAsString(claims).getBytes(StandardCharsets.UTF_8));
        byte[] signature = WebPushCrypto.es256(WebPushCrypto.privateKey(B64D.decode(properties.vapidPrivateKey())),
                unsigned.getBytes(StandardCharsets.US_ASCII));
        return unsigned + "." + B64.encodeToString(signature);
    }
}
