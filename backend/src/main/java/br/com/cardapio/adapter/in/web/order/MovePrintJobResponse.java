package br.com.cardapio.adapter.in.web.order;

import br.com.cardapio.application.port.in.order.MovePrintJob;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(name = "MovePrintJob", description = "A via \"MUDOU DE MESA\" (ou \"JUNTOU\") para a impressora da cozinha")
record MovePrintJobResponse(
        @Schema(requiredMode = REQUIRED) UUID moveId,
        @Schema(requiredMode = REQUIRED, description = "A comanda se juntou à da mesa nova") boolean merged,
        @Schema(requiredMode = REQUIRED) String fromTableLabel,
        @Schema(requiredMode = REQUIRED) String toTableLabel,
        @Schema(requiredMode = REQUIRED, description = "O setor da mesa nova") String sectorName,
        @Schema(requiredMode = REQUIRED) Instant movedAt,
        @Schema(requiredMode = REQUIRED, description = "Os itens a caminho que saíram no papel com a mesa antiga; "
                + "vazio se outra estação já imprimiu ou se tudo já chegou")
        List<PrintJobResponse.Line> items
) {

    static MovePrintJobResponse from(MovePrintJob job) {
        return new MovePrintJobResponse(job.moveId().value(), job.merged(), job.fromTableLabel(), job.toTableLabel(),
                job.sectorName(), job.movedAt(), job.items().stream().map(PrintJobResponse::line).toList());
    }
}
