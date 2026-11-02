package br.com.cardapio.domain.call.model;

/** Os botões da mesa. Os botões próprios do restaurante (fase 2) vêm depois. */
public enum CallType {
    /** "Chamar garçom". */
    WAITER,
    /** "Pedir a conta", com a forma de pagamento. */
    BILL,
    /** "Liberar esta mesa": o celular não provou a presença (Wi-Fi e GPS falharam) e pede ao garçom. */
    UNLOCK
}
