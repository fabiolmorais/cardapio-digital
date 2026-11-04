package br.com.cardapio.application.port.in.menu;

public interface SyncVideoUseCase {

    /**
     * O serviço de vídeo avisou que este vídeo mudou (o webhook). O aviso só diz QUAL vídeo: o
     * status vem do próprio serviço, consultado de novo. Vídeo desconhecido: nada acontece.
     */
    void sync(String videoId);
}
