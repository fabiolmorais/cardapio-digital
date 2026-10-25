package br.com.cardapio.adapter.in.web.account;

import br.com.cardapio.application.port.in.account.SignupResult;
import br.com.cardapio.application.port.in.account.SignupUseCase;
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

import java.net.URI;

@RestController
@RequestMapping("/api/v1/signup")
@Tag(name = "Conta", description = "Cadastro e acesso de donos e gerentes")
class SignupController {

    private final SignupUseCase signup;

    SignupController(SignupUseCase signup) {
        this.signup = signup;
    }

    @PostMapping
    @Operation(operationId = "signup", summary = "Cria a conta do restaurante (teste grátis)")
    @ApiResponse(responseCode = "201", description = "Conta criada")
    @ApiResponse(responseCode = "400", description = "Dados inválidos",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "E-mail já cadastrado",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    ResponseEntity<SignupResponse> signup(@Valid @RequestBody SignupRequest request) {
        SignupResult result = signup.signup(request.toCommand());
        URI location = URI.create("/api/v1/establishments/" + result.establishmentId().value());
        return ResponseEntity.created(location).body(SignupResponse.from(result));
    }
}
