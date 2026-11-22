package br.com.cardapio.domain.report.model;

import java.util.List;

/** Quantas comandas esquecidas o período teve e as mais recentes. */
public record ForgottenTabs(int count, List<ForgottenTab> latest) {

    public ForgottenTabs {
        latest = List.copyOf(latest);
    }
}
