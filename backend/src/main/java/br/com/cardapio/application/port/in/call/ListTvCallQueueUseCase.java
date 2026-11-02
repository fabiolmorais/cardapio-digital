package br.com.cardapio.application.port.in.call;

import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.application.port.shared.ResultPage;

public interface ListTvCallQueueUseCase {

    /** A mesma fila da equipe, para a TV vinculada, que se identifica pelo cookie (sem login). */
    ResultPage<StaffCall> list(String deviceToken, PageQuery page);
}
