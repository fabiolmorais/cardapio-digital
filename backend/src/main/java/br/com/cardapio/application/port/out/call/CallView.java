package br.com.cardapio.application.port.out.call;

import br.com.cardapio.domain.call.model.Call;

/**
 * O chamado com o que as telas mostram junto: a mesa, o setor e o nome de quem aceitou.
 *
 * @param acceptedByName null enquanto ninguém aceitou
 */
public record CallView(Call call, String tableLabel, String sectorName, String acceptedByName) {
}
