package br.com.cardapio.application.port.out.video;

/**
 * O vídeo como o serviço de vídeo o vê.
 *
 * @param width           0 enquanto não converteu
 * @param durationSeconds 0 enquanto não converteu
 */
public record HostedVideo(Status status, int width, int height, int durationSeconds) {

    public enum Status {
        /** Esperando o arquivo, ou convertendo. */
        PROCESSING,
        READY,
        FAILED
    }
}
