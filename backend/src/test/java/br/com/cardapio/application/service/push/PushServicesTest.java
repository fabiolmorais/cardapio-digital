package br.com.cardapio.application.service.push;

import br.com.cardapio.application.port.out.push.PushSenderPort;
import br.com.cardapio.application.port.out.push.PushSubscriptionPort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.UserId;
import br.com.cardapio.domain.call.event.CallChanged;
import br.com.cardapio.domain.call.model.CallId;
import br.com.cardapio.domain.call.model.CallStatus;
import br.com.cardapio.domain.call.model.CallType;
import br.com.cardapio.domain.floor.model.DiningTable;
import br.com.cardapio.domain.order.event.OrderChanged;
import br.com.cardapio.domain.order.model.OrderId;
import br.com.cardapio.domain.order.model.OrderStatus;
import br.com.cardapio.domain.push.exception.PushDisabledException;
import br.com.cardapio.domain.push.exception.PushSubscriptionNotFoundException;
import br.com.cardapio.domain.push.model.PushMessage;
import br.com.cardapio.domain.push.model.PushSubscription;
import br.com.cardapio.domain.shared.exception.InvalidValueException;
import br.com.cardapio.support.TablesByToken;
import br.com.cardapio.support.TestVapidKeys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Quem recebe a notificação, quando, e o que diz. O envio de verdade é testado no adaptador. */
class PushServicesTest {

    private static final Instant NOW = Instant.parse("2026-11-05T22:00:00Z");

    /** As inscrições na memória, com o papel de cada pessoa (o "ativo na equipe" fica no teste de integração). */
    static final class Subscriptions implements PushSubscriptionPort {
        final Map<URI, PushSubscription> saved = new LinkedHashMap<>();
        final Map<UserId, String> roles = new HashMap<>();
        final List<URI> gone = new ArrayList<>();

        @Override
        public void replace(PushSubscription s) { saved.put(s.endpoint(), s); }

        @Override
        public boolean delete(EstablishmentId establishmentId, UserId userId, URI endpoint) {
            PushSubscription s = saved.get(endpoint);
            if (s == null || !s.userId().equals(userId) || !s.establishmentId().equals(establishmentId)) {
                return false;
            }
            saved.remove(endpoint);
            return true;
        }

        @Override
        public List<PushSubscription> findForRoles(EstablishmentId establishmentId, List<String> wanted) {
            return saved.values().stream().filter(s -> s.establishmentId().equals(establishmentId)
                    && wanted.contains(roles.get(s.userId()))).toList();
        }

        @Override
        public void deleteGone(URI endpoint) {
            gone.add(endpoint);
            saved.remove(endpoint);
        }
    }

    /** O serviço de push na memória: aceita só o FCM e responde GONE para os endpoints que terminam em /gone. */
    static final class Sender implements PushSenderPort {
        boolean on = true;
        final List<Map.Entry<URI, PushMessage>> sent = new ArrayList<>();

        @Override
        public boolean enabled() { return on; }

        @Override
        public String publicKey() { return "chave-publica"; }

        @Override
        public boolean accepts(URI endpoint) { return "fcm.googleapis.com".equals(endpoint.getHost()); }

        @Override
        public Delivery send(PushSubscription s, PushMessage m) {
            sent.add(Map.entry(s.endpoint(), m));
            return s.endpoint().getPath().endsWith("/gone") ? Delivery.GONE : Delivery.SENT;
        }
    }

    private final Subscriptions subscriptions = new Subscriptions();
    private final Sender sender = new Sender();
    private final TablesByToken tables = new TablesByToken();
    private final ManagePushService manage = new ManagePushService(subscriptions, sender,
            Clock.fixed(NOW, ZoneOffset.UTC));
    private final NotifyStaffService notify = new NotifyStaffService(tables, subscriptions, sender);
    private final EstablishmentId sakura = EstablishmentId.newId();
    private final String p256dh = TestVapidKeys.generate().publicKey();
    private final String auth = Base64.getUrlEncoder().withoutPadding().encodeToString(new byte[16]);
    private DiningTable mesa04;

