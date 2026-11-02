package br.com.cardapio.application.port.in.call;

public interface RequestCallUseCase {

    /** Chama o garçom ou pede a conta; com o chamado já aberto, devolve o mesmo (e troca a forma de pagamento, se mudou). */
    RequestedCall request(RequestCallCommand command);
}
