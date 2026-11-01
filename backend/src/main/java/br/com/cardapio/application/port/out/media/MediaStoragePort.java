package br.com.cardapio.application.port.out.media;

import java.time.Duration;
import java.util.Optional;

/**
 * O armazenamento de arquivos (Cloudflare R2; nos testes, o S3Mock). O núcleo só conhece
 * caminhos ("products/…/large.jpg"), nunca o nome do bucket nem a URL do serviço.
 */
public interface MediaStoragePort {

    /**
     * Uma URL assinada para o navegador enviar o arquivo direto ao armazenamento, sem passar
     * pelo back. Ela só vale para este caminho, este tipo e este tamanho, e por pouco tempo.
     */
    SignedUpload signUpload(String path, String contentType, long sizeBytes, Duration validity);

    /**
     * Lê um arquivo enviado. Vazio se ele não existe (o envio falhou ou não aconteceu).
     * Recusa ler mais do que maxBytes: quem enviou pode ter mandado um arquivo maior que o combinado.
     */
    Optional<byte[]> read(String path, long maxBytes);

    /** Grava um arquivo nosso (os tamanhos gerados), que pode ficar em cache por muito tempo. */
    void store(String path, byte[] content, String contentType);

    void delete(String path);

    /** O endereço público do arquivo, para o navegador baixar. */
    String publicUrl(String path);
}
