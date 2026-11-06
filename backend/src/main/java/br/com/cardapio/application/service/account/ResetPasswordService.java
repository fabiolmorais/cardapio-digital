package br.com.cardapio.application.service.account;

import br.com.cardapio.application.port.in.account.ResetPasswordCommand;
import br.com.cardapio.application.port.in.account.ResetPasswordUseCase;
import br.com.cardapio.application.port.out.account.PasswordResetPort;
import br.com.cardapio.application.port.out.security.PasswordHasher;
import br.com.cardapio.domain.account.exception.PasswordResetLinkInvalidException;
import br.com.cardapio.domain.account.model.PasswordPolicy;
import br.com.cardapio.domain.account.model.PasswordResetToken;
import br.com.cardapio.domain.shared.model.TokenHash;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

public class ResetPasswordService implements ResetPasswordUseCase {

    private final PasswordResetPort resets;
    private final PasswordHasher hasher;
    private final Clock clock;

    public ResetPasswordService(PasswordResetPort resets, PasswordHasher hasher, Clock clock) {
        this.resets = resets;
        this.hasher = hasher;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void reset(ResetPasswordCommand command) {
        // A senha nova primeiro: uma senha fraca não gasta o link, e a pessoa tenta de novo.
        PasswordPolicy.check(command.newPassword());
        Instant now = clock.instant();
        if (command.token() == null || command.token().isBlank()) {
            throw new PasswordResetLinkInvalidException();
        }
        PasswordResetToken used = resets.findByHash(TokenHash.of(command.token()))
                .orElseThrow(PasswordResetLinkInvalidException::new)
                .use(now);
        resets.save(used);
        resets.changePassword(used.userId(), hasher.hash(command.newPassword()), now);
    }
}
