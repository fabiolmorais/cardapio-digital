package br.com.cardapio.adapter.in.web.account;

import br.com.cardapio.adapter.in.web.shared.AuthenticatedUser;
import br.com.cardapio.application.port.in.account.GetCurrentAccountUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me")
@Tag(name = "Conta")
class MeController {

    private final GetCurrentAccountUseCase currentAccount;

    MeController(GetCurrentAccountUseCase currentAccount) {
        this.currentAccount = currentAccount;
    }

    @GetMapping
    @Operation(operationId = "getMe", summary = "Dados de quem está logado", security = @SecurityRequirement(name = "bearer"))
    MeResponse me(@AuthenticationPrincipal Jwt jwt) {
        AuthenticatedUser user = AuthenticatedUser.from(jwt);
        return MeResponse.from(currentAccount.current(user.userId(), user.establishmentId()));
    }
}
