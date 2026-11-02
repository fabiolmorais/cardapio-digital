package br.com.cardapio.application.port.in.presence;

public interface UpdatePresenceRuleUseCase {

    PresenceSettings update(UpdatePresenceRuleCommand command);
}
