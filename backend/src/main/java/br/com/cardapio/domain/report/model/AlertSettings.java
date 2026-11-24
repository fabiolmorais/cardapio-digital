package br.com.cardapio.domain.report.model;

import br.com.cardapio.domain.shared.exception.InvalidValueException;

/**
 * Os alertas em tempo real do restaurante (REL-78, desenho 14.15): cada um liga e desliga e tem o seu
 * limite. Sem configuração, vale o {@link #DEFAULT}: cozinha (25 min) e chamado (3 min) ligados,
 * comanda (6 h) desligada.
 */
public record AlertSettings(boolean kitchenEnabled, int kitchenMinutes, boolean callEnabled, int callMinutes,
                            boolean tabEnabled, int tabHours) {

    public static final AlertSettings DEFAULT = new AlertSettings(true, 25, true, 3, false, 6);

    public static final int MIN_KITCHEN = 5;
    public static final int MAX_KITCHEN = 120;
    public static final int MIN_CALL = 1;
    public static final int MAX_CALL = 30;
    public static final int MIN_TAB = 1;
    public static final int MAX_TAB = 24;

    public AlertSettings {
        if (kitchenMinutes < MIN_KITCHEN || kitchenMinutes > MAX_KITCHEN) {
            throw new InvalidValueException("O alerta da cozinha vai de 5 a 120 minutos");
        }
        if (callMinutes < MIN_CALL || callMinutes > MAX_CALL) {
            throw new InvalidValueException("O alerta do chamado vai de 1 a 30 minutos");
        }
        if (tabHours < MIN_TAB || tabHours > MAX_TAB) {
            throw new InvalidValueException("O alerta da comanda vai de 1 a 24 horas");
        }
    }

    public boolean anyEnabled() {
        return kitchenEnabled || callEnabled || tabEnabled;
    }
}
