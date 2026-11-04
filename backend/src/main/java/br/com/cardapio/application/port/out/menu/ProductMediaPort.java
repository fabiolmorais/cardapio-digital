package br.com.cardapio.application.port.out.menu;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.menu.model.ProductId;
import br.com.cardapio.domain.menu.model.ProductMedia;
import br.com.cardapio.domain.menu.model.ProductMediaId;

import java.util.List;
import java.util.Optional;

public interface ProductMediaPort {

    void save(ProductMedia media);

    /** Vazio se a mídia não existe ou é de outro restaurante. */
    Optional<ProductMedia> find(EstablishmentId establishmentId, ProductMediaId id);

    /** As mídias ativas do produto, na ordem (a primeira é a capa). São no máximo 8. */
    List<ProductMedia> findActiveByProduct(EstablishmentId establishmentId, ProductId productId);

    /** Posição para uma mídia nova: depois de todas as que o produto já teve. */
    int nextPosition(EstablishmentId establishmentId, ProductId productId);

    /**
     * As galerias prontas do cardápio: as mídias ativas e PRONTAS (o vídeo em conversão ou que
     * falhou fica de fora) de todos os produtos do restaurante, em ordem (produto, posição), numa
     * consulta só (sem N+1). A capa de cada produto sai daqui: a primeira foto.
     */
    List<ProductMedia> findReadyGalleries(EstablishmentId establishmentId);

    /**
     * A mídia deste vídeo, de qualquer restaurante: o aviso do serviço de vídeo chega só com o id
     * dele. Quem chama é o webhook, protegido por um token secreto.
     */
    Optional<ProductMedia> findByVideoId(String videoId);
}
