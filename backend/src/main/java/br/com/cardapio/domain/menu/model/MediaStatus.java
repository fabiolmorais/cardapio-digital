package br.com.cardapio.domain.menu.model;

/**
 * Se a mídia já pode aparecer no cardápio. A foto nasce pronta (o back gera os tamanhos na hora);
 * o vídeo nasce em conversão e fica pronto quando o serviço de vídeo termina.
 */
public enum MediaStatus {
    /** O vídeo está sendo enviado ou convertido. */
    PROCESSING,
    READY,
    /** A conversão falhou, ou o vídeo não serve (longo demais): o dono vê o motivo e envia outro. */
    FAILED
}
