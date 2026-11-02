package br.com.cardapio.application.port.in.presence;

import br.com.cardapio.domain.presence.model.GeoPosition;

/**
 * "Estou no salão?", ao abrir a mesa e de novo quando o cliente toca em "Usar minha localização".
 *
 * @param clientIp o IP de quem chamou, como o adaptador web o recebeu
 * @param position null na primeira conferência (só a rede); com a localização, na segunda
 */
public record CheckPresenceCommand(String tableToken, String sessionToken, String clientIp, GeoPosition position) {
}
