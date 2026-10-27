package br.com.cardapio.application.port.in.account;

import br.com.cardapio.domain.account.model.MembershipId;

import java.util.Objects;

/**
 * @param deviceToken o token do aparelho, do cookie: é ele que diz de que restaurante é o login
 * @param memberId    quem tocou no próprio nome na tela
 */
public record PinLoginCommand(String deviceToken, MembershipId memberId, String pin) {

    public PinLoginCommand {
        Objects.requireNonNull(memberId);
    }

    /** Nem o token do aparelho nem o PIN aparecem em log. */
    @Override
    public String toString() {
        return "PinLoginCommand[memberId=" + memberId + "]";
    }
}
