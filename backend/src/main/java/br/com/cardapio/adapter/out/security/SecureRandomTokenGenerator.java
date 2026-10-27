package br.com.cardapio.adapter.out.security;

import br.com.cardapio.application.port.out.security.RandomCodeGenerator;
import br.com.cardapio.application.port.out.security.RandomTokenGenerator;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

@Component
class SecureRandomTokenGenerator implements RandomTokenGenerator, RandomCodeGenerator {

    /** 32 bytes = 256 bits: impossível de adivinhar, mesmo tentando bilhões de vezes. */
    private static final int TOKEN_BYTES = 32;

    private final SecureRandom random = new SecureRandom();

    @Override
    public String generate() {
        byte[] bytes = new byte[TOKEN_BYTES];
        random.nextBytes(bytes);
        // Base64 "de URL" e sem "=": cabe num cookie sem precisar escapar nada.
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    @Override
    public String digits(int length) {
        StringBuilder code = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            code.append(random.nextInt(10));
        }
        return code.toString();
    }
}
