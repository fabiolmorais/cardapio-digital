package br.com.cardapio.domain.report.model;

/**
 * Os produtos ativos com vídeo pronto e sem vídeo, e quanto cada grupo vendeu no período (REL-26).
 *
 * @param withVideo     produtos com vídeo
 * @param withVideoSold unidades que eles venderam
 */
public record VideoComparison(int withVideo, int withVideoSold, int withoutVideo, int withoutVideoSold) {

    /** Vendidos por produto, com vídeo; null sem produtos. */
    public Double perProductWithVideo() {
        return withVideo == 0 ? null : (double) withVideoSold / withVideo;
    }

    public Double perProductWithoutVideo() {
        return withoutVideo == 0 ? null : (double) withoutVideoSold / withoutVideo;
    }
}
