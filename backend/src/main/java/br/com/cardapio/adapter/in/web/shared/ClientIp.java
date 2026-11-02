package br.com.cardapio.adapter.in.web.shared;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * O IP de quem chamou, para a trava de presença ("o celular está no Wi-Fi do restaurante?").
 *
 * Cuidado: o cabeçalho X-Forwarded-For pode ser escrito por qualquer um, e um celular de fora
 * poderia "dizer" que tem o IP do restaurante. Por isso, em produção, o IP vem de um cabeçalho
 * que só o proxy da hospedagem escreve (no Fly.io, o Fly-Client-IP), configurado em
 * app.presence.client-ip-header. Sem ele (desenvolvimento), vale o endereço da conexão.
 */
@Component
public class ClientIp {

    private final String trustedHeader;

    ClientIp(@Value("${app.presence.client-ip-header:}") String trustedHeader) {
        this.trustedHeader = trustedHeader;
    }

    public String of(HttpServletRequest request) {
        if (!trustedHeader.isBlank()) {
            String value = request.getHeader(trustedHeader);
            return value == null ? null : value.strip();
        }
        return request.getRemoteAddr();
    }
}
