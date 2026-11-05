package br.com.cardapio.domain.push.model;

import java.util.Objects;

/**
 * O que aparece na notificação. Sem dado pessoal (CLAUDE.md): a mesa e o motivo.
 *
 * @param url para onde o toque na notificação leva (o painel)
 * @param tag uma notificação com a mesma tag substitui a anterior no aparelho, em vez de empilhar
 */
public record PushMessage(String title, String body, String url, String tag) {

    public PushMessage {
        Objects.requireNonNull(title);
        Objects.requireNonNull(body);
        Objects.requireNonNull(url);
        Objects.requireNonNull(tag);
    }
}
