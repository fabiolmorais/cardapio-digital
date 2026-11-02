package br.com.cardapio.application.port.in.presence;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.presence.model.GeoFence;

import java.util.List;
import java.util.Objects;

/**
 * A configuração inteira, como a tela salva (troca tudo de uma vez).
 *
 * @param networks os IPs ou faixas, em texto ("189.45.212.18", "2804:14c:6a::/64")
 * @param location null para não usar a localização
 * @param clientIp o IP de quem está salvando (para a resposta trazer a rede atual)
 */
public record UpdatePresenceRuleCommand(EstablishmentId establishmentId, List<String> networks, String wifiName,
                                        GeoFence location, String clientIp) {

    public UpdatePresenceRuleCommand {
        Objects.requireNonNull(establishmentId);
        networks = List.copyOf(Objects.requireNonNull(networks));
    }
}
