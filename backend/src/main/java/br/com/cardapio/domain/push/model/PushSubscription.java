package br.com.cardapio.domain.push.model;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.UserId;
import br.com.cardapio.domain.shared.exception.InvalidValueException;

import java.net.URI;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;

/**
 * Um navegador da equipe inscrito nas notificações: para onde mandar (o endpoint do serviço de
 * push do Google, da Apple, da Mozilla…) e as duas chaves que o navegador gerou para cifrar a
 * mensagem (RFC 8291). Nada disso é dado pessoal: é o endereço de um aparelho.
 *
 * @param p256dh a chave pública do navegador (ponto P-256 sem compressão, 65 bytes, em base64url)
 * @param auth   o segredo de autenticação do navegador (16 bytes, em base64url)
 */
public record PushSubscription(
        PushSubscriptionId id,
        EstablishmentId establishmentId,
        UserId userId,
        URI endpoint,
        String p256dh,
        String auth,
        Instant createdAt
) {

    public static final int MAX_ENDPOINT = 1000;

    public PushSubscription {
        Objects.requireNonNull(id);
        Objects.requireNonNull(establishmentId);
        Objects.requireNonNull(userId);
        Objects.requireNonNull(createdAt);
        if (endpoint == null || endpoint.getHost() == null || endpoint.toString().length() > MAX_ENDPOINT) {
            throw new InvalidValueException("O endereço de notificação do navegador não é válido.");
        }
        p256dh = requireKey(p256dh, 65, "a chave do navegador");
        auth = requireKey(auth, 16, "o segredo do navegador");
        if (Base64.getUrlDecoder().decode(p256dh)[0] != 0x04) {
            throw new InvalidValueException("A chave do navegador não é um ponto P-256 sem compressão.");
        }
    }

    public static PushSubscription create(EstablishmentId establishmentId, UserId userId, URI endpoint,
                                          String p256dh, String auth, Instant now) {
        return new PushSubscription(PushSubscriptionId.newId(), establishmentId, userId, endpoint, p256dh, auth, now);
    }

    /** A chave em base64url (com ou sem o "=" no fim), com o tamanho certo depois de decodificada. */
    private static String requireKey(String value, int bytes, String what) {
        String clean = value == null ? "" : value.strip().replace("=", "");
        try {
            if (Base64.getUrlDecoder().decode(clean).length == bytes) {
                return clean;
            }
        } catch (IllegalArgumentException e) {
            // cai na mensagem abaixo
        }
        throw new InvalidValueException("O navegador mandou " + what + " num formato que não é o do web push.");
    }
}
