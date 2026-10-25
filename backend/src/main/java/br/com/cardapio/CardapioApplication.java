package br.com.cardapio;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;

// Sem usuário padrão em memória: a autenticação é própria (etapa 2).
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class CardapioApplication {

    public static void main(String[] args) {
        SpringApplication.run(CardapioApplication.class, args);
    }

}
