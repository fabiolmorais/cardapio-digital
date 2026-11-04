package br.com.cardapio.application.port.out.video;

import java.time.Duration;
import java.util.Optional;

/**
 * O serviço de vídeo (Bunny Stream; no desenvolvimento e nos testes, um falso). Ele recebe o
 * arquivo direto do navegador, converte para tocar leve no celular e entrega pela CDN. O núcleo
 * só conhece o id do vídeo, nunca a chave da API nem o endereço do serviço.
 */
public interface VideoHostingPort {

    /** Cria o vídeo (ainda vazio) no serviço e devolve o id dele. */
    String createVideo(String title);

    /**
     * A autorização para o navegador enviar o arquivo deste vídeo, e só dele, por pouco tempo.
     * A chave da API nunca vai para o navegador: vai uma assinatura que vence.
     */
    VideoUploadTicket signUpload(String videoId, Duration validity);

    /** Como o vídeo está no serviço. Vazio se o serviço não conhece o id. */
    Optional<HostedVideo> find(String videoId);

    /** Apaga o vídeo do serviço (o vídeo removido do prato não fica custando armazenamento). */
    void deleteVideo(String videoId);

    /** O endereço para tocar o vídeo convertido (MP4, que o celular toca sem biblioteca). */
    String playbackUrl(String videoId);

    /** A imagem de capa do vídeo; null se o serviço não gera uma. */
    String posterUrl(String videoId);
}