    @BeforeEach
    void aTable() {
        mesa04 = tables.given(sakura, "04", "qr-da-mesa-04-00000000000");
    }

    private UserId subscribed(String role, String path) {
        UserId user = UserId.newId();
        subscriptions.roles.put(user, role);
        manage.subscribe(sakura, user, URI.create("https://fcm.googleapis.com/fcm/send/" + path), p256dh, auth);
        return user;
    }

    private CallChanged call(CallType type, CallStatus status) {
        return new CallChanged(sakura, mesa04.id(), CallId.newId(), type, status, NOW);
    }

    @Test
    void aNewCallReachesTheFloorTeamButNotTheKitchen() {
        subscribed("OWNER", "dono");
        subscribed("STAFF", "carlos");
        subscribed("KITCHEN", "kenji");

        notify.notify(call(CallType.BILL, CallStatus.PENDING));

        assertThat(sender.sent).extracting(e -> e.getKey().getPath())
                .containsExactlyInAnyOrder("/fcm/send/dono", "/fcm/send/carlos");
        PushMessage message = sender.sent.getFirst().getValue();
        assertThat(message.title()).isEqualTo("Mesa 04");
        assertThat(message.body()).isEqualTo("Pediu a conta");
        assertThat(message.url()).isEqualTo("/painel");
        assertThat(message.tag()).startsWith("call-");
    }

    @Test
    void onlyWhatNeedsSomeoneAtTheTable() {
        subscribed("STAFF", "carlos");

        notify.notify(call(CallType.WAITER, CallStatus.IN_PROGRESS));
        notify.notify(call(CallType.WAITER, CallStatus.DONE));
        notify.notify(new OrderChanged(sakura, mesa04.id(), OrderId.newId(), OrderStatus.SENT, NOW));
        assertThat(sender.sent).isEmpty();

        notify.notify(new OrderChanged(sakura, mesa04.id(), OrderId.newId(), OrderStatus.PENDING_APPROVAL, NOW));
        assertThat(sender.sent).singleElement().satisfies(e -> {
            assertThat(e.getValue().body()).isEqualTo("Pedido para confirmar");
            assertThat(e.getValue().tag()).startsWith("order-");
        });
    }

    @Test
    void anotherRestaurantsTableOrAnEndedSubscriptionGetNothing() {
        subscribed("STAFF", "gone");
        DiningTable theirs = tables.given(EstablishmentId.newId(), "04", "qr-de-outra-casa-00000000");

        notify.notify(new CallChanged(sakura, theirs.id(), CallId.newId(), CallType.WAITER, CallStatus.PENDING, NOW));
        assertThat(sender.sent).isEmpty();

        notify.notify(call(CallType.WAITER, CallStatus.PENDING));
        assertThat(subscriptions.gone).containsExactly(URI.create("https://fcm.googleapis.com/fcm/send/gone"));
        assertThat(subscriptions.saved).isEmpty();
    }

    @Test
    void subscribingNeedsTheKeysAKnownServiceAndOwnership() {
        UserId carlos = subscribed("STAFF", "carlos");
        URI endpoint = URI.create("https://fcm.googleapis.com/fcm/send/carlos");

        assertThatThrownBy(() -> manage.subscribe(sakura, carlos, URI.create("http://169.254.169.254/x"), p256dh, auth))
                .isInstanceOf(InvalidValueException.class).hasMessageContaining("serviço de push");
        assertThatThrownBy(() -> manage.unsubscribe(sakura, UserId.newId(), endpoint))
                .isInstanceOf(PushSubscriptionNotFoundException.class);
        manage.unsubscribe(sakura, carlos, endpoint);
        assertThat(subscriptions.saved).isEmpty();

        sender.on = false;
        assertThat(manage.settings().enabled()).isFalse();
        assertThatThrownBy(() -> manage.subscribe(sakura, carlos, endpoint, p256dh, auth))
                .isInstanceOf(PushDisabledException.class);
        notify.notify(call(CallType.WAITER, CallStatus.PENDING));
        assertThat(sender.sent).isEmpty();
    }
}
