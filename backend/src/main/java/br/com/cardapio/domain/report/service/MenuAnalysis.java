package br.com.cardapio.domain.report.service;

import java.time.Duration;
import java.time.Instant;

/** As regras do relatório de cardápio (REL-13, 14, 16). */
public final class MenuAnalysis {

    /**
     * "Vendem pouco" (REL-14) só olha produtos ativos há mais de 30 dias: um prato novo ainda não teve
     * tempo de vender e não é candidato a sair do cardápio.
     */
    public static final Duration SLOW_SELLER_MIN_AGE = Duration.ofDays(30);

    /** Quantos produtos aparecem em "Vendem pouco". */
    public static final int SLOW_SELLERS = 5;

    private MenuAnalysis() {
    }

    /** Até quando o produto precisa ter sido cadastrado para entrar em "Vendem pouco". */
    public static Instant slowSellerCutoff(Instant now) {
        return now.minus(SLOW_SELLER_MIN_AGE);
    }
}
