package br.com.cardapio.adapter.in.web.account;

import br.com.cardapio.application.port.in.account.LoginResult;
import br.com.cardapio.application.port.in.account.LoginUseCase;
import br.com.cardapio.application.port.in.account.LogoutUseCase;
import br.com.cardapio.application.port.in.account.RefreshSessionUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Conta", description = "Cadastro e acesso de donos e gerentes")
class AuthController {

    private final LoginUseCase login;
    private final RefreshSessionUseCase refreshSession;
    private final LogoutUseCase logout;
    private final RefreshTokenCookie cookie;

    AuthController(LoginUseCase login, RefreshSessionUseCase refreshSession, LogoutUseCase logout, RefreshTokenCookie cookie) {
        this.login = login;
        this.refreshSession = refreshSession;
        this.logout = logout;
        this.cookie = cookie;
    }

    @PostMapping("/login")
    @Operation(operationId = "login", summary = "Entra com e-mail e senha; devolve o token de acesso e grava o refresh token num cookie HttpOnly")
    @ApiResponse(responseCode = "200", description = "Login feito")
    @ApiResponse(responseCode = "401", description = "E-mail ou senha incorretos",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return withSession(login.login(request.toCommand()));
    }

    @PostMapping("/refresh")
    @Operation(operationId = "refreshSession", summary = "Troca o refresh token do cookie por um token de acesso novo")
    @ApiResponse(responseCode = "200", description = "Sessão renovada")
    @ApiResponse(responseCode = "401", description = "Sessão terminou; é preciso entrar de novo",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    ResponseEntity<LoginResponse> refresh(
            @CookieValue(name = RefreshTokenCookie.NAME, required = false) String refreshToken) {
        return withSession(refreshSession.refresh(refreshToken));
    }

    @PostMapping("/logout")
    @Operation(operationId = "logout", summary = "Encerra a sessão e apaga o cookie do refresh token")
    @ApiResponse(responseCode = "204", description = "Sessão encerrada")
    ResponseEntity<Void> logout(
            @CookieValue(name = RefreshTokenCookie.NAME, required = false) String refreshToken) {
        logout.logout(refreshToken);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookie.clear().toString())
                .build();
    }

    /** O refresh token vai só no cookie, nunca no corpo da resposta. */
    private ResponseEntity<LoginResponse> withSession(LoginResult result) {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.create(result.refreshToken(), result.refreshTokenExpiresAt()).toString())
                .body(LoginResponse.from(result));
    }
}
