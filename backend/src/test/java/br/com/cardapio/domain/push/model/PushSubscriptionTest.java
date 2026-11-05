package br.com.cardapio.domain.push.model;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.UserId;
import br.com.cardapio.domain.shared.exception.InvalidValueException;
import br.com.cardapio.support.TestVapidKeys;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Instant;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PushSubscriptionTest {

    private static final URI ENDPOINT = URI.create("https://fcm.googleapis.com/fcm/send/abc");
    private static final String AUTH = Base64.getUrlEncoder().withoutPadding().encodeToString(new byte[16]);

    private static PushSubscription subscription(URI endpoint, String p256dh, String auth) {
        return PushSubscription.create(EstablishmentId.newId(), UserId.newId(), endpoint, p256dh, auth,
                Instant.parse("2026-11-05T22:00:00Z"));
    }

    @Test
    void theBrowserKeysAreTheOnesOfTheWebPush() {
        String p256dh = TestVapidKeys.generate().publicKey();

        // Com o "=" no fim (alguns navegadores mandam assim), fica sem.
        assertThat(subscription(ENDPOINT, p256dh, AUTH + "==").auth()).isEqualTo(AUTH);
        assertThat(subscription(ENDPOINT, p256dh + "=", AUTH).p256dh()).isEqualTo(p256dh);
    }

    @Test
    void wrongKeysOrEndpointAreRefused() {
        String p256dh = TestVapidKeys.generate().publicKey();

        assertThatThrownBy(() -> subscription(ENDPOINT, "curta", AUTH)).isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> subscription(ENDPOINT, p256dh, "não é base64!"))
                .isInstanceOf(InvalidValueException.class);
        // 65 bytes, mas sem o 0x04 de ponto sem compressão.
        assertThatThrownBy(() -> subscription(ENDPOINT,
                Base64.getUrlEncoder().withoutPadding().encodeToString(new byte[65]), AUTH))
                .isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> subscription(URI.create("mailto:x@y.z"), p256dh, AUTH))
                .isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> subscription(URI.create("https://fcm.googleapis.com/" + "a".repeat(1000)), p256dh,
                AUTH)).isInstanceOf(InvalidValueException.class);
    }
}
