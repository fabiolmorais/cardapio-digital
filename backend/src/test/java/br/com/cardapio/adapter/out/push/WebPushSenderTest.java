package br.com.cardapio.adapter.out.push;

import br.com.cardapio.application.port.out.push.PushSenderPort.Delivery;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.UserId;
import br.com.cardapio.domain.push.model.PushMessage;
import br.com.cardapio.domain.push.model.PushSubscription;
import br.com.cardapio.support.FakePushService;
import br.com.cardapio.support.TestVapidKeys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.Signature;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * O envio de ponta a ponta, sem Spring: o serviço de push é falso, e o teste faz o papel do
 * navegador, decifrando a mensagem como o RFC 8291 manda e conferindo a assinatura do VAPID.
 */
class WebPushSenderTest {

    private static final Instant NOW = Instant.parse("2026-11-05T22:00:00Z");

    private final TestVapidKeys vapid = TestVapidKeys.generate();
    private final TestVapidKeys browser = TestVapidKeys.generate();
    private final byte[] auth = new byte[16];
    private final FakePushService push;
    private final JsonMapper json = JsonMapper.builder().build();
    private final WebPushSender sender;

    WebPushSenderTest() throws Exception {
        new SecureRandom().nextBytes(auth);
        push = new FakePushService();
        sender = new WebPushSender(new PushProperties(vapid.publicKey(), vapid.privateKey(), "mailto:dono@teste.com",
                List.of("http://127.0.0.1", "https://*.notify.windows.com")), json, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @AfterEach
    void stop() {
        push.close();
    }

    private PushSubscription subscription(String endpoint) {
        return PushSubscription.create(EstablishmentId.newId(), UserId.newId(), URI.create(endpoint),
                browser.publicKey(), Base64.getUrlEncoder().withoutPadding().encodeToString(auth), NOW);
    }

    @Test
    void theBrowserDecryptsTheMessageAndTheVapidSignatureChecks() throws Exception {
        Delivery delivery = sender.send(subscription(push.endpoint("ana")),
                new PushMessage("Mesa 04", "Chamou o garçom", "/painel", "call-1"));

        assertThat(delivery).isEqualTo(Delivery.SENT);
        FakePushService.Received got = push.received.poll(5, TimeUnit.SECONDS);
        assertThat(got).isNotNull();
        assertThat(got.headers()).containsEntry("content-encoding", "aes128gcm").containsEntry("urgency", "high")
                .containsEntry("ttl", "600");

        JsonNode message = json.readTree(decrypt(got.body()));
        assertThat(message.get("title").asString()).isEqualTo("Mesa 04");
        assertThat(message.get("body").asString()).isEqualTo("Chamou o garçom");
        assertThat(message.get("tag").asString()).isEqualTo("call-1");

        // Authorization: vapid t=<JWT>, k=<chave pública>
        String authorization = got.headers().get("authorization");
        assertThat(authorization).startsWith("vapid t=").endsWith(", k=" + vapid.publicKey());
        String[] jwt = authorization.substring("vapid t=".length(), authorization.indexOf(',')).split("\\.");
        JsonNode claims = json.readTree(Base64.getUrlDecoder().decode(jwt[1]));
        assertThat(claims.get("aud").asString()).isEqualTo(push.endpoint("").replaceAll("/$", ""));
        assertThat(claims.get("exp").asLong()).isEqualTo(NOW.plusSeconds(12 * 3600).getEpochSecond());
        Signature check = Signature.getInstance("SHA256withECDSAinP1363Format");
        check.initVerify(vapid.pair().getPublic());
        check.update((jwt[0] + "." + jwt[1]).getBytes(StandardCharsets.US_ASCII));
        assertThat(check.verify(Base64.getUrlDecoder().decode(jwt[2]))).isTrue();
    }

    @Test
    void goneOnlyFromKnownServicesAndOffWithoutKeys() {
        assertThat(sender.send(subscription(push.endpoint("gone/ana")),
                new PushMessage("Mesa 04", "Pediu a conta", "/painel", "call-2"))).isEqualTo(Delivery.GONE);
        push.received.clear();

        assertThat(sender.accepts(URI.create("https://wns2-bl2p.notify.windows.com/w/?token=1"))).isTrue();
        assertThat(sender.accepts(URI.create("https://notify.windows.com.evil.example/w"))).isFalse();
        assertThat(sender.accepts(URI.create("https://127.0.0.1/push"))).isFalse(); // o esquema também conta
        assertThat(sender.accepts(URI.create("http://169.254.169.254/latest/meta-data"))).isFalse();

        WebPushSender off = new WebPushSender(new PushProperties("", "", "mailto:x@y.z", List.of()), json,
                Clock.systemUTC());
        assertThat(off.enabled()).isFalse();
        assertThat(off.send(subscription(push.endpoint("ana")),
                new PushMessage("Mesa 04", "x", "/painel", "t"))).isEqualTo(Delivery.FAILED);
        assertThat(push.received).isEmpty();
    }

    /** O lado do navegador (RFC 8291, seção 3): a mesma derivação de chaves, com a chave privada dele. */
    private byte[] decrypt(byte[] body) throws Exception {
        ByteBuffer in = ByteBuffer.wrap(body);
        byte[] salt = new byte[16];
        in.get(salt);
        assertThat(in.getInt()).isEqualTo(4096);
        byte[] asPublic = new byte[in.get()];
        in.get(asPublic);
        byte[] ciphertext = new byte[in.remaining()];
        in.get(ciphertext);

        KeyAgreement agreement = KeyAgreement.getInstance("ECDH");
        agreement.init(browser.pair().getPrivate());
        agreement.doPhase(WebPushCrypto.publicKey(asPublic), true);
        byte[] uaPublic = Base64.getUrlDecoder().decode(browser.publicKey());
        byte[] prkKey = WebPushCrypto.hmac(auth, agreement.generateSecret());
        byte[] info = WebPushCrypto.concat("WebPush: info".getBytes(StandardCharsets.US_ASCII), new byte[] {0},
                uaPublic, asPublic);
        byte[] ikm = WebPushCrypto.hmac(prkKey, info, new byte[] {1});
        byte[] prk = WebPushCrypto.hmac(salt, ikm);
        byte[] cek = Arrays.copyOf(WebPushCrypto.hmac(prk,
                "Content-Encoding: aes128gcm".getBytes(StandardCharsets.US_ASCII), new byte[] {0, 1}), 16);
        byte[] nonce = Arrays.copyOf(WebPushCrypto.hmac(prk,
                "Content-Encoding: nonce".getBytes(StandardCharsets.US_ASCII), new byte[] {0, 1}), 12);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(cek, "AES"), new GCMParameterSpec(128, nonce));
        byte[] plain = cipher.doFinal(ciphertext);
        assertThat(plain[plain.length - 1]).isEqualTo((byte) 2); // o delimitador do último registro
        return Arrays.copyOf(plain, plain.length - 1);
    }
}
