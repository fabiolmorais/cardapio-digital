package br.com.cardapio.application.port.in.call;

/** created = false: a mesa já tinha esse chamado aberto, e é ele que volta (tocar de novo não duplica). */
public record RequestedCall(TableCall call, boolean created) {
}
