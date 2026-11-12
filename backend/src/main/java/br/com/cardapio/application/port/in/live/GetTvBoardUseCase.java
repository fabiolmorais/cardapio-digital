package br.com.cardapio.application.port.in.live;

public interface GetTvBoardUseCase {

    /** O restaurante vem do aparelho (o cookie), nunca de um parâmetro. */
    TvBoard get(String deviceToken);
}
