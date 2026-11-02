package br.com.cardapio.application.port.in.presence;

import br.com.cardapio.domain.presence.model.GeoFence;
import br.com.cardapio.domain.presence.model.IpRange;
import br.com.cardapio.domain.presence.model.PresenceRule;

import java.util.List;

/**
 * A configuração como a tela de gestão mostra.
 *
 * @param location       null se o local não foi marcado
 * @param currentNetwork a rede de quem está na tela agora, para o botão "Usar o IP desta rede"
 */
public record PresenceSettings(List<String> networks, String wifiName, GeoFence location, String currentNetwork) {

    public static PresenceSettings of(PresenceRule rule, String clientIp) {
        return new PresenceSettings(rule.networks().stream().map(IpRange::toString).toList(), rule.wifiName(),
                rule.location(), currentNetwork(clientIp));
    }

    private static String currentNetwork(String clientIp) {
        try {
            return IpRange.networkOf(clientIp).toString();
        } catch (RuntimeException e) {
            return null; // IP que não dá para usar (não deveria acontecer): a tela só não oferece o botão
        }
    }
}
