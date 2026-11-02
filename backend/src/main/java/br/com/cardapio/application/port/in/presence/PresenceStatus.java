package br.com.cardapio.application.port.in.presence;

import br.com.cardapio.domain.tablesession.model.PresenceMethod;

/**
 * O que a página da mesa precisa para decidir o que mostrar.
 *
 * @param method          como foi provada; null enquanto não foi
 * @param locationEnabled o restaurante marcou o local: vale a pena oferecer "Usar minha localização"
 * @param wifiName        o nome da rede do salão, para a tela sugerir; null se não informado
 * @param blocked         a equipe bloqueou este celular: nem a presença o libera
 */
public record PresenceStatus(boolean present, PresenceMethod method, boolean locationEnabled, String wifiName,
                             boolean blocked) {
}
