package br.com.cardapio.application.service.account;

import br.com.cardapio.application.port.in.account.RequestPasswordResetUseCase;
import br.com.cardapio.application.port.out.account.LoadUserPort;
import br.com.cardapio.application.port.out.account.PasswordResetPort;
import br.com.cardapio.application.port.out.email.EmailMessage;
import br.com.cardapio.application.port.out.email.EmailSenderPort;
import br.com.cardapio.application.port.out.security.RandomTokenGenerator;
import br.com.cardapio.domain.account.model.PasswordResetToken;
import br.com.cardapio.domain.account.model.User;
import br.com.cardapio.domain.shared.model.Email;
import br.com.cardapio.domain.shared.model.TokenHash;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

public class RequestPasswordResetService implements RequestPasswordResetUseCase {

    private final LoadUserPort users;
    private final PasswordResetPort resets;
    private final RandomTokenGenerator tokens;
    private final EmailSenderPort email;
    private final Clock clock;
    private final String resetUrl;

    public RequestPasswordResetService(LoadUserPort users, PasswordResetPort resets, RandomTokenGenerator tokens,
                                       EmailSenderPort email, Clock clock, String publicUrl) {
        this.users = users;
        this.resets = resets;
        this.tokens = tokens;
        this.email = email;
        this.clock = clock;
        this.resetUrl = publicUrl + "/redefinir-senha?token=";
    }

    @Override
    @Transactional
    public void request(String rawEmail) {
        Email address = new Email(rawEmail);
        Instant now = clock.instant();
        // Sem conta, sem senha (equipe de PIN) ou desligada: nada, e a mesma resposta de sempre.
        User user = users.findByEmail(address).filter(u -> u.active() && u.hasPasswordLogin()).orElse(null);
        if (user == null || resets.countSince(user.id(), now.minus(Duration.ofHours(1)))
                >= PasswordResetToken.MAX_PER_HOUR) {
            return;
        }
        String token = tokens.generate();
        resets.save(PasswordResetToken.issue(user.id(), TokenHash.of(token), now));
        email.send(new EmailMessage(address, "Redefina sua senha",
                "Olá, " + user.name() + "!\n\n"
                        + "Para criar uma senha nova, abra o link abaixo. Ele vale por 30 minutos e uma vez só:\n"
                        + resetUrl + token + "\n\n"
                        + "Se você não pediu, ignore este e-mail: a sua senha continua a mesma."));
    }
}
