package br.com.cardapio.application.port.in.rodizio;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.rodizio.model.BannerFile;
import br.com.cardapio.domain.rodizio.model.Rodizio;
import br.com.cardapio.domain.rodizio.model.RodizioId;

import java.util.UUID;

/**
 * O banner do rodízio, enviado pelo restaurante (a tela "Escolha o rodízio" da mesa). Sem ele, a
 * mesa mostra a imagem automática. O envio é em dois passos, como na logo: a URL assinada e o aviso.
 */
public interface RodizioBannerUseCase {

    /** Passo 1: a URL assinada, de validade curta, direto para o armazenamento. */
    RodizioBannerUpload start(EstablishmentId establishmentId, RodizioId rodizioId, BannerFile file);

    /** Passo 2: o arquivo chegou; confere pelos bytes, corta em 2:1 e troca o banner. */
    Rodizio set(EstablishmentId establishmentId, RodizioId rodizioId, UUID uploadId);

    /** Tira o banner: a mesa volta para a imagem automática. */
    Rodizio remove(EstablishmentId establishmentId, RodizioId rodizioId);

    /** O endereço público do banner do rodízio; null se ele não tem. */
    String bannerUrl(Rodizio rodizio);
}
