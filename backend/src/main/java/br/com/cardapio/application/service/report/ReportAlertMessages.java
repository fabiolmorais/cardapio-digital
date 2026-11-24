package br.com.cardapio.application.service.report;

import br.com.cardapio.domain.push.model.PushMessage;
import br.com.cardapio.domain.report.model.AlertFinding;
import br.com.cardapio.domain.report.model.AlertSettings;

/**
 * O texto de cada alerta no celular (REL-78). Curto, com a mesa do caso mais antigo; tocar abre onde
 * se resolve (a fila da cozinha, o Ao vivo, a comanda). A tag por tipo faz o celular trocar o aviso
 * antigo pelo novo, em vez de empilhar.
 */
final class ReportAlertMessages {

    private ReportAlertMessages() {
    }

    static PushMessage of(AlertFinding f, AlertSettings settings) {
        int more = f.count() - 1;
        return switch (f.type()) {
            case KITCHEN -> new PushMessage("Cozinha atrasando",
                    (f.count() == 1 ? "1 item" : f.count() + " itens") + " em preparo há mais de "
                            + settings.kitchenMinutes() + " min. O mais antigo é da mesa " + f.oldestTable() + ".",
                    "/painel/cozinha", "alerta-cozinha");
            case CALL -> new PushMessage("Chamado sem resposta",
                    "Mesa " + f.oldestTable() + " chamando há mais de " + settings.callMinutes() + " min"
                            + (more == 0 ? "." : more == 1 ? " e mais 1 mesa." : " e mais " + more + " mesas."),
                    "/painel", "alerta-chamado");
            case TAB -> new PushMessage("Comanda aberta há muito tempo",
                    "Mesa " + f.oldestTable() + " com a comanda aberta há mais de " + settings.tabHours() + " h"
                            + (more == 0 ? "." : more == 1 ? " e mais 1 comanda." : " e mais " + more + " comandas."),
                    f.tabId().map(id -> "/painel/comandas/" + id.value()).orElse("/painel/comandas"),
                    "alerta-comanda");
        };
    }
}
