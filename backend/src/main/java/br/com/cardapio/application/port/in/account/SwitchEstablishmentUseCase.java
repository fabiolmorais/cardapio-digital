package br.com.cardapio.application.port.in.account;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.UserId;

/**
 * Trocar de unidade (RF31): encerra a sessão de agora e abre outra, na unidade escolhida, se a
 * pessoa tem vínculo ativo com ela. Sem vínculo, 404, como qualquer recurso de outro restaurante.
 */
public interface SwitchEstablishmentUseCase {

    /**
     * @param rawRefreshToken o refresh token do cookie (pode faltar); a família dele é encerrada
     */
    LoginResult switchTo(UserId userId, String rawRefreshToken, EstablishmentId target);
}
