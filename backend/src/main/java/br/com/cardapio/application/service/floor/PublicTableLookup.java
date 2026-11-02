package br.com.cardapio.application.service.floor;

import br.com.cardapio.application.port.out.floor.DiningTablePort;
import br.com.cardapio.application.port.out.floor.PublicTableView;
import br.com.cardapio.domain.floor.exception.TableNotFoundException;
import br.com.cardapio.domain.floor.model.TableToken;

/**
 * A busca que toda tela pública começa fazendo: o token da URL abre uma mesa ativa, de um
 * restaurante ativo? Senão, 404. É pública porque o cardápio e a sessão da mesa (outros
 * pacotes) começam do mesmo jeito.
 */
public final class PublicTableLookup {

    private PublicTableLookup() {
    }

    public static PublicTableView resolve(DiningTablePort tables, String token) {
        TableToken tableToken;
        try {
            tableToken = new TableToken(token);
        } catch (IllegalArgumentException | NullPointerException e) {
            // Token fora do formato: nem vale ir ao banco. Mesma resposta de um token que não existe.
            throw new TableNotFoundException();
        }
        return tables.findActiveByToken(tableToken).orElseThrow(TableNotFoundException::new);
    }
}
