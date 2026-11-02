package br.com.cardapio.application.port.out.call;

import br.com.cardapio.domain.call.model.Call;
import br.com.cardapio.domain.tablesession.model.PresenceMethod;

/**
 * O chamado com o que as telas mostram junto: a mesa, o setor e o nome de quem aceitou.
 *
 * @param acceptedByName null enquanto ninguém aceitou
 * @param presenceMethod como a sessão que chamou provou a presença; null no pedido de liberação
 */
public record CallView(Call call, String tableLabel, String sectorName, String acceptedByName,
                       PresenceMethod presenceMethod) {
}
