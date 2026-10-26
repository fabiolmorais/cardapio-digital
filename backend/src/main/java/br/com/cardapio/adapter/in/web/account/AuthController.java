package br.com.cardapio.adapter.in.web.account;

import br.com.cardapio.application.port.in.account.LoginUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Conta", description = "Cadastro e acesso de donos e gerentes")
class AuthController {

    private final LoginUseCase login;

    AuthController(LoginUseCase login) {
        this.login = login;
    }

    @PostMapping("/login")
    @Operation(operationId = "login", summary = "Entra com e-mail e senha e recebe o token de acesso")
    @ApiResponse(responseCode = "200", description = "Login feito")
    @ApiResponse(responseCode = "401", description = "E-mail ou senha incorretos",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return LoginResponse.from(login.login(request.toCommand()));
    }
}
