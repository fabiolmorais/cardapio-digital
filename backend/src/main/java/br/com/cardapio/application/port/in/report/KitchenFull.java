package br.com.cardapio.application.port.in.report;

import br.com.cardapio.domain.report.model.CategoryPrep;
import br.com.cardapio.domain.report.model.DelayedSlot;
import br.com.cardapio.domain.report.model.HourTimes;
import br.com.cardapio.domain.report.model.PeakAverage;
import br.com.cardapio.domain.report.model.ServiceTimes;

import java.util.List;

/** Os blocos completos da Cozinha e tempos (Business, REL-31, 32, 34, 35, 36, 37). */
public record KitchenFull(ServiceTimes current, ServiceTimes compared, List<HourTimes> byHour,
                          List<PeakAverage> peaks, List<CategoryPrep> byCategory, List<DelayedSlot> delayed) {
}
