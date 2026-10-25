package br.com.cardapio.adapter.out.security;

import br.com.cardapio.application.port.out.account.PasswordHasher;
import br.com.cardapio.domain.account.model.PasswordHash;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
class BCryptPasswordHasher implements PasswordHasher {

    /** Custo 12: em torno de 0,2 s por senha, caro para quem tenta adivinhar e aceitável no login. */
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);

    @Override
    public PasswordHash hash(String rawPassword) {
        return new PasswordHash(encoder.encode(rawPassword));
    }

    @Override
    public boolean matches(String rawPassword, PasswordHash hash) {
        return encoder.matches(rawPassword, hash.value());
    }
}
