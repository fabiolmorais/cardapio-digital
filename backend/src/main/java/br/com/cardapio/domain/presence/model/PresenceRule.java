package br.com.cardapio.domain.presence.model;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.shared.exception.InvalidValueException;
import br.com.cardapio.domain.tablesession.model.PresenceMethod;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Como o restaurante confere que o celular está no salão. Sem nada configurado (o padrão), só a
 * liberação pelo garçom vale, que é o modo mais seguro (especificação).
 *
 * @param wifiName o nome da rede, para a tela da mesa; null se o dono não informou
 * @param location         null se o dono não marcou o local
 * @param sessionIdleLimit quanto tempo sem uso a sessão da mesa aguenta (2 horas, se o dono não mudou)
 */
public record PresenceRule(
        PresenceRuleId id,
        EstablishmentId establishmentId,
        List<IpRange> networks,
        String wifiName,
        GeoFence location,
        Duration sessionIdleLimit,
        Instant updatedAt
) {

    public static final int MAX_NETWORKS = 10;
    public static final int MAX_WIFI_NAME = 60;
    public static final Duration DEFAULT_SESSION_IDLE_LIMIT = Duration.ofHours(2);
    /** Menos que isso, o cliente perde a sessão no meio do jantar; mais, o QR fotografado vale tempo demais. */
    public static final Duration MIN_SESSION_IDLE_LIMIT = Duration.ofMinutes(30);
    public static final Duration MAX_SESSION_IDLE_LIMIT = Duration.ofHours(6);

    public PresenceRule {
        Objects.requireNonNull(id);
        Objects.requireNonNull(establishmentId);
        Objects.requireNonNull(updatedAt);
        Objects.requireNonNull(sessionIdleLimit);
        if (sessionIdleLimit.compareTo(MIN_SESSION_IDLE_LIMIT) < 0
                || sessionIdleLimit.compareTo(MAX_SESSION_IDLE_LIMIT) > 0) {
            throw new InvalidValueException("A sessão da mesa deve durar de 30 minutos a 6 horas sem uso");
        }
        // Sem repetidas, na ordem em que vieram.
        networks = List.copyOf(new LinkedHashSet<>(Objects.requireNonNull(networks)));
        if (networks.size() > MAX_NETWORKS) {
            throw new InvalidValueException("Cadastre até " + MAX_NETWORKS + " redes");
        }
        wifiName = wifiName == null || wifiName.isBlank() ? null : wifiName.strip();
        if (wifiName != null && wifiName.length() > MAX_WIFI_NAME) {
            throw new InvalidValueException("O nome da rede pode ter até " + MAX_WIFI_NAME + " caracteres");
        }
    }

    /** O restaurante que nunca configurou nada: só a liberação pelo garçom. */
    public static PresenceRule empty(EstablishmentId establishmentId, Instant now) {
        return new PresenceRule(PresenceRuleId.newId(), establishmentId, List.of(), null, null,
                DEFAULT_SESSION_IDLE_LIMIT, now);
    }

    public PresenceRule update(List<IpRange> newNetworks, String newWifiName, GeoFence newLocation, Instant now) {
        return new PresenceRule(id, establishmentId, newNetworks, newWifiName, newLocation, sessionIdleLimit, now);
    }

    /** Vale para as sessões que abrirem daqui em diante; as abertas seguem com o prazo com que nasceram. */
    public PresenceRule withSessionIdleLimit(Duration limit, Instant now) {
        return new PresenceRule(id, establishmentId, networks, wifiName, location, limit, now);
    }

    /**
     * Confere a presença, na ordem da especificação: primeiro a rede (sem pedir nada ao
     * cliente), depois a localização, se ela veio. Vazio: nenhuma das duas provou.
     *
     * @param position null se o celular ainda não mandou a localização
     */
    public Optional<PresenceMethod> check(String clientIp, GeoPosition position) {
        if (clientIp != null && networks.stream().anyMatch(range -> range.contains(clientIp))) {
            return Optional.of(PresenceMethod.WIFI);
        }
        if (location != null && position != null && location.admits(position)) {
            return Optional.of(PresenceMethod.GPS);
        }
        return Optional.empty();
    }
}
