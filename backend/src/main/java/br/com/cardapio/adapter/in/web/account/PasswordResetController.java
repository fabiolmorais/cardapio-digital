package br.com.cardapio.adapter.in.web.account;

import br.com.cardapio.application.port.in.account.RequestPasswordResetUseCase;
import br.com.cardapio.application.port.in.account.ResetPasswordCommand;
import br.com.cardapio.application.port.in.account.ResetPasswordUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** "Esqueci a senha": pedir o link por e-mail e trocar a senha com ele. Sem login (é para quem não consegue entrar). */
@RestController
@RequestMapping("/api/v1/auth/password-reset")
@Tag(name = "Conta", description = "Cadastro e acesso de donos e gerentes")
class PasswordResetController {

    private final RequestPasswordResetUseCase requestReset;
    private final ResetPasswordUseCase reset;

    PasswordResetController(RequestPasswordResetUseCase requestReset, ResetPasswordUseCase reset) {
        this.requestReset = requestReset;
        this.reset = reset;
    }

    @PostMapping
    @Operation(operationId = "requestPasswordReset",
            summary = "Manda o link para redefinir a senha, se houver conta com este e-mail")
    @ApiResponse(responseCode = "202", description = "Pedido recebido (a mesma resposta com ou sem conta)")
    @ApiResponse(responseCode = "400", description = "E-mail fora do formato",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    ResponseEntity<Void> request(@Valid @RequestBody PasswordResetRequest request) {
        requestReset.request(request.email());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/confirm")
    @Operation(operationId = "confirmPasswordReset", summary = "Troca a senha com o token do link e encerra as sessões")
    @ApiResponse(responseCode = "204", description = "Senha trocada")
    @ApiResponse(responseCode = "400", description = "Senha fraca",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "422", description = "Link vencido, já usado ou inexistente",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    ResponseEntity<Void> confirm(@Valid @RequestBody PasswordResetConfirmRequest request) {
        reset.reset(new ResetPasswordCommand(request.token(), request.newPassword()));
        return ResponseEntity.noContent().build();
    }
}
