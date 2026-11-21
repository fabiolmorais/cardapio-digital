package br.com.cardapio.adapter.in.web.report;

import br.com.cardapio.adapter.in.web.shared.AuthenticatedUser;
import br.com.cardapio.application.port.in.report.GetReportContextUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Os relatórios (RF24, ADR 0013): só o dono e o gerente. */
@RestController
@RequestMapping("/api/v1/reports")
@Tag(name = "Relatórios", description = "Vendas, cardápio, cozinha, equipe e salão, por período")
@SecurityRequirement(name = "bearer")
@PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
class ReportController {

    private final GetReportContextUseCase context;

    ReportController(GetReportContextUseCase context) {
        this.context = context;
    }

    @GetMapping("/context")
    @Operation(operationId = "getReportContext",
            summary = "O plano, o fuso, o histórico liberado e desde quando há dados")
    @ApiResponse(responseCode = "200", description = "O contexto dos relatórios (também no Essencial)")
    ReportContextResponse context(@AuthenticationPrincipal Jwt jwt) {
        return ReportContextResponse.from(context.get(AuthenticatedUser.from(jwt).establishmentId()));
    }
}
