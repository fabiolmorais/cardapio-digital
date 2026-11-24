package br.com.cardapio.application.port.in.rating;

import br.com.cardapio.domain.rating.model.RatingTag;

import java.util.Objects;
import java.util.Set;

/** A mesa avalia o atendimento (RF40), pelo celular, com o cookie da sessão. */
public interface RateServiceUseCase {

    /** O que o celular vê: se pode avaliar, se já avaliou e quem atendeu. */
    RatingOffer offer(String tableToken, String sessionToken);

    RatingOffer rate(RateServiceCommand command);

    /** O toque em "Avaliar no Google" (REL-84). */
    void clickGoogle(String tableToken, String sessionToken);

    record RateServiceCommand(String tableToken, String sessionToken, int stars, Set<RatingTag> tags,
                              String comment) {
        public RateServiceCommand {
            Objects.requireNonNull(tableToken);
            tags = tags == null ? Set.of() : Set.copyOf(tags);
        }
    }

    /**
     * @param available     pode avaliar agora (e ainda não avaliou)
     * @param stars         a nota dada; null se ainda não avaliou
     * @param googleInvite  já avaliou com 4 ou 5: mostra o convite ao Google
     * @param waiterName    o primeiro nome de quem mais atendeu; null se ninguém da equipe atendeu
     */
    record RatingOffer(boolean available, Integer stars, boolean googleInvite, String waiterName) {

        public static final RatingOffer NONE = new RatingOffer(false, null, false, null);
    }
}
