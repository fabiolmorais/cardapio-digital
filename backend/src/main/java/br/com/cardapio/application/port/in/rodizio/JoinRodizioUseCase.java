package br.com.cardapio.application.port.in.rodizio;

import br.com.cardapio.domain.rodizio.model.RodizioParticipation;

/**
 * "Vou participar": o celular que chegou depois da ativação pede para entrar no rodízio da mesa.
 * O garçom é chamado ("Quero o rodízio") e, ao ajustar as pessoas, inclui este celular.
 */
public interface JoinRodizioUseCase {

    /** Devolve como o celular ficou: REQUESTED (esperando o garçom) ou JOINED (já estava dentro). */
    RodizioParticipation join(String tableToken, String sessionToken);
}
