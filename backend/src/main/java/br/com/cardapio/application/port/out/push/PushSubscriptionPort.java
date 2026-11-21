package br.com.cardapio.application.port.out.push;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.UserId;
import br.com.cardapio.domain.push.model.PushSubscription;

import java.net.URI;
import java.util.Collection;
import java.util.List;

public interface PushSubscriptionPort {

    /**
     * Grava a inscrição. O endpoint é único: se o mesmo navegador já estava inscrito (outra pessoa
     * da equipe usou o celular antes, por exemplo), a inscrição antiga sai.
     */
    void replace(PushSubscription subscription);

    /** Tira a inscrição deste navegador, se ela é desta pessoa neste restaurante. false: não havia. */
    boolean delete(EstablishmentId establishmentId, UserId userId, URI endpoint);

    /** Os navegadores de quem está ATIVO na equipe com estes papéis (o desligado para de receber). */
    List<PushSubscription> findForRoles(EstablishmentId establishmentId, List<String> roles);

    /** Os navegadores destas pessoas neste restaurante (o resumo diário vai para quem escolheu recebê-lo). */
    List<PushSubscription> findForUsers(EstablishmentId establishmentId, Collection<UserId> users);

    /** O serviço de push disse que a inscrição não existe mais (o navegador desinstalou ou recusou). */
    void deleteGone(URI endpoint);
}
