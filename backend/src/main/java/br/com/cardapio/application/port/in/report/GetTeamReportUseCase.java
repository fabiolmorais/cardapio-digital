package br.com.cardapio.application.port.in.report;

import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.application.port.shared.ResultPage;
import br.com.cardapio.domain.report.model.RatingComment;

public interface GetTeamReportUseCase {

    TeamReport get(ReportQuery query);

    /** Os comentários das avaliações do período (REL-83), paginados; só as notas baixas, se pedido. */
    ResultPage<RatingComment> comments(ReportQuery query, boolean onlyLow, PageQuery page);
}
